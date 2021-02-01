package eu.fasten.crawler;

import eu.fasten.crawler.output.StdOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IncrementalMavenCrawler implements Runnable {

    public static void main(String[] args) {
        new IncrementalMavenCrawler(679).run();
//        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//        service.scheduleAtFixedRate(new IncrementalMavenCrawler(679), 0, 1, TimeUnit.MINUTES);
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private int index;
    public IncrementalMavenCrawler(int startIndex) {
        // Get index to start from
        this.index = startIndex;
    }

    public void run() {
        if (!DownloadIndex.indexExists(index)) {
            logger.info("Attempting to download index " + index + ", but it doesn't exist yet.");
            return;
        }

        logger.info(index + " exists. Now downloading the index file.");

        // Download the index.
        File indexFile = DownloadIndex.download(index);

        logger.info("Index file successfully downloaded. Now crawling and outputting it.");

        // Setup crawler.
        CrawlIndex crawlIndex = new CrawlIndex(index, indexFile);
        crawlIndex.crawlAndSendUnique(new StdOutput(), 256);

        // Delete the index file.
        indexFile.delete();

        logger.info("Index " + index + " successfully crawled.");

        // Update (and increment) the index.
        updateIndex();
    }

    public void updateIndex() {
        index += 1;
    }

}
