package jpl.cws.core.code;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import jpl.cws.core.CwsConfig;
import jpl.cws.core.service.SpringApplicationContext;

/**
 * Helper / service methods related to CWS code capabilities.
 * 
 * @author ghollins
 *
 */
public class CodeService implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(CodeService.class);
	
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private SpringApplicationContext springApplicationContext;
	@Autowired private CwsConfig cwsConfig;
	
	private static String TEMP_DIR_PATH;
	
	public static List<URL> urls  = new ArrayList<URL>();
	
	public CodeService() {
		log.trace("CodeService constructor...");
	}
	
	@SuppressWarnings("resource")
	@Override
	public void afterPropertiesSet() throws Exception {
		log.trace("jdbcTemplate = "+jdbcTemplate);
		TEMP_DIR_PATH = File.createTempFile("foo", ".dummy").getParentFile().getAbsolutePath();
		log.trace("temp dir path = "+TEMP_DIR_PATH);
		log.trace("made dir = " + new File(TEMP_DIR_PATH+"/jpl").mkdir());
		log.trace("made dirs = " + new File(TEMP_DIR_PATH+"/jpl/cws/core/code").mkdirs());
		
		// Construct the set of URLs
		File outputDir = new File(TEMP_DIR_PATH);

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		ClassLoader parent = cl;

		while (parent != null) {
			if (parent.getClass().getName().equals("java.net.URLClassLoader")) {
				try {
					Method getURLsMethod = parent.getClass().getMethod("getURLs");
					URL[] urlsArray = (URL[]) getURLsMethod.invoke(parent);
					for (URL url : urlsArray) {
						urls.add(url);
						log.trace("CC ["+parent+"] URL: " + url);
					}
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					log.error("Error accessing getURLs() method on classloader: " + parent, e);
				}
			}
			parent = parent.getParent(); // traverse up the chain
		}

		urls.add(outputDir.toURI().toURL());
	}
	
	
	/**
	 * Updates the "cws" bean to the latest code in the database.
	 * 
	 */
	public void updateToLatestCode() throws Exception {
		String latestCode = getLatestCode();
		log.trace("latestCode = " + latestCode);

		// If code is successfully compiled,
		// then replace current Spring bean with new class
		String errors = compileCode(latestCode);
		if (errors == null) {
			Class compiledClass = getCompiledClass();
			springApplicationContext.replaceBean("cws", compiledClass);
			
			//
			// Get the new bean back (now constructed), and set CwsConfig into it
			//
			Object bean = springApplicationContext.getBean("cws");
			Method method = compiledClass.getMethod("setCwsConfig", new Class[]{CwsConfig.class});
			method.invoke(bean, cwsConfig);
			
			log.trace("after replaceBean");
		}
		else {
			log.error("Spring context bean not replaced, since error(s) occurred while compiling class ("+errors+")");
		}
	}
	
	
	/**
	 * Returns the latest code in the database.
	 * 
	 */
	public String getLatestCode() {
		List<Map<String,Object>> list = jdbcTemplate.queryForList(
				"SELECT code FROM cws_code ORDER BY id DESC LIMIT 1");
		if (list.isEmpty()) {
			return null;
		}
		else {
			return list.iterator().next().values().iterator().next().toString();
		}
	}
	
	
	/**
	 * Returns the latest "in progress" code in the database.
	 * 
	 */
	public String getLatestInProgressCode() {
		List<Map<String,Object>> list = jdbcTemplate.queryForList(
				"SELECT code FROM cws_code_inprogress ORDER BY id DESC LIMIT 1");
		if (list.isEmpty()) {
			return null;
		}
		else {
			return list.iterator().next().values().iterator().next().toString();
		}
	}
	
	
	/**
	 * 
	 */
	public Class getCompiledClass() {
		// Load a class from the compiled code
		URLClassLoader loader = null;
		Class clazz = null;

		try {
			loader = new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
			clazz = loader.loadClass("jpl.cws.core.code.Snippets");
			log.trace("LOADED CLASS: " + clazz);
			Object classObj = clazz.newInstance();
			if (log.isTraceEnabled()) {
				for (Method m : classObj.getClass().getDeclaredMethods()) {
					log.trace(" DECLARED METHOD :::: " + m);
				}
			}
		} catch (Exception e) {
			log.error("Unexpected exception", e);
		} finally {
			if (loader != null) {
				try {
					loader.close();
				} catch (IOException e) {
					log.error("Unexpected error while closing URLClassLoader", e);
				}
			}
		}
		return clazz;
	}
	
	
	/**
	 * Validates the specified code.
	 * 
	 * @return true if valid, false otherwise
	 * 
	 */
	public String validateAndPersistCode(String code) {
		// Regardless of what the result of the compilation below is,
		// persist the "in progress" code in the database
		//
		persistInProgressCode(code);
		
		// Compile code, and if valid, persist code in the database
		String errors = compileCode(code);
		
		if (errors == null){
			// Code is valid, so persist it in the database
			persistCode(code);
			return null;
		}
		else {
			return errors;
		}
	}
	
	
	/**
	 * Compiles the specified code.
	 * 
	 */
	public String compileCode(String code) {
		log.trace("Compiling code...");
		String classPath = System.getProperty("java.class.path");
		
		// Write code to temporary file
		File tempJavaFile = null;
		PrintWriter tempFileWriter = null;
		try {
			tempJavaFile = new File(TEMP_DIR_PATH+"/jpl/cws/core/code/Snippets.java");
			if (tempJavaFile.exists()) {
				tempJavaFile.delete();
			}
			tempJavaFile.createNewFile();
			tempFileWriter = new PrintWriter(tempJavaFile);
			code = code.replace("class CustomMethods", "class Snippets");
			System.out.println("CODE: " + code);
			tempFileWriter.println(code);
		} catch (IOException e) {
			log.error("Unexpected error while writing code to temp file", e);
		}
		finally {
			tempFileWriter.close();
		}
		
		StandardJavaFileManager sjfm = null;
		try {
			// Compile code in temp file 
			JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
			sjfm = jc.getStandardFileManager(null, null, null);
			
			Iterable fileObjects = sjfm.getJavaFileObjects(tempJavaFile);
			
			List<String> optionList = new ArrayList<String>();
			
			// Construct class path
			for (URL url : urls) {
				classPath += url.toString().replaceFirst("file:", ":");
			}
			log.trace("classPath: "+classPath);
			optionList.addAll(Arrays.asList("-classpath", classPath));
			
			// specify where compiled class goes
			String[] options = new String[]{"-d", TEMP_DIR_PATH};
			
			// any other options you want
			optionList.addAll(Arrays.asList(options));
			
			// log what the options are
			for (String op : optionList) { log.trace("option: "+op); }
			
			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
			boolean compileSuccess = jc.getTask(null, sjfm, diagnostics, optionList, null, fileObjects).call();
			
			if (!compileSuccess) {
				log.debug("compile not success");
				String errors = "<div class=\"error-log\">";
				for(Diagnostic<?> error : diagnostics.getDiagnostics()) {
					errors += "<p>" +
							error.getKind() + " : " +
							"Line " + error.getLineNumber() + ", col " + error.getStartPosition() + ": " +
							error.getMessage(null) + "</p>";
					log.warn("DIAG: " + error);
				}
				errors += "</div>";
				return errors;
			}
			
		} catch (Exception e) {
			log.error("Unexpected error while compiling code in temp file", e);
			throw e;
		}
		finally {
			// close std Java file manager
			log.trace("closing sjfm...");
			try {
				sjfm.close();
			} catch (IOException e) {
				log.error("Unexpected error while closing sjfm", e);
			}
		}
		
		// Finally, clean up the temp file
		if (tempJavaFile != null && tempJavaFile.exists()) {
			log.trace("Deleting the temporary Java code file...");
			tempJavaFile.delete();
		}
		
		return null;
	}
	
	
	/**
	 * Persists and versions the code/compiled-object in the database.
	 * 
	 */
	public void persistCode(String code) {
		log.info("Persisting code into cws_code table...");
		
		// Get the object, and persist to DB
		//
		jdbcTemplate.update("INSERT INTO cws_code(code) VALUES (?)", new Object[] {code});
		
		// TODO: add date column
	}
	
	
	/**
	 * Persists and versions the code/compiled-object in the "tmp" database.
	 * This is the "in progress" version of the code, and may not compile
	 * 
	 */
	public void persistInProgressCode(String code) {
		log.info("Persisting code into cws_code_inprogress table...");
		
		// Get the object, and persist to DB
		//
		jdbcTemplate.update("INSERT INTO cws_code_inprogress(code) VALUES (?)", new Object[] {code});
		
		// TODO: add date column
	}
	
	
	public String getTempDirPath() {
		return TEMP_DIR_PATH;
	}

}
