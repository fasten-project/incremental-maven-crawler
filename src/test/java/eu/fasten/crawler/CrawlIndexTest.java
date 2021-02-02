package eu.fasten.crawler;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertThrows;

public class CrawlIndexTest {

    @Test
    public void testIndexSetup() {
        File f = DownloadIndex.download(600);
        new CrawlIndex(1, f);

        f.delete();
    }

    @Test
    public void testIndexSetupFail() {
        assertThrows(IllegalArgumentException.class, () -> new CrawlIndex(1, new File("non existent")));
    }
}
