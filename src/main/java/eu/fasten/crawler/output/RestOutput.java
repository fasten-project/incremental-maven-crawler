package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class RestOutput implements Output {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String endpoint;

    public RestOutput(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean send(List<MavenArtifact> artifact) {
        HttpClient httpcClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(endpoint);

        try {
            int responseCode = httpcClient.execute(httpPost).getStatusLine().getStatusCode();

            if (responseCode == 200) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed sending to rest endpoint. ", e);
            return false;
        }
    }
}
