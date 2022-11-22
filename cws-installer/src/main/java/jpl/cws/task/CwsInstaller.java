package jpl.cws.task;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static jpl.cws.task.CwsInstallerUtils.bailOutMissingOption;
import static jpl.cws.task.CwsInstallerUtils.bailOutWithMessage;
import static jpl.cws.task.CwsInstallerUtils.copy;
import static jpl.cws.task.CwsInstallerUtils.copyAllType;
import static jpl.cws.task.CwsInstallerUtils.move;
import static jpl.cws.task.CwsInstallerUtils.createFile;
import static jpl.cws.task.CwsInstallerUtils.deleteDirectory;
import static jpl.cws.task.CwsInstallerUtils.flushConsole;
import static jpl.cws.task.CwsInstallerUtils.getFileContents;
import static jpl.cws.task.CwsInstallerUtils.getPreset;
import static jpl.cws.task.CwsInstallerUtils.isLocalPortAvailable;
import static jpl.cws.task.CwsInstallerUtils.isRemotePortListening;
import static jpl.cws.task.CwsInstallerUtils.mkDir;
import static jpl.cws.task.CwsInstallerUtils.openUpPermissions;
import static jpl.cws.task.CwsInstallerUtils.print;
import static jpl.cws.task.CwsInstallerUtils.readLine;
import static jpl.cws.task.CwsInstallerUtils.readPassword;
import static jpl.cws.task.CwsInstallerUtils.readRequiredLine;
import static jpl.cws.task.CwsInstallerUtils.serverListening;
import static jpl.cws.task.CwsInstallerUtils.writeToFile;
import static jpl.cws.task.UnzipUtility.unzipFile;

import java.lang.*;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.nio.file.Files;
import java.io.StringReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Math;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

import javax.tools.ToolProvider;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/*
 * @author danyu, ghollins, collinss, ztaylor
 *
 */
public class CwsInstaller {
	private static final Logger log = LoggerFactory.getLogger(CwsInstaller.class);

	private static final String SEP = File.separator;

	// Number of available processing cores this machine has
	//
	public static final int CORES = Runtime.getRuntime().availableProcessors();

	// Number of bytes per gigabyte
	private static final long BYTES_PER_GIG = (long) 1e+9;

	// amount of time in milliseconds that a part of a CWS installation
	// (database, worker, or console) can be off from the system time of
	// this installation
	private static final long SYSTEM_TIME_OFFSET_THRESHOLD_MILLIS = 10000;

	private static final String LDAP_IDENTITY_PLUGIN_CLASS = "ldap_identity_plugin_class";
	private static final String LDAP_SECURITY_FILTER_CLASS = "ldap_security_filter_class";
	private static final String CAMUNDA_SECURITY_FILTER_CLASS = "camunda_security_filter_class";

	// The set of valid authentication plugin schemes
	private static final HashSet<String> VALID_PLUGINS = new HashSet<String>() {
		{add("LDAP"); add("CAMUNDA"); add("CUSTOM");}
	};

	// The set of valid database types
	private static final HashSet<String> VALID_DATABASES = new HashSet<String>() {
		{add("mysql"); add("mariadb");}
	};

	// The set of valid history level types
	private static final HashSet<String> VALID_HISTORY_LEVELS = new HashSet<String>() {
		{add("activity"); add("audit"); add("full");}
	};

	private static String cws_installer_mode = System.getenv("CWS_INSTALLER_MODE");

	private static String cws_version;

	private static String osName;
	private static int physicalMemoryGigs;
	private static String this_hostname;
	private static String cws_console_host;
	private static String cws_console_ssl_port;

	private static String cws_root;
	private static String last_install_type_path;
	private static String config_templates_dir;
	private static String config_work_dir;
	private static String cws_tomcat_root;
	private static String cws_tomcat_bin;
	private static String cws_tomcat_conf;
	private static String cws_tomcat_lib;
	private static String cws_tomcat_webapps;
	private static String logstash_root;

	private static String installType;
	private static boolean installConsole;
	private static boolean installWorker;
	private static String cws_worker_type;

	private static String cws_engine_proc_start_req_listener;
	private static String cws_engine_jobexecutor_enabled;

	private static String cws_db_type;
	private static String cws_db_host;
	private static String cws_db_port;
	private static String cws_db_name;
	private static String cws_db_url;
	private static String cws_db_driver;
	private static String cws_db_username;
	private static String cws_db_password;

	private static String cws_auth_scheme;
	private static String cws_user;
	private static String cws_user_firstname;
	private static String cws_user_lastname;
	private static String cws_user_email;
	private static String cws_notification_emails;

	private static String cws_tomcat_connector_port;
	private static String cws_tomcat_ssl_port;
	private static String cws_shutdown_port;
	private static String cws_tomcat_ajp_port;

	private static String cws_smtp_hostname;
	private static String cws_smtp_port;

	private static String cws_amq_jmx_port;
	private static String cws_amq_host;
	private static String cws_amq_port;
	private static String cws_jmx_port;
	private static String cws_ldap_url;
	private static String ldap_identity_plugin_class;
	private static String ldap_security_filter_class;
	private static String camunda_security_filter_class;
	private static String cws_identity_plugin_class;
	private static String cws_send_user_task_assign_emails;
	private static String cws_task_assignment_sub;
	private static String cws_task_assignment_body;
	private static String cws_brand_header;
	private static String cws_project_webapp_root;
	private static String cws_enable_cloud_autoscaling;
	private static String aws_cloudwatch_endpoint;
	private static String metrics_publishing_interval;
	private static String cws_worker_id;
	private static String cws_server_root;
	private static String logstash_ver;
	private static String unique_broker_group_name;
	private static String cws_security_filter_class;
	private static String startup_autoregister_process_defs;
	private static String cws_token_expiration_hours;
	private static String elasticsearch_protocol;
	private static String elasticsearch_protocol_init;
	private static String elasticsearch_host;
	private static String elasticsearch_host_init;
	private static String elasticsearch_port;
	private static String elasticsearch_use_auth;
	private static String elasticsearch_username;
	private static String elasticsearch_password;
	private static String user_provided_logstash;
	private static String history_level;
	private static String history_days_to_live;
	private static String max_num_procs_per_worker;
	private static String worker_abandoned_days;

	private static String aws_default_region;
	private static String aws_sqs_dispatcher_sqsUrl;
	private static String aws_sqs_dispatcher_msgFetchLimit;


	private static Boolean reconfigure = false;

	public CwsInstaller() {}

	public static void main(String args[]) {
		try {

			for (String arg : args) {

				if (arg.equals("--reconfigure")) {
					reconfigure = true;
					break;
				}
			}

			if (reconfigure) {
				print("RECONFIGURE CwsInstaller");
			}
			else {
				print("START CwsInstaller");
			}

			getVersion();
			getOsName();
			getTotalPhysicalMemory();
			checkCompiler();
			printWelcomeMessage();
			init();
			getHostname();
			setInstallType();
			setWorkerType();
			if (!reconfigure && !installConsole) {
				deleteCwsUiWebApp();
			}
			setIdentityPluginType();
			setupDatabase();
			setupAdminUser();
			setupNotificationEmails();
			setupTokenExpirationHours();
			setupPorts();
			setupTaskAssigmentEmails();
			setupSMTP();
			setupElasticsearch();
			setupLogstash();
			setupHistoryLevel();
			setupAws();
			if (installConsole) {
				setupHistoryDaysToLive();
				setBrandHeader();
				setProjectAppRoot();
				setupCloudAutoscaling();
				setupAwsSqs();
			}
			setupLimitToRemoveAbandonedWorkersByDays();
			setupMaxLimitForNumberOfProcessesPerWorker();
			genUniqueWorkerId();
			setupStartupAutoregisterProcessDefs();
			showInstallationInfo();
			validateConfig();

			// Up until this point, no changes have been made to the CWS installation,
			// and after this point, only unhandled IOExceptions should be able to
			// interrupt the installation process
			acceptInstallationConfig();
			createFreshWorkDir();
			updateFiles();

			if (!reconfigure) {
				installLogstash();
			}

			flushConsole();

			print("END CwsInstaller");
			print("");
			print("---------------------------------------------------------------");
			print("Your configuration settings have been saved to:                ");
			print("  " + System.getenv("CWS_INSTALLER_PRESET_FILE"));
			print("");
			print("If no errors were seen above, then you are ready to launch CWS!");
			print("  Run:");
			print("    ./start_cws.sh");
			print("");
			print("  Then access CWS with a browser at:");
			print("     https://" + cws_console_host + ":" + cws_console_ssl_port + "/cws-ui");
			print("---------------------------------------------------------------");
			print("");
			if (cws_auth_scheme.equals("CAMUNDA")) {
				print("");
				print("  IMPORTANT:  Your default password is set to \"changeme\".  Change your password ASAP.");
				print("");
				print("              To change your password, inside CWS, first navigate to the home page.  Click \"Admin\" in the upper right corner.");
				print("              Then click the upper right icon with your name.  Then click \"My profile\".");
				print("              Then on the left panel click \"Account\".  Then follow the directions on the screen.");
				print("");
			}

			writeOutConfigurationFile();

			// Write the .installType file to disk so it may be detected on subsequent install attempts
			writeToFile(Paths.get(last_install_type_path), installType);

			exit(0);
		} catch (IOException e) {
			e.printStackTrace();

			exit(1);
		}
	}


	private static void init() {
		cws_root = getenv("CWS_HOME");
		cws_server_root = cws_root + SEP + "server";
		last_install_type_path = cws_root + SEP + ".installType";

		cws_tomcat_root = cws_root + SEP + "server" + SEP + "apache-tomcat-" + getenv("TOMCAT_VER");
		cws_tomcat_bin     = cws_tomcat_root + SEP + "bin";
		cws_tomcat_conf    = cws_tomcat_root + SEP + "conf";
		cws_tomcat_lib     = cws_tomcat_root + SEP + "lib";
		cws_tomcat_webapps = cws_tomcat_root + SEP + "webapps";

		logstash_ver = getenv("LOGSTASH_VER");
		logstash_root = cws_server_root + SEP + "logstash-" + logstash_ver;

		config_templates_dir = cws_root + SEP + "config" + SEP + "templates";
		config_work_dir      = cws_root + SEP + "config" + SEP + "work";

		ldap_identity_plugin_class = getPreset(LDAP_IDENTITY_PLUGIN_CLASS);
		ldap_security_filter_class = getPreset(LDAP_SECURITY_FILTER_CLASS);
		camunda_security_filter_class = getPreset(CAMUNDA_SECURITY_FILTER_CLASS);
	}

	private static void exit(int status) {
		System.exit(status);
	}

	private static void getVersion() {
		cws_version = getenv("CWS_VER");
	}

	private static void getOsName() {
		osName = getProperty("os.name");
	}

	private static void getTotalPhysicalMemory() {
		OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		physicalMemoryGigs = (int)(osBean.getTotalPhysicalMemorySize() / BYTES_PER_GIG);

		// Round result down to nearest multiple of 2
		physicalMemoryGigs -= (physicalMemoryGigs % 2);

		// Assume there's at least one gig if we end up with 0 somehow.
		physicalMemoryGigs = Math.max(physicalMemoryGigs, 1);
	}

	private static void checkCompiler() {
		if (ToolProvider.getSystemJavaCompiler() == null) {
			print ("No JDK found. Please set your JAVA_HOME environment variable to a valid JDK root directory (not JRE)");
			exit(1);
		}
	}

	private static void printWelcomeMessage() {
		print ("");
		print ("  /█████   ██       ██   /██████  ");
		print (" ██        ██   |   ██   ██       ");
		print (" ██        ██   █   ██   \\█████\\  ");
		print (" ██        \\█\\ /█\\ /█/        ██  ");
		print ("  \\█████     \\█/ \\█/     ██████/  ");
		print ("");
		print ("  Common    Workflow     Service   .....   ( v" + cws_version + " )");
		print ("                                           ( installing on platform: " + osName + " )");
		print ("");
	}


	private static void getHostname() {
		this_hostname = getPreset("hostname");

		if (cws_installer_mode.equals("interactive")) {
			if (this_hostname == null) {
				this_hostname = readRequiredLine("Please enter the host name of this machine: ",
						"Must specify host name!");
			} else {
				this_hostname = readLine("Please enter the host name of this machine. " +
						"Default is " + this_hostname + ": ", this_hostname);
			}
		} else {
			if (this_hostname == null) {
				bailOutMissingOption("hostname");
			}
		}
	}


	private static void setInstallType() throws IOException {
		installType = getPreset("install_type");

		if (cws_installer_mode.equals("interactive")) {
			showInstallConfigOptionsInfo();

			if (installType == null) {
				installType = readRequiredLine("Select a configuration (1/2/3): ",
						"ERROR: Must specify either 1, 2 or 3. Try again: ");
			} else {
				installType = readLine("Select a configuration (1/2/3). Default is " + installType + ": ", installType);
			}
		} else {
			if (installType == null) {
				bailOutMissingOption("install_type");
			}
		}

		log.debug("Install Type: " + installType);

		switch (installType) {
			case "1":  // CONSOLE AND WORKER
				installConsole = true;
				installWorker = true;
				cws_engine_proc_start_req_listener = "Y";
				cws_engine_jobexecutor_enabled = "true";
				break;
			case "2":  // CONSOLE ONLY
				installConsole = true;
				installWorker = false;
				cws_engine_proc_start_req_listener = "N";
				cws_engine_jobexecutor_enabled = "false";
				break;
			case "3":  // WORKER ONLY
				installConsole = false;
				installWorker = true;
				cws_engine_proc_start_req_listener = "Y";
				cws_engine_jobexecutor_enabled = "true";
				break;
		}

		// Check for consistency of installation type, and advise the User, if not consistent
		File lastInstallTypeFile = new File(Paths.get(last_install_type_path).toAbsolutePath().toString());

		if (lastInstallTypeFile.exists()) {
			FileReader fr = new FileReader(lastInstallTypeFile);
			BufferedReader br = new BufferedReader(fr);
			String typeRead = br.readLine();
			br.close();
			if (!typeRead.equals(installType)) {
				print("WARN:  It looks like you have previously configured this CWS installation with a different install type (" + typeRead + ").");
				print("       Re-configurations are only allowed if you keep the same installation type.");
				print("       If you want to change the installation type, then please blow away your entire CWS installation directory, and re-extract the CWS zip file.");
				print("       NOTE 1:  If you blow away the installation directory, then any log information accumulated so far will be gone forever.");
				print("       NOTE 2:  If you completely start over, it's also advisable to drop and recreate your CWS database as well.");
				exit(1);
			}
		}
	}

	private static void setWorkerType() {

		if (installWorker) {
			// WORKER TYPE
			cws_worker_type = getPreset("worker_type");
			if (cws_worker_type == null) {
				if (cws_installer_mode.equals("interactive")) {
					cws_worker_type = readLine("Please enter this worker type (run_all | run_models_only | run_external_tasks_only). Default is run_all: ", "run_all");
				} else {
					cws_worker_type = "run_all"; // default
				}
			}
			log.debug("worker_type: " + cws_worker_type);

			if (cws_worker_type.equals("run_external_tasks_only")) {

				cws_engine_proc_start_req_listener = "N";
				cws_engine_jobexecutor_enabled     = "false";
			}
		}
		else {
			cws_worker_type = "run_all"; // default
		}
	}


	private static void showInstallConfigOptionsInfo() {
		print ( "                                                                               ");
		print ( "Please select an installation configuration:                                   ");
		print ( "                                                                               ");
		print ( "   +--------------------+------------------------------------------------------");
		print ( "1) | CONSOLE AND WORKER |                                                      ");
		print ( "   +--------------------+                                                      ");
		print ( "   |  This installation will contain:                                          ");
		print ( "   |    -- CWS Dashboard (console)                                             ");
		print ( "   |    -- Cockpit, Tasklist, and Admin Apps                                   ");
		print ( "   |    -- A process engine**                                                  ");
		print ( "   |                                                                           ");
		print ( "   |  **NOTE: The process engine will be available for running activities      ");
		print ( "   |          initiated via the TaskList, starting/running newly scheduled     ");
		print ( "   |          processes, and will pick up jobs to run from the database        ");
		print ( "   |          (distribution).                                                  ");
		print ( "   |                                                                           ");
		print ( "   |  This is the recommended starting installation configuration, especially  ");
		print ( "   |  if you don't anticipate the need to scale out due to a large quantity of ");
		print ( "   |  processes running at any given time and/or multiple CPU/IO/memory-       ");
		print ( "   |  intensive processes that need to run at the same time.  You can always   ");
		print ( "   |  install more \"WORKER ONLY\" CWS node(s) at later time, if you wish to   ");
		print ( "   |  scale out.                                                               ");
		print ( "   +---------------------------------------------------------------------------");
		print ( "                                                                               ");
		print ( "   +--------------+------------------------------------------------------------");
		print ( "2) | CONSOLE ONLY |                                                            ");
		print ( "   +--------------+                                                            ");
		print ( "   |  This installation will contain:                                          ");
		print ( "   |    -- CWS Dashboard (console)                                             ");
		print ( "   |    -- Cockpit, Tasklist, and Admin Apps                                   ");
		print ( "   |    -- A process engine**                                                  ");
		print ( "   |                                                                           ");
		print ( "   |  **NOTE: The process engine will ONLY be available for running activities ");
		print ( "   |          initiated via the TaskList.  For example initiating a process    ");
		print ( "   |          manually, or completing a User Task.  The engine will NOT pick up");
		print ( "   |          jobs from the database, and therefore will not play a part in the");
		print ( "   |          distribution of a process's tasks.                               ");
		print ( "   +---------------------------------------------------------------------------");
		print ( "                                                                               ");
		print ( "   +-------------+-------------------------------------------------------------");
		print ( "3) | WORKER ONLY |                                                             ");
		print ( "   +-------------+                                                             ");
		print ( "   |  This installation will contain:                                          ");
		print ( "   |    -- A process engine**                                                  ");
		print ( "   |                                                                           ");
		print ( "   |  **NOTE: The process engine will be able to start/run newly scheduled     ");
		print ( "   |          processes, as well as pick up distributed (DB) jobs to run.      ");
		print ( "   +---------------------------------------------------------------------------");
		print ( "                                                                               ");
		print ( "NOTE: A full CWS topology must contain at least one Worker and one Console     ");
		print ( "                                                                               ");
	}


	private static void setIdentityPluginType() throws UnsupportedOperationException {
		cws_auth_scheme = getPreset("identity_plugin_type");

		if (cws_auth_scheme == null) {
			cws_auth_scheme = getPreset("default_cws_auth_scheme");
		}

		if (cws_installer_mode.equals("interactive")) {
			String read_cws_auth_scheme = readLine("Enter authentication scheme. (LDAP | CAMUNDA | CUSTOM). " +
					"Default is " + cws_auth_scheme + ": ", cws_auth_scheme);

			while (!VALID_PLUGINS.contains(read_cws_auth_scheme.toUpperCase())) {
				print(" ERROR: Invalid authentication scheme, must be one of (LDAP | CAMUNDA | CUSTOM).");
				read_cws_auth_scheme = readLine("Enter authentication scheme. " +
						"Default is " + cws_auth_scheme + ": ", cws_auth_scheme);
			}

			cws_auth_scheme = read_cws_auth_scheme.toUpperCase();
		} else {
			if (!VALID_PLUGINS.contains(cws_auth_scheme.toUpperCase())) {
				print("ERROR: Invalid authentication scheme '" + cws_auth_scheme + "'.");
				print("Must be one of (LDAP | CAMUNDA | CUSTOM).");
				exit(1);
			}
		}

		switch (cws_auth_scheme) {
			case "LDAP":
				print("Using LDAP authentication scheme...");

				cws_ldap_url = getPreset("cws_ldap_url");

				if (cws_ldap_url == null) {
					cws_ldap_url = getPreset("default_cws_ldap_url");
				}

				// PROMPT USER FOR LDAP SERVER URL
				if (cws_installer_mode.equals("interactive")) {
					cws_ldap_url = readLine("Enter the LDAP URL, default is " + cws_ldap_url + ": ", cws_ldap_url);
				}

				cws_identity_plugin_class = ldap_identity_plugin_class;
				cws_security_filter_class = ldap_security_filter_class;

				break;
			case "CAMUNDA":
				print("Using CAMUNDA (Camunda built-in) authentication scheme...");

				cws_security_filter_class = camunda_security_filter_class;

				break;
			case "CUSTOM":
				print("Using CUSTOM identity plugin for security...");
				throw new UnsupportedOperationException("CUSTOM AUTHENTICATION SCHEME SUPPORT NOT IMPLEMENTED YET!! " +
						"PLEASE USE LDAP OR CAMUNDA FOR NOW");

				// TODO: PROMPT USER FOR CUSTOM FILTER CLASS
				// cws_seccurity_filter_class='todo -- custom to be filled in here'
			default:
				// Should never get here...
				break;
		}
	}


	private static void setupDatabase() {
		cws_db_type = getPreset("database_type");

		if (cws_db_type == null) {
			cws_db_type = "mariadb";
		}

		// DB TYPE
		if (cws_installer_mode.equals("interactive")) {
			String read_cws_db_type = readLine("Enter type of database (mysql | mariadb). " +
					"Default is " + cws_db_type + ": ", cws_db_type);

			while (!VALID_DATABASES.contains(read_cws_db_type.toLowerCase())) {
				print("  ERROR: Invalid database, must be one of (mysql | mariadb).");
				read_cws_db_type = readLine("Enter type of database: " +
						"Default is " + cws_db_type + ": ", cws_db_type);
			}

			cws_db_type = read_cws_db_type.toLowerCase();
		} else {
			if (!VALID_DATABASES.contains(cws_db_type.toLowerCase())) {
				print("ERROR: '" + cws_db_type + "' database not supported!");
				print("Must be one of (mysql | mariadb).");
				exit(1);
			}
		}

		log.debug("cws_db_type: " + cws_db_type);

		cws_db_host = getPreset("database_host");

		// DATABASE HOST / IP
		if (cws_installer_mode.equals("interactive")) {
			if (cws_db_host == null) {
				cws_db_host = readRequiredLine("Enter the database host IP address or host name: ",
						"Must specify a database host!");
			} else {
				cws_db_host = readLine("Enter the database host IP address or host name. " +
						"Defaults to " + cws_db_host + ": ", cws_db_host);
			}
		} else {
			if (cws_db_host == null) {
				bailOutMissingOption("database_host");
			}
		}

		log.debug("cws_db_host: " + cws_db_host);

		cws_db_port = getPreset("database_port");

		if (cws_db_port == null) {
			cws_db_port = "3306"; // default
		}

		// DATABASE PORT
		if (cws_installer_mode.equals("interactive")) {
			cws_db_port = readLine("Enter the database port. Default is " + cws_db_port + ": ", cws_db_port);
		}

		log.debug("cws_db_port: " + cws_db_port);

		cws_db_name = getPreset("database_name");

		if (cws_db_name == null) {
			cws_db_name = "cws";
		}

		// DATABASE NAME
		if (cws_installer_mode.equals("interactive")) {
			cws_db_name = readLine("Enter the database name. Default is " + cws_db_name + ": ", cws_db_name);
		}

		log.debug("cws_db_name: " + cws_db_name);

		// CREATE DB URL AND DRIVER
		cws_db_url = "jdbc:" + cws_db_type + "://" + cws_db_host + ":" + cws_db_port + "/" + cws_db_name + "?autoReconnect=true";

		if (cws_db_type.equals("mariadb")) {
			cws_db_driver = "org.mariadb.jdbc.Driver";
		} else {
			cws_db_driver = "com.mysql.jdbc.Driver";
		}

		log.debug("cws_db_driver: " + cws_db_driver);

		cws_db_username = getPreset("database_username");

		// DATABASE USERNAME
		if (cws_installer_mode.equals("interactive")) {
			if (cws_db_username == null) {
				cws_db_username = readRequiredLine("Enter the database username: ",
						"Must specify a database username!");
			} else {
				cws_db_username = readLine("Enter the database username. " +
						"Default is " + cws_db_username + ": ", cws_db_username);
			}
		} else {
			if (cws_db_username == null) {
				bailOutMissingOption("database_username");
			}
		}

		log.debug("cws_db_username: " + cws_db_username);

		cws_db_password = getPreset("database_password");

		if (cws_installer_mode.equals("interactive")) {
			char[] password = readPassword("Enter the database password: ");

			while (password == null || password.length < 1) {
				print("Must specify a database password!");
				password = readPassword("Enter the database password: ");
			}

			cws_db_password = String.valueOf(password);
		} else {
			if (cws_db_password == null) {
				bailOutMissingOption("database_password");
			}
		}
	}


	private static void setupAdminUser() {
		cws_user = getPreset("admin_user");

		if (cws_installer_mode.equals("interactive")) {
			if (cws_user == null) {
				if (cws_auth_scheme.equalsIgnoreCase("LDAP")) {
					cws_user = readRequiredLine("Enter name of LDAP user to be used as initial administrator: ",
							"Must specify a LDAP username!");
				} else {
					cws_user = readRequiredLine("Enter username to be used as initial administrator: ",
							"Must specify a username!");
				}
			} else {
				if (cws_auth_scheme.equalsIgnoreCase("LDAP")) {
					cws_user = readLine("Enter name of LDAP user to be used as initial administrator. " +
									"Default is " + cws_user + ": ", cws_user);
				} else {
					cws_user = readLine("Enter username to be used as initial administrator. " +
									"Default is " + cws_user + ": ", cws_user);
				}
			}
		} else {
			if (cws_user == null) {
				bailOutMissingOption("admin_user");
			}
		}

		// Prompt only for CAMUNDA security scheme
		if (cws_auth_scheme.equalsIgnoreCase("CAMUNDA")) {
			cws_user_firstname = getPreset("admin_firstname");

			if (cws_installer_mode.equals("interactive")) {
				if (cws_user_firstname == null) {
					cws_user_firstname = readRequiredLine("Enter the initial administrator's first name: ",
							"Must specify a first name!");
				} else {
					cws_user_firstname = readLine("Enter the initial administrator's first name. " +
							"Default is " + cws_user_firstname + ": ", cws_user_firstname);
				}
			} else {
				if (cws_user_firstname == null) {
					bailOutMissingOption("admin_firstname");
				}
			}

			cws_user_lastname = getPreset("admin_lastname");

			if (cws_installer_mode.equals("interactive")) {
				if (cws_user_lastname == null) {
					cws_user_lastname = readRequiredLine("Enter the initial administrator's last name: ",
							"Must specify a last name!");
				} else {
					cws_user_lastname = readLine("Enter the initial administrator's last name. " +
							"Default is " + cws_user_lastname + ": ", cws_user_lastname);
				}
			} else {
				if (cws_user_lastname == null) {
					bailOutMissingOption("admin_lastname");
				}
			}

			cws_user_email = getPreset("admin_email");

			if (cws_installer_mode.equals("interactive")) {
				if (cws_user_email == null) {
					cws_user_email = readRequiredLine("Enter the initial administrator's email address: ",
							"Must specify an email address!");
				} else {
					cws_user_email = readLine("Enter the initial administrator's email address. " +
							"Default is " + cws_user_email + ": ", cws_user_email);
				}
			} else {
				if (cws_user_email == null) {
					bailOutMissingOption("admin_email");
				}
			}
		}
	}


	private static void setupNotificationEmails() {
		cws_notification_emails = getPreset("cws_notification_emails");

		if (cws_installer_mode.equals("interactive")) {
			if (cws_notification_emails == null) {
				cws_notification_emails = readRequiredLine("Enter email addresses (separate with commas) used to notify of system errors: ",
						"Must specify at least one email address!");
			} else {
				cws_notification_emails = readLine("Enter email addresses used to notify of system errors. " +
								"Default is " + cws_notification_emails + ": ", cws_notification_emails);
			}
		} else {
			if (cws_notification_emails == null) {
				bailOutMissingOption("cws_notification_emails");
			}
		}
	}


	private static void setupTokenExpirationHours() {
		cws_token_expiration_hours = getPreset("cws_token_expiration_hours");

		if (cws_token_expiration_hours == null) {
			cws_token_expiration_hours = getPreset("default_cws_token_expiration_hours");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_token_expiration_hours = readLine("Enter the amount of time (in hours) that the token will expire. " +
					"Default is " + cws_token_expiration_hours + ": ", cws_token_expiration_hours);
		}
	}


	private static void setupHistoryLevel() {
		history_level = getPreset("history_level");

		if (history_level == null) {
			history_level = getPreset("default_history_level");
		}

		if (cws_installer_mode.equals("interactive")) {
			String read_history_level = readLine("Enter the history level (options: activity, audit, full) to control the amount of history data stored in the database. " +
					"Default is " + history_level + ": ", history_level);

			while (!VALID_HISTORY_LEVELS.contains(read_history_level.toLowerCase())) {
				print("  ERROR: Invalid history level, must be one of (activity | audit | full).");
				read_history_level = readLine("Enter the history level. " +
						"Default is " + history_level + ": ", history_level);
			}

			history_level = read_history_level.toLowerCase();
		} else {
			if (!VALID_HISTORY_LEVELS.contains(history_level.toLowerCase())) {
				print("ERROR: history level '" + history_level + "' not supported!");
				print("Must be one of (activity | audit | full).");
				exit(1);
			}
		}
	}


	private static void setupHistoryDaysToLive() {
		history_days_to_live = getPreset("history_days_to_live");

		if (history_days_to_live == null) {
			history_days_to_live = getPreset("default_history_days_to_live");
		}

		if (cws_installer_mode.equals("interactive")) {
			history_days_to_live = readLine("Enter the number of days to keep history (procs, vars, logs, etc.) before it is deleted automatically. " +
					"Default is " + history_days_to_live + ": ", history_days_to_live);
		}
	}


	private static void setupMaxLimitForNumberOfProcessesPerWorker() {
		max_num_procs_per_worker = getPreset("max_num_procs_per_worker");

		if (max_num_procs_per_worker == null) {
			max_num_procs_per_worker = getPreset("default_max_num_procs_per_worker");
		}

		// make sure preset is valid positive integer
		try {
			if (Integer.parseInt(max_num_procs_per_worker) <= 0) {
				log.warn("Processes per worker value must be a positive integer. Got: " + max_num_procs_per_worker + ". Defaulting to 25.");
				max_num_procs_per_worker = "25";
			}
		} catch (NumberFormatException e) {
			log.warn("Processes per worker value failed to parse as an integer. Got: " + max_num_procs_per_worker + ". Defaulting to 25.");
			max_num_procs_per_worker = "25";
		}

		if (cws_installer_mode.equals("interactive")) {
			boolean done = false;
			while (!done) {
				max_num_procs_per_worker = readLine("Enter the maximum number of processes that run on worker(s). " +
					"Default is " + max_num_procs_per_worker + ": ", max_num_procs_per_worker);

				// make sure input was valid
				try {
					done = Integer.parseInt(max_num_procs_per_worker) >= 1;
				} catch (NumberFormatException e) {
					// bad input, try again
				}
			}
		}
	}


	private static void setupLimitToRemoveAbandonedWorkersByDays() {
		worker_abandoned_days = getPreset("worker_abandoned_days");

		if (worker_abandoned_days == null) {
			worker_abandoned_days = getPreset("default_worker_abandoned_days");
		}

		// make sure preset is valid positive integer
		try {
			if (Integer.parseInt(worker_abandoned_days) <= 0) {
				log.warn("Worker abandoned days must be a positive integer. Got: " + worker_abandoned_days + ". Defaulting to 1.");
				worker_abandoned_days = "1";
			}
		} catch (NumberFormatException e) {
			log.warn("Worker abandoned days failed to parse as an integer. Got: " + worker_abandoned_days + ". Defaulting to 1.");
			worker_abandoned_days = "1";
		}

		if (cws_installer_mode.equals("interactive")) {
			boolean done = false;
			while (!done) {
				worker_abandoned_days = readLine("Enter the number of days after worker(s) are abandoned to remove worker(s) from the database. " +
						"Default is " + worker_abandoned_days + ": ", worker_abandoned_days);

				// make sure input was valid
				try {
					done = Integer.parseInt(worker_abandoned_days) >= 1;
				} catch (NumberFormatException e) {
					// bad input, try again
				}
			}
		}
	}


	private static void setupPorts() {
		// PROMPT USER FOR CWS WEB PORT
		cws_tomcat_connector_port = getPreset("cws_web_port");

		if (cws_tomcat_connector_port == null) {
			cws_tomcat_connector_port = getPreset("default_tomcat_web_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_tomcat_connector_port = readLine("Enter the CWS web port. " +
					"Default is " + cws_tomcat_connector_port + ": ", cws_tomcat_connector_port);
		}

		// PROMPT USER FOR CWS SSL PORT
		cws_tomcat_ssl_port = getPreset("cws_ssl_port");

		if (cws_tomcat_ssl_port == null) {
			cws_tomcat_ssl_port = getPreset("default_tomcat_ssl_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_tomcat_ssl_port = readLine("Enter the CWS SSL port. " +
					"Default is " + cws_tomcat_ssl_port + ": ", cws_tomcat_ssl_port);
		}

		// PROMPT USER FOR CWS AJP PORT
		cws_tomcat_ajp_port = getPreset("cws_ajp_port");

		if (cws_tomcat_ajp_port == null) {
			cws_tomcat_ajp_port = getPreset("default_tomcat_ajp_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_tomcat_ajp_port = readLine("Enter the CWS AJP port. " +
					"Default is " + cws_tomcat_ajp_port + ": ", cws_tomcat_ajp_port);
		}

		// PROMPT USER FOR CWS TOMCAT SHUTDOWN PORT
		cws_shutdown_port = getPreset("cws_shutdown_port");

		if (cws_shutdown_port == null) {
			cws_shutdown_port = getPreset("default_shutdown_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_shutdown_port = readLine("Enter the CWS shutdown port. " +
					"Default is " + cws_shutdown_port + ": ", cws_shutdown_port);
		}

		if (!installConsole) { // WORKER ONLY
			// PROMPT USER FOR CONSOLE HOST
			cws_console_host = getPreset("cws_console_host");

			if (cws_installer_mode.equals("interactive")) {
				if (cws_console_host == null) {
					cws_console_host = readRequiredLine("Enter the CWS Console host: ",
							"You must enter a hostname");
				} else {
					cws_console_host = readLine("Enter the CWS Console host. " +
							"Default is " + cws_console_host + ": ", cws_console_host);
				}
			} else {
				if (cws_console_host == null) {
					bailOutMissingOption("cws_console_host");
				}
			}

			// PROMPT USER FOR CONSOLE SSL PORT
			cws_console_ssl_port = getPreset("cws_console_ssl_port");

			if (cws_installer_mode.equals("interactive")) {
				if (cws_console_ssl_port == null) {
					cws_console_ssl_port = readRequiredLine("Enter the CWS Console web SSL port: ",
							"You must enter a port");
				} else {
					cws_console_ssl_port = readLine("Enter the CWS Console web SSL port. " +
							"Default is " + cws_console_ssl_port + ": ", cws_console_ssl_port);
				}
			} else {
				if (cws_console_ssl_port == null) {
					bailOutMissingOption("cws_console_ssl_port");
				}
			}
		}
		else {
			// We must be installing a console on this host, so automatically use that value without prompting
			cws_console_host     = this_hostname;
			cws_console_ssl_port = cws_tomcat_ssl_port;
		}

		// PROMPT USER FOR AMQ HOST
		cws_amq_host = getPreset("amq_host");

		if (cws_amq_host == null) {
			cws_amq_host = this_hostname;
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_amq_host = readLine("Enter the AMQ host. " +
					"Default is " + cws_amq_host + ": ", cws_amq_host);
		}

		// PROMPT USER FOR AMQ PORT
		cws_amq_port = getPreset("amq_port");

		if (cws_amq_port == null) {
			cws_amq_port = getPreset("default_amq_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_amq_port = readLine("Enter the AMQ port. Default is " + cws_amq_port + ": ", cws_amq_port);
		}

		if (installConsole) {
			// PROMPT USER FOR AMQ JMX PORT
			cws_amq_jmx_port = getPreset("cws_amq_jmx_port");

			if (cws_amq_jmx_port == null) {
				cws_amq_jmx_port = getPreset("default_amq_jmx_port");
			}

			if (cws_installer_mode.equals("interactive")) {
				cws_amq_jmx_port = readLine("Enter the CWS AMQ JMX port. " +
						"Default is " + cws_amq_jmx_port + ": ", cws_amq_jmx_port);
			}
		}

		// PROMPT USER FOR JMX PORT
		cws_jmx_port = getPreset("cws_jmx_port");

		if (cws_jmx_port == null) {
			cws_jmx_port = getPreset("default_cws_jmx_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_jmx_port = readLine("Enter the CWS JMX port. " +
					"Default is " + cws_jmx_port + ": ", cws_jmx_port);
		}
	}


	private static void setupLogstash() {

		// PROMPT USER FOR USER PROVIDED LOGSTASH
		user_provided_logstash = getPreset("user_provided_logstash");

		if (user_provided_logstash == null) {
			user_provided_logstash = getPreset("default_user_provided_logstash");
		}

		if (cws_installer_mode.equals("interactive")) {
			String read_user_provided_logstash = "";

			while (!read_user_provided_logstash.equalsIgnoreCase("y") &&
					!read_user_provided_logstash.equalsIgnoreCase("n")) {
				read_user_provided_logstash =
						readRequiredLine("Are you providing your own Logstash service? (Y/N): ",
								"ERROR: Must specify either 'Y' or 'N'");
			}

			user_provided_logstash = read_user_provided_logstash.toLowerCase();
		}
	}


	private static void setupElasticsearch() {

		// PROMPT USER FOR ELASTICSEARCH PROTOCOL
		elasticsearch_protocol = getPreset("elasticsearch_protocol");

		if (cws_installer_mode.equals("interactive")) {
			if (elasticsearch_protocol == null) {

				String read_elasticsearch_protocol = "";
				while (!read_elasticsearch_protocol.toLowerCase().startsWith("https") &&
					!read_elasticsearch_protocol.toLowerCase().startsWith("http")) {
					read_elasticsearch_protocol = readRequiredLine("Enter the Elasticsearch protocol (be sure to use HTTP or HTTPS):  ",
						"You must enter a protocol");
				}

				elasticsearch_protocol_init = read_elasticsearch_protocol;
				elasticsearch_protocol = read_elasticsearch_protocol.toLowerCase();
				if (elasticsearch_protocol.startsWith("https")) {
					elasticsearch_protocol = "https";
				}
				if (elasticsearch_protocol.startsWith("http")) {
					elasticsearch_protocol = "http";
				}
			} else {
				elasticsearch_protocol = readLine("Enter the Elasticsearch protocol. " + "Default is " + elasticsearch_protocol + ": ", elasticsearch_protocol);
			}
		} else {
			if (elasticsearch_protocol == null) {
				bailOutMissingOption("elasticsearch_protocol");
			}

			elasticsearch_protocol_init = elasticsearch_protocol;
			elasticsearch_protocol = elasticsearch_protocol.toLowerCase();
			if (elasticsearch_protocol.startsWith("https")) {
				elasticsearch_protocol = "https";
			} else if (elasticsearch_protocol.startsWith("http")) {
				elasticsearch_protocol = "http";
			} else {
				bailOutWithMessage("ERROR: elasticsearch_protocol config input is '" +  elasticsearch_protocol
					+ "' ... Be sure to use 'HTTP' or 'HTTPS' for elasticsearch_protocol configuration.");
			}
		}

		log.debug("elasticsearch_protocol: " + elasticsearch_protocol);


		// PROMPT USER FOR ELASTICSEARCH HOST
		elasticsearch_host = getPreset("elasticsearch_host");

		if (cws_installer_mode.equals("interactive")) {
			if (elasticsearch_host == null) {

				String read_elasticsearch_host = "";
				read_elasticsearch_host = readRequiredLine("Enter the Elasticsearch host:  ",
							"You must enter a hostname");

				elasticsearch_host_init = read_elasticsearch_host;
				elasticsearch_host = read_elasticsearch_host.toLowerCase();
				if (elasticsearch_host.startsWith("http:/") || elasticsearch_host.startsWith("http://") ||
					elasticsearch_host.startsWith("https:/") || elasticsearch_host.startsWith("https://")) {
					elasticsearch_host = elasticsearch_host.replaceAll("http://", "").replaceAll("http:/","").replaceAll("https://","").replaceAll("https:/","");
				}
			} else {
				elasticsearch_host = readLine("Enter the Elasticsearch host. " + "Default is " + elasticsearch_host + ": ", elasticsearch_host);
			}
		} else {
			if (elasticsearch_host == null) {
				bailOutMissingOption("elasticsearch_host");
			}

			elasticsearch_host_init = elasticsearch_host;
			elasticsearch_host = elasticsearch_host.toLowerCase();
			if (elasticsearch_host.startsWith("http:/") || elasticsearch_host.startsWith("http://") ||
				elasticsearch_host.startsWith("https:/") || elasticsearch_host.startsWith("https://")) {
				elasticsearch_host = elasticsearch_host.replaceAll("http://", "").replaceAll("http:/","").replaceAll("https://","").replaceAll("https:/","");
			}
		}

		log.debug("elasticsearch_host: " + elasticsearch_host);


		// PROMPT USER FOR ELASTICSEARCH PORT
		elasticsearch_port = getPreset("elasticsearch_port");

		if (elasticsearch_port == null) {
			elasticsearch_port = getPreset("default_elasticsearch_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			elasticsearch_port = readLine("Enter the Elasticsearch port. " +
					"Default is " + elasticsearch_port + ": ", elasticsearch_port);
		}

		log.debug("elasticsearch_port: " + elasticsearch_port);

		// PROMPT USER ELASTICSEARCH AUTH
		elasticsearch_use_auth = getPreset("elasticsearch_use_auth");

		if (elasticsearch_use_auth == null) {
			elasticsearch_use_auth = getPreset("default_elasticsearch_use_auth");
		}

		if (cws_installer_mode.equals("interactive")) {
			String read_elasticsearch_use_auth = "";

			while (!read_elasticsearch_use_auth.equalsIgnoreCase("y") &&
					!read_elasticsearch_use_auth.equalsIgnoreCase("n")) {
				read_elasticsearch_use_auth =
						readRequiredLine("Does you Elasticsearch cluster require authentication? (Y/N): ",
								"ERROR: Must specify either 'Y' or 'N'");
			}

			elasticsearch_use_auth = read_elasticsearch_use_auth.toLowerCase();
		}

		if (elasticsearch_use_auth.equalsIgnoreCase("Y")) {

			elasticsearch_username = getPreset("elasticsearch_username");

			// PROMPT USER FOR ELASTICSEARCH USERNAME
			if (cws_installer_mode.equals("interactive")) {
				if (elasticsearch_username == null) {
					elasticsearch_username = readRequiredLine("Enter the elasticsearch username: ",
							"Must specify an elasticsearch username!");
				} else {
					elasticsearch_username = readLine("Enter the database username. " +
							"Default is " + elasticsearch_username + ": ", elasticsearch_username);
				}
			} else {
				if (elasticsearch_username == null) {
					bailOutMissingOption("elasticsearch_username");
				}
			}

			log.debug("elasticsearch_username: " + elasticsearch_username);

			elasticsearch_password = getPreset("elasticsearch_password");

			// PROMPT USER FOR ELASTICSEARCH PASSWORD
			if (cws_installer_mode.equals("interactive")) {
				char[] password = readPassword("Enter the elasticsearch password: ");

				while (password == null || password.length < 1) {
					print("Must specify an elasticsearch password!");
					password = readPassword("Enter the elasticsearch password: ");
				}

				elasticsearch_password = String.valueOf(password);
			} else {
				if (elasticsearch_password == null) {
					bailOutMissingOption("elasticsearch_password");
				}
			}
		}
	}


	private static void setupTaskAssigmentEmails() {

		// PROMPT USER TO ENABLE SENDING USER TASK ASSIGNMENT EMAILS
		cws_send_user_task_assign_emails = getPreset("notify_users_email");

		if (cws_installer_mode.equals("interactive")) {
			String read_cws_send_user_task_assign_emails = "";

			while (!read_cws_send_user_task_assign_emails.equalsIgnoreCase("y") &&
					!read_cws_send_user_task_assign_emails.equalsIgnoreCase("n")) {
				read_cws_send_user_task_assign_emails = readRequiredLine("Automatically notify users via email when they get assigned a User task? (Y/N): ",
						"ERROR: Must specify either 'Y' or 'N'");
			}

			cws_send_user_task_assign_emails = read_cws_send_user_task_assign_emails.toLowerCase();
		} else {
			if (cws_send_user_task_assign_emails == null) {
				bailOutMissingOption("notify_users_email");
			}
		}

		// PROMPT USER FOR USER TASK ASSIGNMENT EMAIL SUBJECT / BODY
		if (cws_send_user_task_assign_emails.equalsIgnoreCase("Y")) {

			cws_task_assignment_sub = getPreset("email_subject");
			if (cws_task_assignment_sub == null) {
				cws_task_assignment_sub = "[CWS] You have been assigned a task (CWS_TASK_NAME)";
			}

			if (cws_installer_mode.equals("interactive")) {
				print("Enter subject to be used in user task assignment emails.");
				print("Available tokens: ");
				print("  CWS_TASK_NAME      -- the name of the User task");
				print("  CWS_USER_EMAIL     -- the User's email address");
				print("  CWS_USER_FIRSTNAME -- the User's first name");
				print("  CWS_USER_LASTNAME  -- the User's last name");
				print("  (use <br/> to insert a newline)");
				print("Press enter, if you wish to accept this default subject: ");
				print("\"" + cws_task_assignment_sub +"\"");

				cws_task_assignment_sub = readLine("Or enter your own subject line: ",
						cws_task_assignment_sub);
			}

			cws_task_assignment_body = getPreset("email_body");

			if (cws_task_assignment_body == null) {
				cws_task_assignment_body = "Hi CWS_USER_FIRSTNAME,<br/>You have been assigned a User task (CWS_TASK_NAME."
						+ "<br/>Please log into the CWS Tasklist web application to check your assigned tasks.";
			}

			if (cws_installer_mode.equals("interactive")) {
				print("Enter body to be used in user task assignment emails.");
				print("Available tokens:\n");
				print("  CWS_TASK_NAME      -- the name of the User task");
				print("  CWS_USER_EMAIL     -- the User's email address");
				print("  CWS_USER_FIRSTNAME -- the User's first name");
				print("  CWS_USER_LASTNAME  -- the User's last name");
				print("  (use <br/> to insert a newline)\n\n");
				print("Press enter, if you wish to accept this default body: ");
				print("\"" + cws_task_assignment_body + "\"");
				cws_task_assignment_body = readLine("Or enter your own body text: ",
						cws_task_assignment_body);
			}

		}
		else {
			cws_task_assignment_sub = "N/A";
			cws_task_assignment_body = "N/A";
		}
	}

	private static void setupSMTP() {
		// PROMPT USER FOR SMTP HOSTNAME
		cws_smtp_hostname = getPreset("smtp_hostname");

		if (cws_smtp_hostname == null) {
			cws_smtp_hostname = getPreset("default_smtp_hostname");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_smtp_hostname = readLine("Enter the Simple Mail Transfer Protocol (SMTP) hostname. " +
					"Default is " + cws_smtp_hostname + ": ", cws_smtp_hostname);
		}

		// PROMPT USER FOR SMTP PORT
		cws_smtp_port = getPreset("smtp_port");

		if (cws_smtp_port == null) {
			cws_smtp_port = getPreset("default_smtp_port");
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_smtp_port = readLine("Enter the Simple Mail Transfer Protocol (SMTP) port. " +
					"Default is " + cws_smtp_port + ": ", cws_smtp_port);
		}
	}

	private static void setBrandHeader() {
		// PROMPT USER FOR BRAND HEADER
		cws_brand_header = getPreset("brand_header");

		if (cws_brand_header == null) {
			cws_brand_header = "CWS Dashboard";
		}

		if (cws_installer_mode.equals("interactive")) {
			cws_brand_header = readLine("Enter the brand header text that will display at the top of the web console. " +
					"Default is \"" + cws_brand_header + "\": ", cws_brand_header);
		}
	}


	private static void setProjectAppRoot() {
		// PROMPT USER FOR CUSTOM APP ROOT
		cws_project_webapp_root = getPreset("project_webapp_root");

		if (cws_installer_mode.equals("interactive")) {
			cws_project_webapp_root = readLine("Enter the name of a custom webapp root. Default is " +
					(cws_project_webapp_root == null ? "no custom webapp root" : cws_project_webapp_root) + ": ", cws_project_webapp_root);
		}
	}


	private static void setupCloudAutoscaling() {
		// PROMPT FOR __CWS_ENABLE_CLOUD_AUTOSCALING__
		cws_enable_cloud_autoscaling = getPreset("cws_enable_cloud_autoscaling");

		if (cws_installer_mode.equals("interactive")) {
			String read_cws_enable_cloud_autoscaling = "";

			while (!read_cws_enable_cloud_autoscaling.equalsIgnoreCase("y") &&
					!read_cws_enable_cloud_autoscaling.equalsIgnoreCase("n")) {
				read_cws_enable_cloud_autoscaling =
						readRequiredLine("Enable cloud (AWS) auto-scaling? (Y/N): ",
								"  ERROR: Must specify either 'Y' or 'N'");
			}

			cws_enable_cloud_autoscaling = read_cws_enable_cloud_autoscaling.toLowerCase();
		} else {
			if (cws_enable_cloud_autoscaling == null) {
				bailOutMissingOption("cws_enable_cloud_autoscaling");
			}
		}

		if (cws_enable_cloud_autoscaling.equalsIgnoreCase("Y")) {
			cws_enable_cloud_autoscaling = "true"; // convert
		}
		else {
			cws_enable_cloud_autoscaling = "false"; // convert
		}

		// PROMPT FOR __CWS_CLOUDWATCH_ENDPOINT__
		aws_cloudwatch_endpoint = getPreset("aws_cloudwatch_endpoint");

		if (aws_cloudwatch_endpoint == null) {
			aws_cloudwatch_endpoint = getPreset("default_aws_cloudwatch_endpoint");
		}

		if (cws_installer_mode.equals("interactive") && cws_enable_cloud_autoscaling.equals("true")) {
			aws_cloudwatch_endpoint = readLine("Enter the desired AWS CloudWatch endpoint. " +
					"Default is " + aws_cloudwatch_endpoint + ": ", aws_cloudwatch_endpoint);
		}

		// PROMPT FOR __CWS_METRICS_PUBLISHING_INTERVAL__
		metrics_publishing_interval = getPreset("metrics_publishing_interval");

		if (metrics_publishing_interval == null) {
			metrics_publishing_interval = getPreset("default_metrics_publishing_interval");
		}

		if (cws_installer_mode.equals("interactive") && cws_enable_cloud_autoscaling.equals("true")) {
			metrics_publishing_interval = readLine("Enter an interval in seconds to publish metrics to AWS CloudWatch. " +
					"Default is " + metrics_publishing_interval + ": ", metrics_publishing_interval);
		}
	}

	private static void setupAws() {
		aws_default_region = getPreset("aws_default_region");

		// default to us-west-2
		if(aws_default_region == null) {
			aws_default_region = "us-west-2";
		}
	}

	private static void setupAwsSqs() {
		aws_sqs_dispatcher_sqsUrl = getPreset("aws_sqs_dispatcher_sqsUrl");
		aws_sqs_dispatcher_msgFetchLimit = getPreset("aws_sqs_dispatcher_msgFetchLimit");
		if (aws_sqs_dispatcher_msgFetchLimit == null) {
			aws_sqs_dispatcher_msgFetchLimit = "1";
		}
		if (cws_installer_mode.equals("interactive")) {
			String enable_s3_initiation = "";

			while (!enable_s3_initiation.equalsIgnoreCase("y") &&
					!enable_s3_initiation.equalsIgnoreCase("n")) {
				enable_s3_initiation =
						readRequiredLine("Enable AWS S3 process initiation? (Y/N): ",
								"  ERROR: Must specify either 'Y' or 'N'");
			}

			if (enable_s3_initiation.equalsIgnoreCase("y")) {
				if (aws_sqs_dispatcher_sqsUrl == null) {
					aws_sqs_dispatcher_sqsUrl = readRequiredLine("Enter AWS SQS URL for S3 process initiation: ",
							"Must specify a URL!");
				} else {
					aws_sqs_dispatcher_sqsUrl = readLine("Enter AWS SQS URL for S3 process initiation. " +
							"Default is " + aws_sqs_dispatcher_sqsUrl + ": ", aws_sqs_dispatcher_sqsUrl);
				}

				// Always has a default, set above
				String read_aws_sqs_dispatcher_msgFetchLimit = "";
				while (!read_aws_sqs_dispatcher_msgFetchLimit.matches("^(?:[1-9]|0[1-9]|10)")) {
					aws_sqs_dispatcher_msgFetchLimit = readLine("Enter SQS message fetch limit (1 - 10). " +
							"Default is " + aws_sqs_dispatcher_msgFetchLimit + ": ", aws_sqs_dispatcher_msgFetchLimit);
				}
			}
		}
	}


	private static void setupStartupAutoregisterProcessDefs() {
		// PROMPT FOR __STARTUP_AUTOREGISTER_PROCESS_DEFS__
		startup_autoregister_process_defs = getPreset("startup_autoregister_process_defs");

		if (startup_autoregister_process_defs == null) {
			startup_autoregister_process_defs = getPreset("default_startup_autoregister_process_defs");
			log.debug("Setting default value of: " + startup_autoregister_process_defs + " for startup_autoregister_process_defs");
		}

		if (startup_autoregister_process_defs == null) {
			print("ERROR:  Must specify startup_autoregister_process_defs property.");
			exit(1);
		}
	}


	private static void genUniqueWorkerId() {
		// GENERATE WORKER ID
		// (deterministic based on hostname, install directory, and install time)
		Path filePath = Paths.get(cws_root + SEP + "config" + SEP + "worker_id");
		try {
			cws_worker_id = getFileContents(filePath);
		}
		catch (Exception e) {
			try {
				createFile(filePath);
				writeToFile(filePath, this_hostname.replace('.', '_').replace('-', '_') +
						"_" + Math.abs(cws_root.hashCode()) +
						"_" + System.currentTimeMillis());
				cws_worker_id = getFileContents(filePath);

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}


	private static void showInstallationInfo() {
		// DISPLAY INSTALLATION DETAILS
		print("+----------------------------------------------------------------------------------+");
		print("|   CWS INSTALLATION DETAILS                                                       |");
		print("+----------------------------------------------------------------------------------+");
		if (installType.equals("1")) {
			print("Install Type                  = Console AND Worker");
		}
		if (installType.equals("2")) {
			print("Install Type                  = Console Only");
		}
		if (installType.equals("3")) {
			print("Install Type                  = Worker Only");
		}
		print("Install Console?              = " + installConsole);
		print("Install Worker?               = " + installWorker);
		if (installWorker) {
			print("  Worker Type                 = " + cws_worker_type);
		}
		print("  Worker ID                   = " + cws_worker_id);
		print("  listen for proc start reqs? = " + cws_engine_proc_start_req_listener);
		print("  Auto-register PDs on start? = " + startup_autoregister_process_defs);
		print("....................................................................................");
		print("Database Type                 = " + cws_db_type);
		print("Database URL                  = " + cws_db_url);
		print("Database Driver               = " + cws_db_driver);
		print("Database User                 = " + cws_db_username);
		print("Database Password             = ****** (hidden) ");
		print("....................................................................................");
		if (cws_auth_scheme.equals("LDAP")) {
			print("LDAP User                     = " + cws_user);
		}
		else {
			print("Admin User ID                 = " + cws_user);
		}
		if (cws_auth_scheme.equals("CAMUNDA")) {
			print("Admin First Name              = " + cws_user_firstname);
			print("Admin Last Name               = " + cws_user_lastname);
			print("Admin Email                   = " + cws_user_email);
		}
		print("CWS web port                  = " + cws_tomcat_connector_port);
		print("CWS SSL port                  = " + cws_tomcat_ssl_port);
		print("CWS AJP port                  = " + cws_tomcat_ajp_port);
		print("CWS shutdown port             = " + cws_shutdown_port);
		print("CWS JMX port                  = " + cws_jmx_port);
		print("....................................................................................");
		if (installConsole) {
			print("CWS AMQ JMX port              = " + cws_amq_jmx_port);
		}
		print("CWS AMQ host                  = " + cws_amq_host);
		print("CWS AMQ port                  = " + cws_amq_port);
		print("....................................................................................");
		print("CWS Authentication scheme     = " + cws_auth_scheme);
		print("LDAP URL                      = " + cws_ldap_url);
		print("....................................................................................");
		print("Send task assignment emails?  = " + cws_send_user_task_assign_emails);
		print("Task assignment subject       = " + cws_task_assignment_sub);
		print("Task assignment body          = " + cws_task_assignment_body);
		print("....................................................................................");
		print("SMTP host                     = " + cws_smtp_hostname);
		print("SMTP port                     = " + cws_smtp_port);
		print("....................................................................................");
		print("Elasticsearch Protocol        = " + elasticsearch_protocol);
		print("Elasticsearch Host            = " + elasticsearch_host);
		print("Elasticsearch Port            = " + elasticsearch_port);
		if (elasticsearch_use_auth.equalsIgnoreCase("Y")) {
			print("Elasticsearch User            = " + elasticsearch_username);
			print("Elasticsearch Password        = ****** (hidden) ");
		}
		print("....................................................................................");
		if (user_provided_logstash.equalsIgnoreCase("Y")) {
			print("Logstash                      = User Provided");
		}
		else {
			print("Logstash                      = CWS Provided");
		}
		print("....................................................................................");
		print("CWS Notification Emails       = " + cws_notification_emails);
		print("CWS Token Expiration In Hours = " + cws_token_expiration_hours);
		print("History Level                 = " + history_level);
		print("Processes per Worker          = " + max_num_procs_per_worker);
		print("Days Remove Abandoned Workers = " + worker_abandoned_days);
		if (installConsole) {
			print("History Days to Live          = " + history_days_to_live);
			print("....................................................................................");
			print("Brand Header                  = " + cws_brand_header);
			if (cws_project_webapp_root != null && cws_project_webapp_root.equals("none")) {
				print("Project webapp root           = " + cws_project_webapp_root);
			}
			print("....................................................................................");
			print("Enable Cloud Auto-scaling?    = " + cws_enable_cloud_autoscaling);

			if (cws_enable_cloud_autoscaling.equals("true")) {
				print("AWS CloudWatch Endpoint        = " + aws_cloudwatch_endpoint);
				print("Metrics publishing interval    = " + metrics_publishing_interval);
			}
			if(aws_sqs_dispatcher_sqsUrl != null) {
				print("AWS Region                    = " + aws_default_region);
				print("AWS SQS Dispatcher URL        = " + aws_sqs_dispatcher_sqsUrl);
				print("AWS SQS Fetch Limit           = " + aws_sqs_dispatcher_msgFetchLimit);
			}
		}
		print("....................................................................................");
	}


	/*
	 * Validates the installation configuration.
	 *
	 */
	private static void validateConfig() {
		int warningCount = 0;

		print("");
		print("+----------------------------------------------------------------------------------+");
		print("VALIDATING CONFIGURATION...  Please wait, this may take some time...");

		warningCount += validateDbConfig();
		warningCount += validateTomcatPorts();
		boolean timeSyncMissing = validateTimeSyncService() == 1;

		if (timeSyncMissing) {
			warningCount++;
		}

		// Check that user provided Elasticsearch service is up and healthy
		warningCount += validateElasticsearch();

		if (installWorker && !installConsole) {
			// Validate the AMQ host/port for worker only installations.
			warningCount += validateAmqConfig();
		}
		else if (installConsole) {
			// Validate AMQ port range for console installations
			warningCount += validateAmqPort();
		}

		if (warningCount > 0) {
			print("");
			print("*******************************");
			print("*******************************");
			print("******** WARNING !!!!! ********");
			print("*******************************");
			print("*******************************");
			print("+--------------------------------------------------------+");
			print("|   " + warningCount + " POTENTIAL PROBLEMS IDENTIFIED WITH CONFIGURATION   |");
			print("+--------------------------------------------------------+");

			if (cws_installer_mode.equals("interactive")) {
				String continueResponse = "";

				while (!continueResponse.equalsIgnoreCase("y") &&
						!continueResponse.equalsIgnoreCase("n")) {
					continueResponse =
							readRequiredLine("Problems were identified (see above). Proceed with installation? (Y/N): ",
									"  ERROR: Must specify either 'Y' or 'N'");
				}

				if (continueResponse.equalsIgnoreCase("N")) {
					bailOutWithMessage("Aborting configuration. Please review the problems, and attempt again.");
				}

				print("Okay. Proceed at your own risk...");
			}
			else if (timeSyncMissing && warningCount == 1) {
				// Only warning present is time sync daemon missing... In this case, just continue with more warning message.
				print("Time sync daemon not found.  Will not abort install as this may not be a problem on some systems (e.g. inside Docker).");
			}
			else {
				// If there are other warnings not related to time sync daemon missing, then abort
				bailOutWithMessage(warningCount + " potential problems identified during configuration. Aborting...");
			}
		}
		else {
			print("No issues found with installation configuration.");
			print("");
		}
	}


	/**
	 * Validates the database configuration.
	 *
	 */
	private static int validateDbConfig() {
		int warningCount = 0;

		// CHECK DB HOST, DB PORT ACCESSIBILITY
		print("");
		print("checking DB host (" + cws_db_host + ":" + cws_db_port + ") accessibility...");
		if (!serverListening(cws_db_host, Integer.valueOf(cws_db_port))) {
			print("   [WARNING]");
			print("       " + cws_db_host + ":" + cws_db_port + " DOES NOT SEEM TO BE ACCEPTING tcp connections.");
			warningCount++;
		}
		else {
			print("   [OK]  (" + cws_db_host + ":" + cws_db_port + ") is ACCEPTING tcp connections.");
		}

		Connection connection = null;

		if (cws_db_password == null || cws_db_password.length() < 1) {
			cws_db_password = null;
		}
		if ( cws_db_type.equals("mysql") || cws_db_type.equals("mariadb")) {
			print("");
			print("checking database configuration...");
			try {
				Class.forName(cws_db_driver);

				boolean camunda_schema_exists = false;
				connection = DriverManager.getConnection(cws_db_url, cws_db_username, cws_db_password);
				Statement statm = connection.createStatement();

				// ---------------------------------------
				// CHECK FOR EXISTENCE OF DATABASE SCHEMA
				camunda_schema_exists = statm.execute("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME=\'" + cws_db_name + "\'");

				if (!camunda_schema_exists) {
					print("   [WARNING]");
					print("       Unable to connect to DB and/or detect " + cws_db_name + " database schema at host "
							+ cws_db_host + ":" + cws_db_port + ", with username/password");
					print("       This problem may be due to insufficient permissions on the database.");
					print("       Try running: select * from mysql.user where User=\'" + cws_db_username + "\'");
					print("       Sometimes the 'Host' column needs to be fully qualified");
					print("       (i.e. \'" + cws_db_host + ".domain.org\' instead of just \'" + cws_db_host +"\'");
					print("       Also, always remember to flush! (your privileges, that is)");
					warningCount++;
				}
				else {
					print("   [OK]");
				}

				// ------------------------------------------------
				// CHECK THAT DATABASE SYSTEM TIME IS CLOSE ENOUGH
				// TO THIS INSTALLATION'S SYSTEM TIME
				print("");
				print("checking that database timestamp is consistent with this installation timestamp...");
				long q0 = System.currentTimeMillis();
				Timestamp thisMachineTime = new Timestamp(DateTime.now().getMillis());
				Timestamp databaseTime;

				try (ResultSet rs = statm.executeQuery("SELECT CURRENT_TIMESTAMP()")) {
					rs.next();
					databaseTime = rs.getTimestamp(1);
				}

				String dbTimeZone;

				try (ResultSet rs1 = statm.executeQuery("SELECT @@global.time_zone")) {
					rs1.next();
					dbTimeZone = rs1.getString(1);
				}

				long q1 = System.currentTimeMillis();
				long queryTime = q1 - q0;
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

				long timeDiff = Math.abs(databaseTime.getTime() - thisMachineTime.getTime()) - queryTime;
				if (timeDiff > SYSTEM_TIME_OFFSET_THRESHOLD_MILLIS) {
					TimeZone tz = Calendar.getInstance().getTimeZone();
					print("   [WARNING]");
					print("       System time differs from database system time ");
					print("       more than allowable threshold of " + SYSTEM_TIME_OFFSET_THRESHOLD_MILLIS + " milliseconds.");
					print("       This may cause instability and unexpected results.");
					print("       Please ensure all systems have their system times synced up ");
					print("       via a mechanism such as NTP, and are using the same time zone. ");
					print("       See:  https://en.wikipedia.org/wiki/Network_Time_Protocol");
					print("       If working on the AWS cloud, be sure to update the database parameters ");
					print("       group with the new timezone to the same time zone.");
					print("       See: http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_WorkingWithParamGroups.html");
					print("       -----------------------------------------------------------");
					print("       INSTALL TS: " + sdf.format(thisMachineTime)); // Format the date using the specified pattern.
					print("            DB TS: " + sdf.format(databaseTime)); // Format the date using the specified pattern.
					print("       INSTALL TZ: " + tz.getDisplayName());
					print("            DB TZ: " + dbTimeZone);
					print("       QUERY TIME: " + queryTime + " (time it took to check time of DB)");
					print("       TIME DELTA: " + timeDiff + " milliseconds (diff of times, minus query time)");
					print("       -----------------------------------------------------------");
					warningCount++;
				}
				else {
					print("   [OK]");
				}

				try {
					statm.close();
				} catch (SQLException e) {
					log.error("Unexpected SQLException while closing Statement (" + e.getMessage() + ")");
				}
			}
			catch(ClassNotFoundException ex) {
				log.error("Error: unable to load database driver class (" + cws_db_driver + ")!");
				warningCount++;
			}
			catch (SQLException e) {
				print("   [WARNING]");
				print("      Could not connect to the database (" + cws_db_url + "), so skipping schema check.");
				print("      Please make sure that the following information in your configuration is correct:");
				print("          Database Host: " + cws_db_host);
				print("          Database Port: " + cws_db_port);
				print("          Database Name: " + cws_db_name + "   (make sure this database exists and is empty)");
				print("");
				print("      Also, make sure the database is configured to have permissions for the '" + cws_db_username + "' user, from this host.");
				print("");
				warningCount++;
			}
			finally {
				if (connection != null) {
					try {
						connection.close();
					}
					catch (SQLException e) {
						log.error("Unexpected SQLException while closing connection (" + e.getMessage() + ")");
					}
				}
			}
		}
		return warningCount;
	}


	private static int validateTomcatPorts() {
		int warningCount = 0;
		// VALIDATE CWS_TOMCAT_CONNECTOR_PORT
		print("");
		print("checking availability of tomcat connector port (" + cws_tomcat_connector_port + ")...");
		if (!isLocalPortAvailable(Integer.valueOf(cws_tomcat_connector_port))) {
			print("   [WARNING]");
			print("      CWS web port  " + cws_tomcat_connector_port + " seems to be taken or out of range.");
			print("");
			warningCount++;
		}
		else if (Integer.valueOf(cws_tomcat_connector_port) < 1024) {
			print("   [WARNING]");
			print("      CWS web port is less than 1024.");
			print("");
			warningCount++;
		}
		else {
			print("   [OK]");
			print("");
		}

		// VALIDATE CWS_TOMCAT_AJP_PORT
		print("checking availability of tomcat AJP port (" + cws_tomcat_ajp_port + ")...");
		if (!isLocalPortAvailable(Integer.valueOf(cws_tomcat_ajp_port))) {
			print("   [WARNING]");
			print("      CWS Tomcat AJP port " + cws_tomcat_ajp_port + " seems to be taken or out of range.");
			print("");
			warningCount++;
		}
		else if (Integer.valueOf(cws_tomcat_ajp_port) < 1024) {
			print("   [WARNING]");
			print("      CWS Tomcat AJP port is less than 1024.");
			print("");
			warningCount++;
		}
		else {
			print("   [OK]");
			print("");
		}

		// VALIDATE CWS_SHUTDOWN_PORT
		print("checking availability of shutdown port (" + cws_shutdown_port + ")...");
		if (!isLocalPortAvailable(Integer.valueOf(cws_shutdown_port))) {
			print("   [WARNING]");
			print("      CWS shutdown port " + cws_shutdown_port + " seems to be taken or out of range.");
			print("");
			warningCount++;
		}
		else if (Integer.valueOf(cws_shutdown_port) < 1024) {
			log.warn("CWS shutdown port is less than 1024.");
			warningCount++;
		}
		else {
			print("   [OK]");
			print("");
		}

		return warningCount;
	}

	private static int validateAmqConfig() {
		int warningCount = 0;
		//Validate AMQ host and port
		print("checking that AMQ endpoint (" + cws_amq_host + ":" + cws_amq_port + ") is reachable and listening...");
		if (!isRemotePortListening(cws_amq_host, Integer.valueOf(cws_amq_port))) {
			print("   [WARNING]");
			print("      CWS AMQ host " + cws_amq_host + ":" + cws_amq_port + " is unreachable.");
			print("");
			warningCount++;
		}
		else {
			print("   [OK]");
			print("");
		}

		return warningCount;
	}

	private static int validateAmqPort() {
		int warningCount = 0;

		// VALIDATE AMQ_PORT
		print("checking availability of AMQ port (" + cws_amq_port + ")...");
		if (!isLocalPortAvailable(Integer.valueOf(cws_amq_port))) {
			print("   [WARNING]");
			print("      AMQ port " + cws_amq_port + " seems to be taken or out of range.");
			print("");
			warningCount++;
		}
		else if (Integer.valueOf(cws_amq_port) < 1024) {
			log.warn("AMQ port is less than 1024.");
			warningCount++;
		}
		else {
			print("   [OK]");
			print("");
		}

		return warningCount;
	}

	/**
	 * Validates that User Provided ElasticSearch is running
	 *
	 */
	private static int validateElasticsearch() {
		print("checking that user provided Elasticsearch (" + elasticsearch_protocol + "://" + elasticsearch_host + ":" + elasticsearch_port + ") is running...");

		try {
			if (!(elasticsearch_protocol.startsWith("http") || elasticsearch_protocol.startsWith("https")) ) {
				print("   [WARNING]");
				print("       It was determined that the user provided Elasticsearch endpoint protocol '" + elasticsearch_protocol + "' did not properly set or protocol to 'HTTP' OR 'HTTPS'");
				print("");
				return 1;
			}

			if (elasticsearch_protocol == "http" && elasticsearch_host_init.toLowerCase().startsWith("https") ||
				elasticsearch_protocol == "https" && elasticsearch_host_init.toLowerCase().startsWith("http")) {
				print("   [SETUP RESOLUTION]");
				print("       It was determined that the user provided elasticsearch_protocol and elasticsearch_host have mismatched protocol identifiers.");
				print("          elasticsearch_protocol=" + elasticsearch_protocol_init + "  ");
				print("          elasticsearch_host=" + elasticsearch_host_init + "  ");
				print("");
				print("       CWS Installation will default to using given elasticsearch_protocol value: " + elasticsearch_protocol_init + " ");
				print("");
			}

			String[] cmdArray = new String[] {"curl", "--fail", elasticsearch_protocol + "://" + elasticsearch_host + ":" + elasticsearch_port + "/_cluster/health"};

			if (elasticsearch_use_auth.equalsIgnoreCase("Y")) {
				// Add auth to curl
				cmdArray = new String[] {"curl", "--fail", "-u", elasticsearch_username + ":" + elasticsearch_password, elasticsearch_protocol + "://" + elasticsearch_host + ":" + elasticsearch_port + "/_cluster/health"};
			}

			Process p = Runtime.getRuntime().exec(cmdArray);

			// Wait for the process to complete
			//
			p.waitFor();

			if (p.exitValue() != 0) {
				print("   [WARNING]");
				print("       It was determined that the user provided Elasticsearch is not running or is inaccessible.");
				print("       .........................................................................................");
				print("           [ELASTICSEARCH]: Configuration Details");
				print("               elasticsearch_protocol=" + elasticsearch_protocol_init + "  ");
				print("               elasticsearch_host=" + elasticsearch_host_init + "  ");
				print("               elasticsearch_port=" + elasticsearch_port + "  ");
				print("       .........................................................................................");
				print("");

				return 1;
			}

			print("   [OK]");
			print("");

			return 0; // no warnings

		} catch (Exception e) {
			log.error("error: ", e);
			print("   [WARNING]");
			print("       There was a problem determining if the user provided Elasticsearch is running or not.");
			print("");
			return 1;
		}
	}

	/**
	 * Validates that some sort of time syncing service
	 * such as NTP or chrony is running on this installation machine.
	 *
	 */
	private static int validateTimeSyncService() {
		print("checking for a system time synchronization service...");
		try {
			String process;
			Process p = Runtime.getRuntime().exec("ps -e");
			BufferedReader input =
					new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((process = input.readLine()) != null) {
				//System.out.println(line);
				if (process.contains("ntpd") ||
					process.contains("chronyd") ||
					process.contains("usr/libexec/timed")) {
					print("   [OK]");
					print("");
					return 0; // no warnings
				}
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}

		print("   [WARNING]");
		print("       No time synchronization service was detected on this system.");
		print("       All machines (console, database, and all workers) must have the same timestamp ");
		print("       for CWS to work properly.  It is recommended that all machines use a time ");
		print("       synchronization service such as NTP or chrony running to assure the full system ");
		print("       is time-synchronized.  The time zones of the machines should also be the same. ");
		TimeZone tz = Calendar.getInstance().getTimeZone();
		print("       TIMEZONE ON THIS MACHINE: " + tz.getDisplayName());
		print("       Please work with a System Administrator to make sure your machine is time-synced. ");
		print("       See:  https://en.wikipedia.org/wiki/Network_Time_Protocol");
		print("       See:  https://chrony.tuxfamily.org");

		return 1; // add to warning count
	}


	private static void acceptInstallationConfig() {
		String reply = getPreset("auto_accept_config");
		if (reply == null) {
			reply = readRequiredLine("Please review the installation details above.  Proceed with installation? (Y/N) ",
					"  ERROR: Must specify either 'Y' or 'N'");
			while ( !reply.equalsIgnoreCase("y") && !reply.equalsIgnoreCase("n")) {
				reply = readRequiredLine("Please review the installation details above.  Proceed with installation? (Y/N) ",
						"  ERROR: Must specify either 'Y' or 'N'");
			}
			if (reply.equalsIgnoreCase("n")) {
				exit(1);
			}
		}
		else {
			if (!reply.equalsIgnoreCase("y")) {
				print(  "ERROR:  auto_accept_config had unexpected value of '" + reply + "'.  Aborting installation...");
				exit(1);
			}
		}
	}


	private static void createFreshWorkDir() {
		deleteDirectory(new File(config_work_dir));
		mkDir(config_work_dir);
		mkDir(config_work_dir + SEP + "cws-engine");
		mkDir(config_work_dir + SEP + "cws-ui");
		mkDir(config_work_dir + SEP + "tomcat_lib");
		mkDir(config_work_dir + SEP + "tomcat_bin");
		mkDir(config_work_dir + SEP + "tomcat_conf");
		mkDir(config_work_dir + SEP + "camunda_mods");
		mkDir(config_work_dir + SEP + "engine-rest_mods");
		mkDir(config_work_dir + SEP + "logging");


		copy(Paths.get(config_templates_dir + SEP + "refresh_cws_token.sh"),
				Paths.get(config_work_dir + SEP + "refresh_cws_token.sh"));
		copy(Paths.get(config_templates_dir + SEP + "stop_cws.sh"),
				Paths.get(config_work_dir + SEP + "stop_cws.sh"));
		copy(Paths.get(config_templates_dir + SEP + "clean_es_history.sh"),
				Paths.get(config_work_dir + SEP + "clean_es_history.sh"));
		copy(Paths.get(config_templates_dir + SEP + "tomcat_lib" + SEP + "css-jaas.cfg"),
				Paths.get(config_work_dir + SEP + "tomcat_lib" + SEP + "css-jaas.cfg"));
		copy(Paths.get( config_templates_dir + SEP + "cws-engine" + SEP + "applicationContext.xml"),
				Paths.get(config_work_dir + SEP + "cws-engine" + SEP +  "applicationContext.xml"));
		copy(Paths.get( config_templates_dir + SEP + "cws-engine" + SEP + "process_start_req_listener.xml"),
				Paths.get(config_work_dir + SEP + "cws-engine" + SEP +  "process_start_req_listener.xml"));
		copy(Paths.get( config_templates_dir + SEP + "cws-engine" + SEP + "cws-engine.properties"),
				Paths.get(config_work_dir + SEP + "cws-engine" + SEP +  "cws-engine.properties"));
		copy(Paths.get( config_templates_dir + SEP + "cws-ui" + SEP + "cws-ui.properties"),
				Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "cws-ui.properties"));
		copy(Paths.get( config_templates_dir + SEP + "cws-ui" + SEP + "applicationContext.xml"),
				Paths.get(config_work_dir + SEP + "cws-ui" + SEP +  "applicationContext.xml"));
		copyAllType(
				config_templates_dir + SEP + "cws-ui",
				config_work_dir + SEP + "cws-ui", "ftl");
		copy(Paths.get( config_templates_dir + SEP + "cws-ui" + SEP + "sqs_dispatcher_thread_bean.xml"),
				Paths.get(config_work_dir + SEP + "cws-ui" + SEP +  "sqs_dispatcher_thread_bean.xml"));
		copy(Paths.get( config_templates_dir + SEP + "tomcat_bin" + SEP + "setenv.sh"),
				Paths.get(config_work_dir + SEP + "tomcat_bin" + SEP +  "setenv.sh"));
		copy(Paths.get( config_templates_dir + SEP + "tomcat_conf" + SEP + "bpm-platform.xml"),
				Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP +  "bpm-platform.xml"));
		copy(Paths.get( config_templates_dir + SEP + "tomcat_conf" + SEP + "server.xml"),
				Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP +  "server.xml"));
		copy(Paths.get( config_templates_dir + SEP + "tomcat_conf" + SEP + "web.xml"),
				Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP +  "web.xml"));
		copy(Paths.get( config_templates_dir + SEP + "engine-rest_mods" + SEP + "web.xml"),
				Paths.get(config_work_dir + SEP + "engine-rest_mods" + SEP +  "web.xml"));
		copy(Paths.get( config_templates_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_bean.xml"),
				Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP +  "ldap_plugin_bean.xml"));
		copy(Paths.get( config_templates_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_ref.xml"),
				Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP +  "ldap_plugin_ref.xml"));
		copy(Paths.get( config_templates_dir + SEP + "camunda_mods" + SEP + "web.xml"),
				Paths.get(config_work_dir + SEP + "camunda_mods" + SEP +  "web.xml"));
		copy(Paths.get( config_templates_dir + SEP + "logging" + SEP + "cws-logstash.conf"),
				Paths.get(config_work_dir + SEP + "logging" + SEP +  "cws-logstash.conf"));
	}


	private static void updateFiles() throws IOException {
		Path filePath;
		String content;
		Path replaceFilePath;
		String replaceContent;

		// Update refresh_cws_token.sh file
		//
		filePath = Paths.get(config_work_dir + SEP + "refresh_cws_token.sh");
		content = getFileContents(filePath);
		content = content.replace("__CWS_CONSOLE_HOST__",      cws_console_host);
		content = content.replace("__CWS_CONSOLE_SSL_PORT__",  cws_console_ssl_port);
		content = content.replace("__CWS_AUTH_SCHEME__",       cws_auth_scheme);
		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "refresh_cws_token.sh"),
			Paths.get(cws_root + SEP + "refresh_cws_token.sh"));

		if (installConsole) {
			updateCwsUiConfig();
			updateCwsUiProperties();
		}

		// Update stop_cws.sh file
		//
		filePath = Paths.get(config_work_dir + SEP + "stop_cws.sh");
		content = getFileContents(filePath);
		content = content.replace("__CWS_CONSOLE_HOST__",     cws_console_host);
		content = content.replace("__CWS_CONSOLE_SSL_PORT__", cws_console_ssl_port);
		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "stop_cws.sh"),
			Paths.get(cws_root + SEP + "stop_cws.sh"));

		// UPDATE bpm-platform.xml
		//
		copy(
			Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "bpm-platform.xml"),
			Paths.get(cws_tomcat_root + SEP + "conf" + SEP + "bpm-platform.xml"));

		// COPY css-jaas.cfg to tomcat/lib
		copy(
			Paths.get(config_work_dir + SEP + "tomcat_lib" + SEP + "css-jaas.cfg"),
			Paths.get(cws_tomcat_root + SEP + "lib" + SEP + "css-jaas.cfg"));

		// UPDATE server.xml
		print(" Updating server.xml (to point to CWS database)...");
		filePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "server.xml");
		content = getFileContents(filePath);
		content = content.replace("__CWS_DB_URL__",                cws_db_url);
		content = content.replace("__CWS_DB_DRIVER__",             cws_db_driver);
		content = content.replace("__CWS_DB_USERNAME__",           cws_db_username);
		content = content.replace("__CWS_DB_PASSWORD__",           cws_db_password);
		content = content.replace("__CWS_TOMCAT_CONNECTOR_PORT__", cws_tomcat_connector_port);
		content = content.replace("__CWS_TOMCAT_SSL_PORT__",       cws_tomcat_ssl_port);
		content = content.replace("__CWS_TOMCAT_AJP_PORT__",       cws_tomcat_ajp_port);
		content = content.replace("__CWS_SHUTDOWN_PORT__",         cws_shutdown_port);
		content = content.replace("__CWS_TOMCAT_CONF_DIR__",       cws_tomcat_conf);

		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "server.xml"),
			Paths.get(cws_tomcat_root + SEP + "conf"        + SEP + "server.xml"));

		updateWorkerAppContext();
		updateWorkerProperties();

		// UPDATE tomcat/bin/setenv.sh
		filePath = Paths.get(config_work_dir + SEP + "tomcat_bin" + SEP + "setenv.sh");
		print(" Updating " + filePath + "...");
		content = getFileContents(filePath);
		content = content.replace("__CWS_JMX_PORT__", cws_jmx_port);
		content = content.replace("__CWS_TOMCAT_ROOT__", cws_tomcat_root);
		writeToFile(filePath, content);
		copy(filePath, Paths.get(cws_tomcat_bin + SEP + "setenv.sh"));

		// UPDATE tomcat/conf/web.xml
		print(" Updating tomcat/conf/web.xml..");
		filePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "web.xml");
		content = getFileContents(filePath);
		content = content.replace("__CWS_IDENTITY_PLUGIN_TYPE__",     cws_auth_scheme);
		content = content.replace("__CWS_SECURITY_FILTER_CLASS__",    cws_security_filter_class);
		content = content.replace("__CWS_ADMIN_USERNAME__",           cws_user);
		content = content.replace("__CWS_TOKEN_EXPIRATION_MINUTES__", Integer.toString(Integer.parseInt(cws_token_expiration_hours) * 60));

		if (cws_auth_scheme.equals("CAMUNDA")) {
			content = content.replace("__CWS_ADMIN_FIRSTNAME__",       cws_user_firstname);
			content = content.replace("__CWS_ADMIN_LASTNAME__",        cws_user_lastname);
			content = content.replace("__CWS_ADMIN_EMAIL__",      	   cws_user_email);
		}
		else {
			Path pluginBeanFilePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_bean.xml");
			String[] identityAttr = getIdentityPluginAttribute(pluginBeanFilePath, cws_user, cws_ldap_url);
			content = content.replace("__CWS_ADMIN_FIRSTNAME__",         		identityAttr[0]);
			content = content.replace("__CWS_ADMIN_LASTNAME__",         			identityAttr[1]);
			content = content.replace("__CWS_ADMIN_EMAIL__",         			identityAttr[2]);
		}
		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "web.xml"),
			Paths.get(cws_tomcat_root + SEP + "conf" + SEP + "web.xml"));

		// UPDATE camunda/web.xml
		print(" Updating webapps/camunda/WEB_INF/web.xml..");
		filePath = Paths.get(config_work_dir + SEP + "camunda_mods" + SEP + "web.xml");
		content = getFileContents(filePath);
		content = content.replace("__CWS_IDENTITY_PLUGIN_TYPE__",  cws_auth_scheme);
		content = content.replace("__CWS_SECURITY_FILTER_CLASS__", cws_security_filter_class);

		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "camunda_mods" + SEP + "web.xml"),
			Paths.get(cws_tomcat_webapps + SEP + "camunda" + SEP + "WEB-INF" + SEP + "web.xml"));

		// UPDATE engine-rest/web.xml
		print(" Updating webapps/engine-rest/WEB_INF/web.xml..");
		filePath = Paths.get(config_work_dir + SEP + "engine-rest_mods" + SEP + "web.xml");
		content = getFileContents(filePath);
		content = content.replace("__CWS_IDENTITY_PLUGIN_TYPE__",  cws_auth_scheme);
		content = content.replace("__CWS_SECURITY_FILTER_CLASS__", cws_security_filter_class);
		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "engine-rest_mods" + SEP + "web.xml"),
			Paths.get(cws_tomcat_webapps + SEP + "engine-rest" + SEP + "WEB-INF" + SEP + "web.xml"));

		deleteDirectory(new File(cws_tomcat_webapps + SEP + "h2"));
		deleteDirectory(new File(cws_tomcat_webapps + SEP + "manager"));
		deleteDirectory(new File(cws_tomcat_webapps + SEP + "host-manager"));

		if (!installConsole) {

			deleteDirectory(new File(cws_tomcat_webapps + SEP + "ROOT"));
			deleteDirectory(new File(cws_tomcat_webapps + SEP + "camunda"));
			deleteDirectory(new File(cws_tomcat_webapps + SEP + "engine-rest"));
		}
		else {
			// Check if exists since file may have been moved from a previous "configure" run
			if (new File(cws_tomcat_webapps + SEP + "ROOT" + SEP + "index.html").exists()) {

				move(
						Paths.get(cws_tomcat_webapps + SEP + "ROOT" + SEP + "index.html"),
						Paths.get(cws_tomcat_webapps + SEP + "ROOT" + SEP + "redirect.html"));
			}
		}
	}


	/**
	 * Updates cws-engine.properties file.
	 *
	 */
	private static void updateWorkerProperties() throws IOException {
		print(" Updating cws-engine/cws-engine.properties...");
		Path filePath = Paths.get(config_work_dir + SEP + "cws-engine" + SEP + "cws-engine.properties");
		String content = getFileContents(filePath);
		content = content.replace("__CWS_JMX_PORT__",                      cws_jmx_port);
		content = content.replace("__CWS_SEND_USER_TASK_ASSIGN_EMAILS__",  cws_send_user_task_assign_emails);
		content = content.replace("__CWS_TASK_ASSIGNMENT_SUBJ__",          cws_task_assignment_sub);
		content = content.replace("__CWS_TASK_ASSIGNMENT_BODY__",          cws_task_assignment_body);
		content = content.replace("__CWS_ENGINE_JOBEXECUTOR_ENABLED__",    cws_engine_jobexecutor_enabled);
		content = content.replace("__CWS_INSTALL_HOSTNAME__",              this_hostname);
		content = content.replace("__CWS_CONSOLE_HOST__",                  cws_console_host);
		content = content.replace("__CWS_CONSOLE_SSL_PORT__",              cws_console_ssl_port);
		content = content.replace("__CWS_WORKER_ID__",                     cws_worker_id);
		content = content.replace("__CWS_WORKER_TYPE__",                   cws_worker_type);
		content = content.replace("__CWS_INSTALL_DIR__",                   cws_root);
		content = content.replace("__CWS_TOMCAT_LIB__",                    cws_tomcat_lib);
		content = content.replace("__CWS_TOMCAT_BIN__",                    cws_tomcat_bin);
		content = content.replace("__CWS_TOMCAT_HOME__",                   cws_tomcat_root);
		content = content.replace("__CWS_TOMCAT_WEBAPPS__",                cws_tomcat_webapps);
		content = content.replace("__CWS_AUTH_SCHEME__",                   cws_auth_scheme);
		content = content.replace("__STARTUP_AUTOREGISTER_PROCESS_DEFS__", startup_autoregister_process_defs);

		if (cws_auth_scheme.equalsIgnoreCase("CAMUNDA")) {
			content = content.replace("__CWS_ADMIN_FIRSTNAME__",         		cws_user_firstname);
			content = content.replace("__CWS_ADMIN_LASTNAME__",         			cws_user_lastname);
			content = content.replace("__CWS_ADMIN_EMAIL__",         			cws_user_email);
		} else {
			Path pluginBeanFilePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_bean.xml");
			String[] identityAttr = getIdentityPluginAttribute(pluginBeanFilePath, cws_user, cws_ldap_url);
			content = content.replace("__CWS_ADMIN_FIRSTNAME__",         		identityAttr[0]);
			content = content.replace("__CWS_ADMIN_LASTNAME__",         			identityAttr[1]);
			content = content.replace("__CWS_ADMIN_EMAIL__",         			identityAttr[2]);
		}

		content = content.replace("__CWS_NOTIFICATION_EMAILS__",           cws_notification_emails);
		content = content.replace("__CWS_TOKEN_EXPIRATION_HOURS__",        cws_token_expiration_hours);
		content = content.replace("__CWS_SMTP_HOSTNAME__",                 cws_smtp_hostname);
		content = content.replace("__CWS_SMTP_PORT__",                     cws_smtp_port);
		content = content.replace("__CWS_DB_HOST__",                       cws_db_host);
		content = content.replace("__CWS_DB_PORT__",                       cws_db_port);
		content = content.replace("__CWS_DB_NAME__",                       cws_db_name);
		content = content.replace("__CWS_DB_USERNAME__",                   cws_db_username);
		content = content.replace("__CWS_DB_PASSWORD__",                   cws_db_password);
		content = content.replace("__CAMUNDA_EXEC_SVC_MAX_POOL_SIZE__",    CORES+"");
		content = content.replace("__AWS_DEFAULT_REGION__", 				  aws_default_region);
		content = content.replace("__CWS_MAX_NUM_PROCS_PER_WORKER__",		max_num_procs_per_worker);

		// S3 Initiator might not be in use
		if(aws_sqs_dispatcher_sqsUrl != null) {
			content = content.replace("__AWS_SQS_DISPATCHER_SQS_URL__", aws_sqs_dispatcher_sqsUrl);
			content = content.replace("__AWS_SQS_DISPATCHER_MSG_FETCH_LIMIT__", aws_sqs_dispatcher_msgFetchLimit);
		}


		if (installType.equals("1")) {
			content = content.replace("__CWS_INSTALL_TYPE__", "console_and_worker");
		}
		if (installType.equals("2")) {
			content = content.replace("__CWS_INSTALL_TYPE__", "console_only");
		}
		if (installType.equals("3")) {
			content = content.replace("__CWS_INSTALL_TYPE__", "worker_only");
		}
		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "cws-engine" + SEP + "cws-engine.properties"),
			Paths.get(cws_tomcat_webapps + SEP + "cws-engine" + SEP + "WEB-INF" + SEP + "classes" + SEP + "cws-engine.properties"));
	}


	private static void updateWorkerAppContext() throws IOException {
		print(" Updating cws-engine/applicationContext.xml...");
		Path filePath = Paths.get(config_work_dir + SEP + "cws-engine" + SEP + "applicationContext.xml");
		String content = getFileContents(filePath);

		content = content.replace("__CWS_DB_DRIVER__", cws_db_driver);
		content = content.replace("__CWS_DB_URL__", cws_db_url);
		content = content.replace("__CWS_DB_USERNAME__", cws_db_username);
		content = content.replace("__CWS_DB_PASSWORD__", cws_db_password);
		content = content.replace("__JOB_EXECUTOR_ACTIVATE__", cws_engine_jobexecutor_enabled);
		content = content.replace("__HISTORY_LEVEL__", history_level);

		if (cws_engine_proc_start_req_listener.equalsIgnoreCase("y")) {
			//Fill in the __PROC_START_REQ_LISTENER_XML__ placeholder
			Path replaceFilePath = Paths.get(config_work_dir + SEP + "cws-engine" + SEP + "process_start_req_listener.xml");
			String replaceContent = getFileContents(replaceFilePath);
			content = content.replace("__PROC_START_REQ_LISTENER_XML__", replaceContent);
		}
		else {
			content = content.replace("__PROC_START_REQ_LISTENER_XML__", ""); // erase token
		}
		content = content.replace("__CWS_AMQ_HOST__", cws_amq_host);
		content = content.replace("__CWS_AMQ_PORT__", cws_amq_port);

	content = updateIdentityPluginContent(content);

		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "cws-engine" + SEP + "applicationContext.xml"),
			Paths.get(cws_tomcat_webapps + SEP + "cws-engine" + SEP + "WEB-INF" + SEP + "applicationContext.xml"));
	}


	/**
	 * Updates cws-ui.properties file.
	 *
	 */
	private static void updateCwsUiProperties() throws IOException {
		print(" Updating cws-ui.properties...");
		Path filePath = Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "cws-ui.properties");
		String content = getFileContents(filePath);
		content = content.replace("__CWS_VERSION__",                     cws_version);
		content = content.replace("__CWS_AMQ_JMX_PORT__",                cws_amq_jmx_port);
		content = content.replace("__CWS_AMQ_HOST__",                    cws_amq_host);
		content = content.replace("__CWS_INSTALL_HOSTNAME__",            this_hostname);
		content = content.replace("__CWS_CONSOLE_HOST__",                cws_console_host);
		content = content.replace("__CWS_DB_TYPE__",                     cws_db_type);
		content = content.replace("__CWS_DB_HOST__",                     cws_db_host);
		content = content.replace("__CWS_DB_NAME__",                     cws_db_name);
		content = content.replace("__CWS_DB_PORT__",                     cws_db_port);
		content = content.replace("__CWS_DB_USERNAME__",                 cws_db_username);
		content = content.replace("__CWS_DB_PASSWORD__",                 cws_db_password);
		content = content.replace("__CWS_CONSOLE_SSL_PORT__",            cws_console_ssl_port);
		content = content.replace("__CWS_ES_PROTOCOL__",                 elasticsearch_protocol);
		content = content.replace("__CWS_ES_HOST__",                     elasticsearch_host);
		content = content.replace("__CWS_ES_PORT__",                     elasticsearch_port);
		content = content.replace("__CWS_ES_USE_AUTH__",                 elasticsearch_use_auth);
		content = content.replace("__CWS_ENABLE_CLOUD_AUTOSCALING__",    cws_enable_cloud_autoscaling);
		content = content.replace("__CWS_CLOUDWATCH_ENDPOINT__",         aws_cloudwatch_endpoint);
		content = content.replace("__CWS_METRICS_PUBLISHING_INTERVAL__", metrics_publishing_interval);
		content = content.replace("__CWS_WORKER_ID__",                   cws_worker_id);
		content = content.replace("__CWS_INSTALL_DIR__",                 cws_root);
		content = content.replace("__CWS_TOMCAT_LIB__",                  cws_tomcat_lib);
		content = content.replace("__CWS_TOMCAT_BIN__",                    cws_tomcat_bin);
		content = content.replace("__CWS_TOMCAT_HOME__",                    cws_tomcat_root);
		content = content.replace("__CWS_TOMCAT_WEBAPPS__",                    cws_tomcat_webapps);
		content = content.replace("__CWS_PROJECT_WEBAPP_ROOT__",         (cws_project_webapp_root == null || cws_project_webapp_root.equals("none")) ? "" : cws_project_webapp_root);

		if (cws_auth_scheme.equalsIgnoreCase("CAMUNDA")) {
			content = content.replace("__CWS_ADMIN_FIRSTNAME__",         		cws_user_firstname);
			content = content.replace("__CWS_ADMIN_LASTNAME__",         			cws_user_lastname);
			content = content.replace("__CWS_ADMIN_EMAIL__",         			cws_user_email);
		} else {
			Path pluginBeanFilePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_bean.xml");
			String[] identityAttr = getIdentityPluginAttribute(pluginBeanFilePath, cws_user, cws_ldap_url);
			content = content.replace("__CWS_ADMIN_FIRSTNAME__",         		identityAttr[0]);
			content = content.replace("__CWS_ADMIN_LASTNAME__",         			identityAttr[1]);
			content = content.replace("__CWS_ADMIN_EMAIL__",         			identityAttr[2]);
		}

		content = content.replace("__CWS_NOTIFICATION_EMAILS__",         cws_notification_emails);
		content = content.replace("__CWS_TOKEN_EXPIRATION_HOURS__",      cws_token_expiration_hours);
		content = content.replace("__CWS_SMTP_HOSTNAME__",               cws_smtp_hostname);
		content = content.replace("__CWS_SMTP_PORT__",                   cws_smtp_port);
		content = content.replace("__CWS_AUTH_SCHEME__",                 cws_auth_scheme);
		content = content.replace("__CWS_HISTORY_DAYS_TO_LIVE__",        history_days_to_live);
		content = content.replace("__CWS_HISTORY_LEVEL__",     		     history_level);
		content = content.replace("__CWS_MAX_NUM_PROCS_PER_WORKER__",	max_num_procs_per_worker);
		content = content.replace("__CWS_WORKER_ABANDONED_DAYS__",		worker_abandoned_days);
		content = content.replace("__AWS_DEFAULT_REGION__", 				  aws_default_region);

		// ES auth might not be in use
		if(elasticsearch_use_auth.equalsIgnoreCase("Y")) {
			content = content.replace("__CWS_ES_USERNAME__", elasticsearch_username);
			content = content.replace("__CWS_ES_PASSWORD__", elasticsearch_password);
		}

		// S3 Initiator might not be in use
		if(aws_sqs_dispatcher_sqsUrl != null) {
			content = content.replace("__AWS_SQS_DISPATCHER_SQS_URL__", aws_sqs_dispatcher_sqsUrl);
			content = content.replace("__AWS_SQS_DISPATCHER_MSG_FETCH_LIMIT__", aws_sqs_dispatcher_msgFetchLimit);
		}

		writeToFile(filePath, content);
		copy(
			Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "cws-ui.properties"),
			Paths.get(cws_tomcat_webapps + SEP + "cws-ui" + SEP + "WEB-INF" + SEP + "classes" + SEP + "cws-ui.properties"));
	}


	private static void updateCwsUiConfig() throws IOException {
		print(" Updating cws-ui/applicationContext.xml...");
		Path path = Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "applicationContext.xml");
		String content = getFileContents(path);
		content = content.replace("__CWS_DB_DRIVER__", cws_db_driver);
		content = content.replace("__CWS_DB_URL__", cws_db_url);
		content = content.replace("__CWS_DB_USERNAME__", cws_db_username);
		content = content.replace("__CWS_DB_PASSWORD__", cws_db_password);
		content = content.replace("__JOB_EXECUTOR_ACTIVATE__", "false");
		content = content.replace("__HISTORY_LEVEL__", history_level);
		content = content.replace("__CWS_MAX_NUM_PROCS_PER_WORKER__", max_num_procs_per_worker);
		content = content.replace("__CWS_WORKER_ABANDONED_DAYS__", worker_abandoned_days);

		content = content.replace("__CWS_AMQ_HOST__",     cws_amq_host);
		content = content.replace("__CWS_AMQ_PORT__",     cws_amq_port);
		content = content.replace("__CWS_AMQ_JMX_PORT__", cws_amq_jmx_port);
		print(" replacing __CWS_ROOT_DIR__  with " + cws_root);
		content = content.replace("__CWS_ROOT_DIR__",     cws_root);

		// Fill in the __SQS_DISPATCHER_THREAD_BEAN__ if needed
		if(aws_sqs_dispatcher_sqsUrl != null) {
			String sqsDispatcherThreadBeanContent = getFileContents(
					Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "sqs_dispatcher_thread_bean.xml"));
			content = content.replace("__SQS_DISPATCHER_THREAD_BEAN__", sqsDispatcherThreadBeanContent);
		}
		// Remove it out otherwise
		else {
			content = content.replace("__SQS_DISPATCHER_THREAD_BEAN__", "");
		}

		unique_broker_group_name = cws_db_host + cws_db_port + cws_db_name;
		if (cws_db_host.equals("localhost")) {
			unique_broker_group_name = this_hostname + cws_db_port + cws_db_name;
		}
		content = content.replace("__UNIQUE_BROKER_GROUP_NAME__", unique_broker_group_name);

	content = updateIdentityPluginContent(content);

	writeToFile(path, content);

		copy(path,
			Paths.get(cws_tomcat_webapps + SEP + "cws-ui" + SEP + "WEB-INF" + SEP + "applicationContext.xml"));


		// Update clean_es_history.sh file
		path = Paths.get(config_work_dir + SEP + "clean_es_history.sh");
		content = getFileContents(path);
		content = content.replace("__ES_PROTOCOL__",      			elasticsearch_protocol);
		content = content.replace("__ES_HOST__",      				elasticsearch_host);
		content = content.replace("__ES_PORT__",  					elasticsearch_port);
		content = content.replace("__ES_USE_AUTH__",                 elasticsearch_use_auth);
		if (elasticsearch_use_auth.equalsIgnoreCase("Y")) {
			content = content.replace("__ES_USERNAME__",             elasticsearch_username);
			content = content.replace("__ES_PASSWORD__",             elasticsearch_password);
		} else {
			content = content.replace("__ES_USERNAME__",             "na");
			content = content.replace("__ES_PASSWORD__",             "na");
		}
		content = content.replace("__CWS_HISTORY_DAYS_TO_LIVE__", 	history_days_to_live);
		writeToFile(path, content);
		copy(
			Paths.get(config_work_dir + SEP + "clean_es_history.sh"),
			Paths.get(cws_root + SEP + "clean_es_history.sh"));


		// UPDATE cws-ui brand name in FTL files
		// UPDATE navbar project link
		print(" Updating cws-ui brand name...");
		path = Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "navbar.ftl");
		content = getFileContents(path);
		content = content.replace("__CWS_BRAND_HEADER__", cws_brand_header);

		String projectLink = "";

		if (cws_project_webapp_root != null && !cws_project_webapp_root.equals("none") && cws_project_webapp_root.length() > 1) {

			String linkName = cws_project_webapp_root.substring(0, 1).toUpperCase() + cws_project_webapp_root.substring(1);
			projectLink = "<li><a href=\"/" + cws_project_webapp_root + "\"><span class=\"glyphicon glyphicon-wrench\"></span> &nbsp;" + linkName + "</a></li>";
		}

		content = content.replace("__CWS_PROJECT_LINK__", projectLink);
		writeToFile(path, content);

		path = Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "not_authorized.ftl");
		content = getFileContents(path);
		content = content.replace("__CWS_BRAND_HEADER__", cws_brand_header);
		writeToFile(path, content);

		path = Paths.get(config_work_dir + SEP + "cws-ui" + SEP + "login.ftl");
		content = getFileContents(path);
		content = content.replace("__CWS_BRAND_HEADER__", cws_brand_header);
		writeToFile(path, content);

		String ftlDir = cws_tomcat_webapps + SEP + "cws-ui" + SEP + "WEB-INF" + SEP + "ftl";
		mkDir(ftlDir);
		copyAllType(config_work_dir + SEP + "cws-ui",  ftlDir, "ftl");

		// Make custom project webapp root directory, if one was specified
		if (cws_project_webapp_root != null && !cws_project_webapp_root.equals("none")) {
			mkDir(cws_tomcat_webapps + SEP + cws_project_webapp_root);
			Path indexHtml = Paths.get(cws_tomcat_webapps + SEP + cws_project_webapp_root + SEP + "index.html");
			writeToFile(indexHtml,
				"<html><head><meta http-equiv=\"refresh\" content=\"10;url=/cws-ui/\" /></head>" +
				"<body>You have configured CWS to have a project web page.\n" +
				"Put your custom content here by editing the '" + indexHtml + "' file...<br/><br/><hr/>" +
				"Automatically redirecting to <a href=\"/cws-ui\">CWS Home</a> in 10 seconds...</body></html>");
		}
	}


	private static String[] getIdentityPluginAttribute(Path beanFilePath, String user, String ldapURL) throws IOException {
		//
		// Get identity plugin properties and attributes
		//
		String propertyBase = "";
		String propertySearchBase = "";
		String[] identityAttributes = new String[3];
		String[] attributeFilter = {"givenName", "sn", "mail"};

		try {
			String fileContent = new String(Files.readAllBytes(beanFilePath));
			String repl = "";
			String replContent = fileContent.substring(0, fileContent.indexOf("<bean id=\"ldapIdentityProviderPlugin\""));
			fileContent = fileContent.replace(replContent, repl);

			// Turn file content from string to document
			String xmlContent = fileContent;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document docx = builder.parse(new InputSource(new StringReader(xmlContent)));

			NodeList bean = docx.getElementsByTagName("property");

			for(int i = 0; i < bean.getLength(); i++) {
				Element beanElement = (Element) bean.item(i);
				if (beanElement.getAttribute("name").equalsIgnoreCase("baseDn")) {
					if (beanElement.getAttribute("value").equals("")) {
						propertyBase = beanElement.getTextContent();
					} else {
						propertyBase = beanElement.getAttribute("value");
					}
				}
				if (beanElement.getAttribute("name").equalsIgnoreCase("userSearchBase")) {
					if (beanElement.getAttribute("value").equals("")) {
						propertySearchBase = beanElement.getTextContent();
					} else {
						propertySearchBase = beanElement.getAttribute("value");
					}
				}
			}

			Hashtable env = new Hashtable();
			String cxtFactory = "com.sun.jndi.ldap.LdapCtxFactory";
			env.put(Context.INITIAL_CONTEXT_FACTORY, cxtFactory);
			env.put(Context.PROVIDER_URL, ldapURL);
			DirContext dirCxt = new InitialDirContext(env);

			String base = propertySearchBase + "," + propertyBase;

			SearchControls ctrl = new SearchControls();
			ctrl.setReturningAttributes(attributeFilter);
			ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);

			String filter = "(&(uid=" + user + "))";
			NamingEnumeration results = dirCxt.search(base, filter, ctrl);
			while (results.hasMore()) {
				SearchResult result = (SearchResult) results.next();
				Attributes attrs = result.getAttributes();
				// First name attribute - givenName
				Attribute attr = attrs.get("givenName");
				identityAttributes[0] = attr.get().toString();
				// Last name attribute - sn
				attr = attrs.get("sn");
				identityAttributes[1] = attr.get().toString();
				// Email attribute - mail
				attr = attrs.get("mail");
				identityAttributes[2] = attr.get().toString();
			}
			dirCxt.close();
		} catch (Exception e) {
			print("+----------------------------------------------------------------------------------+");
			print("CWS Installer ERROR: LDAP API failed to retrieve CWS user's " + Arrays.toString(attributeFilter));
			print("     to set in CWS properties files and utilize for CWS services. Make sure 'ldap_plugin_bean.xml' is ");
			print("     properly configured. Refer to the template /tomcat_conf/ldap_plugin_bean.xml");
			print("ERROR: " + e);
			print("+----------------------------------------------------------------------------------+");
			// JNDI LDAP retrieval failed.
			identityAttributes[0] = "__CWS_ADMIN_FIRSTNAME__";
			identityAttributes[1] = "__CWS_ADMIN_LASTNAME__";
			identityAttributes[2] = "__CWS_ADMIN_EMAIL__";
		}
		return identityAttributes;
	}


	private static String updateIdentityPluginContent(String content) throws IOException {
		//
		// Update identity plugin content
		//
		if (cws_auth_scheme.equals("LDAP")) {
			// Erase the __CUSTOM_IDENTITY_PLUGIN_XML__token
			content = content.replace("__CUSTOM_IDENTITY_PLUGIN_XML__", "");

			// Fill in the __LDAP_PLUGIN_BEAN__
			String ldapBeanContent = getFileContents(
					Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_bean.xml"));
			content = content.replace("__LDAP_PLUGIN_BEAN__", ldapBeanContent);

			String ldapRefContent = getFileContents(
					Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_ref.xml"));
			content = content.replace("__LDAP_PLUGIN_REF__", ldapRefContent);

			content = content.replace("__CWS_IDENTITY_PLUGIN_CLASS__", cws_identity_plugin_class);
			content = content.replace("__CWS_LDAP_URL__",              cws_ldap_url);
		}
		else if (cws_auth_scheme.equals("CAMUNDA")) {
			// Erase the unneeded tokens
			content = content.replace("__CUSTOM_IDENTITY_PLUGIN_XML__", "<!-- CUSTOM_IDENTITY_PLUGIN_XML -->"); // erase token
			content = content.replace("__LDAP_PLUGIN_BEAN__",    "<!-- LDAP_PLUGIN_BEAN -->"); // erase token
			content = content.replace("__LDAP_PLUGIN_REF__",     "<!-- LDAP_PLUGIN_REF -->"); // erase token
		}
		else {  // cws_auth_scheme.equals("CUSTOM")
			// Erase the __LDAP_PLUGIN_*__ tokens
			content = content.replace("__LDAP_PLUGIN_BEAN__", "<!-- LDAP_PLUGIN_BEAN -->"); // erase token
			content = content.replace("__LDAP_PLUGIN_REF__",  "<!-- LDAP_PLUGIN_REF -->"); // erase token
			// Fill in the __CUSTOM_IDENTITY_PLUGIN_XML__
			//There is no custom_identity_plugin.xml file now.
			Path replaceFilePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "custom_identity_plugin.xml");
			String replaceContent = getFileContents(replaceFilePath);
			content = content.replace("__CUSTOM_IDENTITY_PLUGIN_XML__", replaceContent);
		}

		content = content.replace("__CWS_LDAP_USER__",             cws_user);

		return content;
	}


	private static void deleteCwsUiWebApp() {
		print(" Removing cws-ui app from webapps...");

		// do not remove those directory and zip file, in case may to reconfigure this cws
		File cwsUiWebappDir = new File(cws_tomcat_webapps + SEP + "cws-ui");

		if (deleteDirectory(cwsUiWebappDir)) {
			log.info("Cleaned up the cws-ui app from webapps.");
		}
		else {
			log.error(" Can not remove the cws-ui app from webapps. " );
			exit(1);
		}
	}


	private static void installLogstash() throws IOException {
		// UNZIP / INSTALL / SETUP  LOGSTASH
		String logstashZipFilePath = cws_server_root + SEP + "logstash-" + logstash_ver + ".zip";
		String logstashDestDirectory = cws_server_root;

		// Unzip the LogStash archive
		unzipFile(logstashZipFilePath, logstashDestDirectory);

		// Open up permissions of logstash executables
		openUpPermissions(logstashDestDirectory + SEP + "logstash-" + logstash_ver + SEP + "bin" + SEP + "logstash");

		// CONFIGURE LOGSTASH CONF TO READ FROM CATALINA.OUT AND CWS.LOG
		// UPDATE cws-logstash.conf
		print(" Updating cws-logstash.conf...");
		Path logstashFilePath = Paths.get(config_work_dir + SEP + "logging" + SEP + "cws-logstash.conf");
		String logstashContent = getFileContents(logstashFilePath);

		// LogStash doesn't like backslashes (i.e. if this is Windows) in the input file path,
		// so replace any backslashes with forward slashes.
		// See:  https://logstash.jira.com/browse/LOGSTASH-1126
		String catalinaLogPath = cws_tomcat_root + SEP + "logs";
		catalinaLogPath = catalinaLogPath.replace("\\", "/");
		logstashContent = logstashContent.replace("__CWS_CATALINA_OUT_PATH__", catalinaLogPath);

		logstashContent = logstashContent.replace("__CWS_ES_PROTOCOL__", elasticsearch_protocol);
		logstashContent = logstashContent.replace("__CWS_ES_HOST__", elasticsearch_host);
		logstashContent = logstashContent.replace("__CWS_ES_PORT__", elasticsearch_port);
		if (elasticsearch_use_auth.equalsIgnoreCase(("Y"))) {
			// Construct the auth config for logstash
			String user = "user => \"" + elasticsearch_username + "\"";
			String pw = "password => \"" + elasticsearch_password + "\"";
			logstashContent = logstashContent.replace("__LOGSTASH_ES_USERNAME__", user);
			logstashContent = logstashContent.replace("__LOGSTASH_ES_PASSWORD__", pw);

			// Tell logstash to use https
			logstashContent = logstashContent.replace("__LOGSTASH_ES_USE_SSL__", "true");
		} else {
			// Remove these blocks if not using auth
			logstashContent = logstashContent.replace("__LOGSTASH_ES_USERNAME__", "");
			logstashContent = logstashContent.replace("__LOGSTASH_ES_PASSWORD__", "");

			// Tell logstash to use http
			logstashContent = logstashContent.replace("__LOGSTASH_ES_USE_SSL__", "false");
		}
		writeToFile(logstashFilePath, logstashContent);

		print(" Put logstash conf file into place.");
		copy(
			Paths.get(config_work_dir + SEP + "logging" + SEP + "cws-logstash.conf"),
			Paths.get(logstash_root   + SEP + "cws-logstash.conf"));
	}

	private static void writeOutConfigurationFile() throws IOException {
		InstallerPresets presets = CwsInstallerUtils.getInstallerPresets();

		setPreset("hostname", this_hostname);
		setPreset("install_type", installType);
		setPreset("worker_type", cws_worker_type);
		setPreset("database_type", cws_db_type);
		setPreset("database_host", cws_db_host);
		setPreset("database_port", cws_db_port);
		setPreset("database_name", cws_db_name);
		setPreset("database_username", cws_db_username);
		setPreset("database_password", cws_db_password);
		setPreset("admin_user", cws_user);

		if (cws_auth_scheme.equalsIgnoreCase("CAMUNDA")) {
			setPreset("admin_firstname", cws_user_firstname);
			setPreset("admin_lastname", cws_user_lastname);
			setPreset("admin_email", cws_user_email);
		} else {
			Path pluginBeanFilePath = Paths.get(config_work_dir + SEP + "tomcat_conf" + SEP + "ldap_plugin_bean.xml");
			String[] identityAttr = getIdentityPluginAttribute(pluginBeanFilePath, cws_user, cws_ldap_url);
			setPreset("admin_firstname", identityAttr[0]);
			setPreset("admin_lastname", identityAttr[1]);
			setPreset("admin_email", identityAttr[2]);
		}

		setPreset("cws_web_port", cws_tomcat_connector_port);
		setPreset("cws_ssl_port", cws_tomcat_ssl_port);
		setPreset("cws_ajp_port", cws_tomcat_ajp_port);
		setPreset("cws_shutdown_port", cws_shutdown_port);
		setPreset("cws_console_host", cws_console_host);
		setPreset("cws_console_ssl_port", cws_console_ssl_port);
		setPreset("smtp_hostname", cws_smtp_hostname);
		setPreset("smtp_port", cws_smtp_port);
		setPreset("amq_host", cws_amq_host);
		setPreset("amq_port", cws_amq_port);
		setPreset("cws_amq_jmx_port", cws_amq_jmx_port);
		setPreset("cws_jmx_port", cws_jmx_port);
		setPreset("identity_plugin_type", cws_auth_scheme);
		setPreset("cws_ldap_url", cws_ldap_url);

		setPreset("notify_users_email", cws_send_user_task_assign_emails);
		setPreset("email_subject", cws_task_assignment_sub);
		setPreset("email_body", cws_task_assignment_body);
		setPreset("brand_header", cws_brand_header);
		setPreset("project_webapp_root", cws_project_webapp_root == null? "" : cws_project_webapp_root);
		setPreset("cws_enable_cloud_autoscaling", cws_enable_cloud_autoscaling);
		setPreset("aws_cloudwatch_endpoint", aws_cloudwatch_endpoint);
		setPreset("metrics_publishing_interval", metrics_publishing_interval);
		setPreset("cws_notification_emails", cws_notification_emails);
		setPreset("cws_token_expiration_hours", cws_token_expiration_hours);
		setPreset("elasticsearch_protocol", elasticsearch_protocol);
		setPreset("elasticsearch_host", elasticsearch_host);
		setPreset("elasticsearch_port", elasticsearch_port);
		setPreset("elasticsearch_use_auth", elasticsearch_use_auth);
		setPreset("elasticsearch_username", elasticsearch_username);
		setPreset("elasticsearch_password", elasticsearch_password);
		setPreset("user_provided_logstash", user_provided_logstash);
		setPreset("history_level", history_level);
		setPreset("history_days_to_live", history_days_to_live);
		setPreset("max_num_procs_per_worker", max_num_procs_per_worker);
		setPreset("worker_abandoned_days", worker_abandoned_days);
		setPreset("aws_default_region", aws_default_region);
		setPreset("aws_sqs_dispatcher_sqsUrl", aws_sqs_dispatcher_sqsUrl);
		setPreset("aws_sqs_dispatcher_msgFetchLimit", aws_sqs_dispatcher_msgFetchLimit);

		presets.writeOutToFile();
	}

	private static void setPreset(String key, String value) {
		if (value != null) {
			CwsInstallerUtils.getInstallerPresets().setProperty(key, value);
		}
	}

}