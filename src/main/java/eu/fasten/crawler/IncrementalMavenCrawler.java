package eu.fasten.crawler;

import eu.fasten.crawler.output.KafkaOutput;
import eu.fasten.crawler.output.Output;
import eu.fasten.crawler.output.RestOutput;
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
            .desc("Index to start crawling from (inclusive). Required.")
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
            .argName("[std|kafka|rest]")
            .desc("Output to send the crawled artifacts to. Defaults to std.")
            .type(String.class)
            .build();

    static Option optCrawlInterval = Option.builder("i")
            .longOpt("interval")
            .hasArg()
            .required()
            .argName("hours")
            .desc("Time to wait between crawl attempts (in hours). Defaults to 1 hour.")
            .type(Integer.class)
            .build();

    static Option optCheckpointDir = Option.builder("cd")
            .longOpt("checkpoint_dir")
            .hasArg()
            .argName("hours")
            .desc("Directory to checkpoint/store latest crawled index. Used for recovery on crash or restart. Optional.")
            .type(Integer.class)
            .build();

    static Option optKafkaTopic = Option.builder("kt")
            .longOpt("kafka_topic")
            .hasArg()
            .argName("topic")
            .desc("Kafka topic to produce to.")
            .build();

    static Option optKafkaBrokers = Option.builder("kb")
            .longOpt("kafka_brokers")
            .hasArg()
            .argName("brokers")
            .desc("Kafka brokers to connect with. I.e. broker1:port,broker2:port,... Optional.")
            .build();

    static Option optRestEndpoint = Option.builder("re")
            .longOpt("rest_endpoint")
            .hasArg()
            .argName("url")
            .desc("HTTP endpoint to post crawled batches to.")
            .build();

    public static void main(String[] args) {
        addOptions();
        CommandLineParser parser = new DefaultParser();

        Properties properties;
        try {
            CommandLine cmd = parser.parse(options, args);
            properties = verifyAndParseArguments(cmd);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // Setup arguments for crawler.
        Output output = new StdOutput();
        int batchSize = Integer.parseInt(properties.getProperty("batch_size"));
        int startIndex = Integer.parseInt(properties.getProperty("index"));
        int interval = Integer.parseInt(properties.getProperty("interval"));
        String checkpointDir = properties.getProperty("checkpoint_dir");

        // Setup Kafka.
        if (properties.get("output").equals("kafka")) {
            output = new KafkaOutput(properties.getProperty("kafka_topic"), properties.getProperty("kafka_brokers"), batchSize);
        } else if (properties.get("output").equals("rest")) {
            output = new RestOutput(properties.getProperty("rest_endpoint"));
        }

        // Start cralwer and execute it with an interval.
        IncrementalMavenCrawler crawler = new IncrementalMavenCrawler(startIndex, batchSize, output, checkpointDir);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(crawler, 0, interval, TimeUnit.HOURS);
    }

    public static void addOptions() {
        options.addOption(optStartIndex);
        options.addOption(optBatchSize);
        options.addOption(optOutputType);
        options.addOption(optCrawlInterval);
        options.addOption(optCheckpointDir);
        options.addOption(optKafkaTopic);
        options.addOption(optKafkaBrokers);
        options.addOption(optRestEndpoint);
    }

    /**
     * Verify and stores arguments in properties instance.
     * @param cmd the parsed command line arguments.
     * @return verified arguments.
     * @throws ParseException when something goes wrong.
     */
    public static Properties verifyAndParseArguments(CommandLine cmd) throws ParseException {
        Properties props = new Properties();

        if (cmd.getOptionValue("output").equals("kafka") && !(cmd.hasOption("kafka_topic") || cmd.hasOption("kafka_brokers"))) {
            throw new ParseException("Configured output to be Kafka, but no `kafka_topic` or `kafka_brokers` have been configured.");
        }

        if (cmd.getOptionValue("output").equals("rest") && !(cmd.hasOption("rest_endpoint"))) {
            throw new ParseException("Configured output to be Rest, but no `rest_endpoint` has been configured.");
        }

        props.setProperty("index", cmd.getOptionValue("start_index", "0"));
        props.setProperty("batch_size", cmd.getOptionValue("batch_size", "50"));
        props.setProperty("output", cmd.getOptionValue("output", "std"));
        props.setProperty("interval", cmd.getOptionValue("interval", "1"));
        props.setProperty("checkpoint_dir", cmd.getOptionValue("checkpoint_dir", ""));
        props.setProperty("kafka_topic", cmd.getOptionValue("kafka_topic", ""));
        props.setProperty("kafka_brokers", cmd.getOptionValue("kafka_brokers", ""));
        props.setProperty("rest_endpoint", cmd.getOptionValue("rest_endpoint", ""));

        return props;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // Crawler related settings.
    private int index;
    private String checkpointDir;
    private int batchSize;
    private Output output;

    /**
     * Crawls the incremental index from Maven (incrementally).
     * Checkpointing is used to persist up until which index we crawled (the stored index is _not_ inclusive).
     *
     * @param startIndex the index to start crawling from. If a higher index is found in the checkpoint dir, we use that as startIndex.
     * @param batchSize the size of the batches to send to the output.
     * @param output the output to send the (unique) crawled artifacts to. Currently we support Std and Kafka.
     * @param checkpointDir the checkpoint directory to persist indexes. Make sure this directory is persistent across restarts.
     */
    public IncrementalMavenCrawler(int startIndex, int batchSize, Output output, String checkpointDir) {
        this.batchSize = batchSize;
        this.output = output;
        this.checkpointDir = checkpointDir.equals("") ? null : checkpointDir;
        this.index = initIndex(startIndex);

        if (this.index > startIndex) {
            logger.info("Found (checkpointed) index in " + checkpointDir + ". Will start crawling from index " + this.index);
        }

        logger.info("Starting IncrementalMavenCrawler with index: " + this.index + ", batch size: " + batchSize + " and output " + output.getClass().getSimpleName() + ".");
    }

    /**
     * Initialize the index by checking the checkpoint directory.
     * The highest checkpoint is picked.
     *
     * @param startIndex the index to start from.
     * @return the actual startIndex.
     */
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

    /**
     * This method is called every x hours and crawls the (new) index if it exists.
     */
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
        boolean success = crawlIndex.crawlAndSend(output, batchSize);

        // Delete the index file.
        indexFile.delete();

        if (success) {
            logger.info("Index " + index + " successfully crawled.");
            // Update (and increment) the index.
            updateIndex();
        } else {
            logger.warn("Failed crawling index " + index + ". Will retry on next interval.");
        }
    }

    /**
     * Updates the index by incrementing it.
     * Also stores the (new) index in the checkpoint directory (if it is enabled).
     */
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
            checkpointFile.getParentFile().mkdirs();
            checkpointFile.createNewFile();
        } catch (IOException e) {
            logger.error("Failed checkpointing index " + this.index + ".", e);
        }
    }

    /**
     * Returns the index.
     * @return the current index.
     */
    public int getIndex() {
        return this.index;
    }

}
