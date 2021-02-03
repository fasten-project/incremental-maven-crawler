package eu.fasten.crawler;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadIndex {

    private static Logger logger = LoggerFactory.getLogger(DownloadIndex.class);
    private static final String BASE_URL = "https://repo1.maven.org/maven2/.index/nexus-maven-repository-index.";

    /**
     * Verify if an (incremental) index exists on Maven Central.
     * @param index index to check.
     * @return whether an index is on Maven central (or not).
     */
    public static boolean indexExists(int index) {
        try {
            URL url = new URL(BASE_URL + index + ".gz");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();

            int responseCode = huc.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            logger.error("IOException while checking if index " + index + " exists on Maven Central.", e);
        }

        return false;
    }

    /**
     * Downloads a Maven index in the `/tmp` folder.
     * @param index the index to download.
     * @return the downloaded file.
     */
    public static File download(int index) {
        try {
            File tempFile = File.createTempFile("nexus-maven-repository-index.", index + ".gz");
            FileUtils.copyURLToFile(new URL(BASE_URL + index + ".gz"), tempFile);

            return tempFile;
        } catch (IOException e) {
            logger.error("IOException while downloading index " + index, e);
            throw new RuntimeException("Couldn't download index. Now exiting.");
        }
    }
}
