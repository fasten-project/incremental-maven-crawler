package eu.fasten.crawler.output;

import java.util.Properties;

public class OutputFactory {

    public static Output getOutput(String outputName, Properties properties) {
        switch (outputName) {
            case "kafka":
                return new KafkaOutput(properties.getProperty("kafka_topic"), properties.getProperty("kafka_brokers"), Integer.parseInt(properties.getProperty("batch_size")));
            case "rest":
                return new RestOutput(properties.getProperty("rest_endpoint"));
            default:
                return new StdOutput();
        }
    }
}
