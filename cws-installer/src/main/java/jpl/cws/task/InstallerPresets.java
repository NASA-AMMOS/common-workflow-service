package jpl.cws.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class InstallerPresets extends Properties {

	private static final long serialVersionUID = 1L;
	private String presetsFilePath;

	public InstallerPresets() {
		super();
		InputStream propStream = null;
		try {
			presetsFilePath = System.getenv("CWS_INSTALLER_PRESET_FILE");
			System.out.println("About to load presets from: " + presetsFilePath);
			propStream = new FileInputStream(new File (presetsFilePath));
			this.load(propStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (propStream != null)
					propStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public InstallerPresets(Properties defaults) {
		super(defaults);
	}
	
	public String getPresetsFilePath() {
		return presetsFilePath;
	}
	
	public void writeOutToFile() {
		System.out.println("Writing configuration properties to: " + presetsFilePath);
		try {
			File file = new File(presetsFilePath);
			file.delete();
			file.createNewFile();
			FileOutputStream fileOut = new FileOutputStream(file);
			// TODO: Replace with URL to config options once wiki is populated
			this.store(fileOut, "CWS Configuration Properties (see: https://github.com/NASA-AMMOS/common-workflow-service/wiki)" + System.currentTimeMillis());
			fileOut.close();
			
			File nextVer = new File(presetsFilePath);
			String preContents = CwsInstallerUtils.getFileContents(nextVer.toPath());
			String nextContents = preContents.replaceAll("\\\\", "");
			CwsInstallerUtils.writeToFile(nextVer.toPath(), nextContents);
		}
		catch (Throwable t) {
			System.out.println("ERROR: Problem writing out properties to : " + presetsFilePath);
		}
	}
}
