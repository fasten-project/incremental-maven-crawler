package eu.fasten.crawler;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadIndexTest {

    @Test
    public void indexExistsTest() {
        assertTrue(DownloadIndex.indexExists(600)); // We know this index exists.
        assertFalse(DownloadIndex.indexExists(-1)); // We know this index does not exist.
    }

    @Test
    public void indexDownloadTest() {
        File file = DownloadIndex.download(600);
        assertTrue(file.exists());

        //delete it again
        file.delete();
    }
}
