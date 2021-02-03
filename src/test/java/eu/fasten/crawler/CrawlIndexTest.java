package eu.fasten.crawler;

import eu.fasten.crawler.output.KafkaOutput;
import eu.fasten.crawler.output.StdOutput;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

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

    @Test
    public void testIndexSetupFullRun() {
        File f = DownloadIndex.download(600);
        CrawlIndex index = new CrawlIndex(600, f);
        StdOutput mockStd = mock(StdOutput.class);

        index.crawlAndSend(mockStd, 50);

        verify(mockStd, atLeastOnce()).send(anyList());
        f.delete();
    }

    @Test
    public void testIndexSetupFullRunKafka() throws IllegalAccessException {
        File f = DownloadIndex.download(600);
        CrawlIndex index = new CrawlIndex(600, f);
        KafkaOutput kafkaOutput = spy(new KafkaOutput("", "", 50));

        KafkaProducer<String, String> prod = mock(KafkaProducer.class);
        Future<RecordMetadata> fut = mock(Future.class);

        doNothing().when(kafkaOutput).open();
        doReturn(fut).when(prod).send(any());

        FieldUtils.writeField(kafkaOutput, "producer", prod, true);

        index.crawlAndSend(kafkaOutput, 50);

        verify(kafkaOutput, atLeastOnce()).send(anyList());
        f.delete();
    }
}
