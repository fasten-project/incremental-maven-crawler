package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KafkaOutput implements Output {

    // Kafka connection data.
    private final String topic;
    private KafkaProducer<String, String> producer;
    private Properties properties = new Properties();

    /**
     * Outputs to a Kafka topic.
     *
     * @param topic the topic to send to.
     * @param brokers the brokers to connect with.
     * @param batchSize the size of the batches to send.
     */
    public KafkaOutput(String topic, String brokers, int batchSize) {
        this.topic = topic;

        properties.put("client.id", "IncrementalMavenCrawler");
        properties.put("bootstrap.servers", brokers);
        properties.put("batch.size", String.valueOf(batchSize));
        properties.put("linger.ms", "1000");
        properties.put("acks", "all"); // await all acknowledgements
    }

    @Override
    public void open() {
        producer = new KafkaProducer<String, String>(properties);
    }

    @Override
    public void close() {
        producer.close();
    }

    @Override
    public void flush() {
        producer.flush();
    }

    /**
     * Sends a batch of artifacts to Kafka.
     * @param artifact the artifacts (we expect it to be of size batch size).
     */
    @Override
    public boolean send(List<MavenArtifact> artifact) {
        List<ProducerRecord<String, String>> records = artifact
                .stream()
                .map((x) -> new ProducerRecord<String, String>(topic, null, x.getTimestamp(), null, x.toString()))
                .collect(Collectors.toList());

        boolean result = records.stream().map((r) -> producer.send(r)).parallel().map((f) -> {
            try {
                f.get();
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return false;
            }
        }).noneMatch((x) -> x == false);

        return result;
    }
}
