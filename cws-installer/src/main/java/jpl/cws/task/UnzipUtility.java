package jpl.cws.task;

import static jpl.cws.task.CwsInstallerUtils.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;


/**
 * This utility extracts files and directories of a standard zip file to
 * a destination directory.
 * 
 * Examples taken from:
 * http://www.javased.com/?api=org.apache.commons.compress.archivers.zip.ZipArchiveEntry
 *
 */
public class UnzipUtility {

	public static void unzipFile(String archivePath, String targetPath) throws IOException {
		print(" Unzipping " + archivePath + " to " + targetPath);
		File archiveFile = new File(archivePath);
		File targetFile = new File(targetPath);
		ZipFile zipFile = new ZipFile(archiveFile);
		Enumeration<?> e = zipFile.getEntries();
		while (e.hasMoreElements()) {
			ZipArchiveEntry zipEntry = (ZipArchiveEntry)e.nextElement();
			File file = new File(targetFile, zipEntry.getName());
			if (zipEntry.isDirectory()) {
				FileUtils.forceMkdir(file);
			}
			else {
				InputStream is = zipFile.getInputStream(zipEntry);
				FileOutputStream os = FileUtils.openOutputStream(file);
				try {
					IOUtils.copy(is, os);
				}
				finally {
					os.close();
					is.close();
				}
			}
		}
		zipFile.close();
		print(" Unzip of " + archivePath + " to " + targetPath + " complete.");
	}

}
