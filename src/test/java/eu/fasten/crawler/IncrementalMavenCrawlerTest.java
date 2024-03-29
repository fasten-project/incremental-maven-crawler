package eu.fasten.crawler;

import eu.fasten.crawler.output.StdOutput;
import org.apache.commons.cli.Option;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

public class IncrementalMavenCrawlerTest {

    @BeforeClass
    public static void beforeAll() {
        new File("src/test/resources").mkdirs();
    }

    @Test
    public void testCheckpointDisabled() {
        int index = 0;
        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "");
        assertEquals(index, crawler.getIndex());
    }

    @Test
    public void testCheckpointEnabledNoOverride() throws IOException {
        int index = 1;

        File file = new File("src/test/resources/0.index");
        file.mkdirs();
        file.createNewFile();

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "src/test/resources/");
        assertEquals(index, crawler.getIndex());
        file.delete();
    }

    @Test
    public void testCheckpointEnabledOverride() throws IOException {
        int index = 0;

        File file = new File("src/test/resources/1.index");
        file.mkdirs();
        file.createNewFile();

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "src/test/resources/");
        assertNotEquals(index, crawler.getIndex());
        assertEquals(1, crawler.getIndex());
        file.delete();
    }

    @Test
    public void testCheckpointEnabledMultipleOverride() throws IOException {
        int index = 0;

        File file = new File("src/test/resources/1.index");
        file.mkdirs();
        file.createNewFile();

        File file2 = new File("src/test/resources/5.index");
        file2.mkdirs();
        file2.createNewFile();

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "src/test/resources/");
        assertNotEquals(index, crawler.getIndex());
        assertEquals(5, crawler.getIndex());

        file.delete();
        file2.delete();
    }

    @Test
    public void testUpdateIndexNoCheckpoint() throws IOException {
        int index = 0;

        File file = new File("src/test/resources/1.index");

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "");
        crawler.updateIndex();

        assertFalse(file.exists());
        assertEquals(1, crawler.getIndex());
    }

    @Test
    public void testUpdateIndexCheckpoint() throws IOException {
        int index = 0;


        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "src/test/resources/");
        crawler.updateIndex();

        File file = new File("src/test/resources/1.index");
        assertTrue(file.exists());
        assertEquals(1, crawler.getIndex());

        file.delete();
    }

    @Test
    public void testUpdateIndexCheckpointEnabledMultipleOverride() throws IOException {
        int index = 0;

        File file = new File("src/test/resources/1.index");
        file.mkdirs();
        file.createNewFile();

        File file2 = new File("src/test/resources/5.index");
        file2.mkdirs();
        file2.createNewFile();

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "src/test/resources/");
        crawler.updateIndex();
        assertFalse(file.exists());
        assertFalse(file2.exists());
        assertEquals(6, crawler.getIndex());
        assertTrue(new File("src/test/resources/6.index").exists());

        new File("src/test/resources/6.index").delete();
    }

    @Test
    public void testNonExistentIndex() {
        int index = 9999999;
        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 0, new StdOutput(), "src/test/resources/");
        crawler.run();
        assertEquals(index, crawler.getIndex());
    }

    @Test
    public void testSuccessfulCrawl() {
        int index = 680;
        StdOutput stdOutput = spy(new StdOutput());

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 50, stdOutput, "src/test/resources/");

        when(stdOutput.send(anyList())).thenReturn(true);
        crawler.run();

        assertTrue(new File("src/test/resources/" + (index + 1) + ".index").exists());
        verify(stdOutput, atLeastOnce()).send(anyList());
        assertEquals(index + 1, crawler.getIndex());

        new File("src/test/resources/" + (index + 1) + ".index").delete();
    }

    @Test
    public void testFailedCrawl() {
        int index = 680;
        StdOutput stdOutput = spy(new StdOutput());

        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(index, 50, stdOutput, "src/test/resources/");

        when(stdOutput.send(anyList())).thenReturn(false);
        crawler.run();

        verify(stdOutput, atLeastOnce()).send(anyList());
        assertEquals(index, crawler.getIndex());
    }

    /**
     * This test aims to detect multiple options with same name.
     * It compares number of Option static attribute declared in IncrementalMavenCrawler with
     * number of elements in the list provided by Options.getOptions().
     */
    @Test
    public void testAddOptions() {
        // Count number of Option static attribute declared in IncrementalMavenCrawler
        long numberOfOptionsDelcared = Arrays.stream(IncrementalMavenCrawler.class.getDeclaredFields()).filter(field -> field.getType() == Option.class).count();

        IncrementalMavenCrawler.addOptions();

        // Number of options registered in options attribute (Apache CLI Options class)
        long actualNumberOfOptions = IncrementalMavenCrawler.options.getOptions().size();

        assertEquals(numberOfOptionsDelcared, actualNumberOfOptions);
    }
}
