package eu.fasten.crawler;

import eu.fasten.crawler.output.KafkaOutput;
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
            .argName("[std|kafka|rest]")
            .desc("Output to send the crawled artifacts to.")
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
            .desc("Directory to checkpoint/store latest crawled index. Used for recovery on crash or restart.")
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
            .desc("Kafka brokers to connect with. I.e. broker1:port,broker2:port,...")
            .build();

    public static void main(String[] args) {
        options.addOption(optStartIndex);
        options.addOption(optBatchSize);
        options.addOption(optOutputType);
        options.addOption(optCrawlInterval);
        options.addOption(optCheckpointDir);
        options.addOption(optKafkaTopic);
        options.addOption(optKafkaBrokers);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "IncrementalMavenCrawler", options );

        Properties properties;
        try {
            CommandLine cmd = parser.parse(options, args);
            properties = verifyAndParseArguments(cmd);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Output output = new StdOutput();
        int batchSize = Integer.parseInt(properties.getProperty("batch_size"));
        int startIndex = Integer.parseInt(properties.getProperty("index"));
        String checkpointDir = properties.getProperty("checkpoint_dir");

        // Setup Kafka.
        if (properties.get("output").equals("kafka")) {
            output = new KafkaOutput(properties.getProperty("kafka_topic"), properties.getProperty("kafka_brokers"), batchSize);
        }

        new IncrementalMavenCrawler(startIndex, batchSize, output, checkpointDir).run();
//        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//        service.scheduleAtFixedRate(new IncrementalMavenCrawler(679), 0, 1, TimeUnit.MINUTES);
    }

    /**
     * Verify and stores arguments in properties instance.
     * @param cmd
     * @return
     * @throws ParseException
     */
    public static Properties verifyAndParseArguments(CommandLine cmd) throws ParseException {
        Properties props = new Properties();

        if (cmd.getOptionValue("output").equals("kafka") && !(cmd.hasOption("kafka_topic") || cmd.hasOption("kafka_brokers"))) {
            throw new ParseException("Configured output to be Kafka, but no `kafka_topic` or `kafka_brokers` have been configured.");
        }

        props.setProperty("index", cmd.getOptionValue("start_index", "0"));
        props.setProperty("batch_size", cmd.getOptionValue("batch_size", "50"));
        props.setProperty("output", cmd.getOptionValue("output", "std"));
        props.setProperty("interval", cmd.getOptionValue("interval", "1"));
        props.setProperty("checkpoint_dir", cmd.getOptionValue("checkpoint_dir", ""));
        props.setProperty("kafka_topic", cmd.getOptionValue("kafka_topic", ""));
        props.setProperty("kafka_brokers", cmd.getOptionValue("kafka_brokers", ""));

        return props;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private int index;
    private String checkpointDir;
    private int batchSize;
    private Output output;

    public IncrementalMavenCrawler(int startIndex, int batchSize, Output output, String checkpointDir) {
        this.batchSize = batchSize;
        this.output = output;
        this.checkpointDir = checkpointDir.equals("") ? null : checkpointDir;
        this.index = initIndex(startIndex);

        if (this.index > startIndex) {
            logger.info("Found (checkpointed) index in " + checkpointDir + ". Will start crawling from index " + this.index);
        }
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
        crawlIndex.crawlAndSend(output, batchSize);

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
