package eu.fasten.crawler;

import eu.fasten.crawler.output.Output;
import eu.fasten.crawler.output.StdOutput;
import org.apache.commons.cli.*;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Int;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IncrementalMavenCrawler implements Runnable {

    // Arguments for incremental maven crawler.
    static Options options = new Options();
    static Option optStartIndex = Option.builder("i")
            .longOpt("start_index")
            .hasArg()
            .argName("index")
            .required()
            .desc("Index to start crawling from (inclusive).")
            .type(Integer.class)
            .build();

    static Option optBatchSize = Option.builder("bs")
            .longOpt("batch_size")
            .hasArg()
            .argName("amount")
            .desc("Size of batches to send to output. Defaults to 50.")
            .type(Integer.class)
            .build();

    static Option optOutputType = Option.builder("o")
            .longOpt("output")
            .hasArg()
            .required()
            .argName("[std|kafka|rest]")
            .desc("Output to send the crawled artifacts to.")
            .type(String.class)
            .build();

    static Option optCrawlInterval = Option.builder("i")
            .longOpt("interval")
            .hasArg()
            .required()
            .argName("hours")
            .desc("Time to wait between crawl attempts (in hours).")
            .type(Integer.class)
            .build();

    static Option optCheckpointDir = Option.builder("cd")
            .longOpt("checkpoint_dir")
            .hasArg()
            .argName("hours")
            .desc("Directory to checkpoint/store latest crawled index. Used for recovery on crash or restart.")
            .type(Integer.class)
            .build();

    public static void main(String[] args) {
        options.addOption(optStartIndex);
        options.addOption(optBatchSize);
        options.addOption(optOutputType);
        options.addOption(optCrawlInterval);
        options.addOption(optCheckpointDir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "IncrementalMavenCrawler", options );
        try {
            CommandLine cmd = parser.parse(options, args);
            verifyAndParseArguments(cmd);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }

        new IncrementalMavenCrawler(679, 256, new StdOutput(), "").run();
//        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//        service.scheduleAtFixedRate(new IncrementalMavenCrawler(679), 0, 1, TimeUnit.MINUTES);
    }

    public static Properties verifyAndParseArguments(CommandLine cmd) throws ParseException {
        Properties props = new Properties();

        props.put(cmd.getOptionValue("i"))
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private int index;
    private String checkpointDir;
    private int batchSize;
    private Output output;

    public IncrementalMavenCrawler(int startIndex, int batchSize, Output output, String checkpointDir) {
        this.batchSize = batchSize;
        this.output = output;
        this.checkpointDir = checkpointDir;
        // Get index to start from
        this.index = initIndex(startIndex);
    }

    public int initIndex(int startIndex) {
        if (this.checkpointDir == null) {
            return startIndex;
        }

        // Get files in checkpoint directory.
        File file = new File(checkpointDir);

        // Find highest index in checkpoint directory.
        int highestStoredIndex = Integer.MIN_VALUE;
        for (String f : file.list()) {
            if (f.endsWith(".index")) {
                int fIndex = Integer.parseInt(f.replace(".index", ""));
                highestStoredIndex = Math.max(fIndex, highestStoredIndex);
            }
        }

        // Return the highest index.
        return Math.max(startIndex, highestStoredIndex);
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
        crawlIndex.crawlAndSendUnique(output, batchSize);

        // Delete the index file.
        indexFile.delete();

        logger.info("Index " + index + " successfully crawled.");

        // Update (and increment) the index.
        updateIndex();
    }

    public void updateIndex() {
        this.index += 1;

        // If checkpointing is disabled, return.
        if (checkpointDir == null) {
            return;
        }

        try {
            // Clean directory.
            FileUtils.cleanDirectory(checkpointDir);

            // Create checkpoint file.
            File checkpointFile = new File(checkpointDir + this.index + ".index");
            checkpointFile.mkdirs();
            checkpointFile.createNewFile();
        } catch (IOException e) {
            logger.error("Failed checkpointing index " + index + ".", e);
        }
    }

}
