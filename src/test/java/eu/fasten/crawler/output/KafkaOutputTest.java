package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Test;
import org.mockito.Spy;

import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

public class KafkaOutputTest {

    @Test
    public void testKafkaOutputSuccessfulSend() throws IllegalAccessException {
        MavenArtifact artifactOne = new MavenArtifact("a", "g", "1", 0L);
        MavenArtifact artifactTwo = new MavenArtifact("a", "g", "2", 0L);

        KafkaOutput kafkaOutput = spy(new KafkaOutput("", "", 50));

        KafkaProducer<String, String> prod = mock(KafkaProducer.class);
        Future<RecordMetadata> fut = mock(Future.class);

        doNothing().when(kafkaOutput).open();
        doReturn(fut).when(prod).send(any());

        FieldUtils.writeField(kafkaOutput, "producer", prod, true);

        boolean res = kafkaOutput.send(List.of(artifactOne, artifactTwo));
        assertTrue(res);
    }

    @Test
    public void testKafkaOutputFailedSend() throws Exception {
        MavenArtifact artifactOne = new MavenArtifact("a", "g", "1", 0L);
        MavenArtifact artifactTwo = new MavenArtifact("a", "g", "2", 0L);

        KafkaOutput kafkaOutput = spy(new KafkaOutput("", "", 50));

        KafkaProducer<String, String> prod = mock(KafkaProducer.class);
        Future<RecordMetadata> fut = mock(Future.class);
        Future<RecordMetadata> futTwo = mock(Future.class);

        doNothing().when(kafkaOutput).open();
        doReturn(fut).when(prod).send(new ProducerRecord<String, String>("", null, artifactOne.getTimestamp(), null, artifactOne.toString()));
        doReturn(futTwo).when(prod).send(new ProducerRecord<String, String>("", null, artifactTwo.getTimestamp(), null, artifactTwo.toString()));

        when(futTwo.get()).thenThrow(InterruptedException.class);

        FieldUtils.writeField(kafkaOutput, "producer", prod, true);

        boolean res = kafkaOutput.send(List.of(artifactOne, artifactTwo));
        assertFalse(res);
    }
}
