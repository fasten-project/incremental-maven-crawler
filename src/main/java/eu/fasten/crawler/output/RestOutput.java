package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

public class RestOutput implements Output {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String endpoint;

    /**
     * Setup RestOutput.
     * @param endpoint the http url to POST to.
     */
    public RestOutput(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean send(List<MavenArtifact> artifacts) {
        // Setup connections.
        HttpClient httpClient = constructHttpClient();
        HttpPost httpPost = constructPostRequest();

        try {
            // Send batch.
            StringEntity jsonList = new StringEntity(buildJsonList(artifacts.stream().map((x) -> x.toString()).collect(Collectors.toList())));
            httpPost.setEntity(jsonList);
            int responseCode = httpClient.execute(httpPost).getStatusLine().getStatusCode();

            // If we don't get a 200, return false.
            if (responseCode == 200) {
                return true;
            } else {
                logger.error("Expected response 200, but got " + responseCode);
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed sending to rest endpoint. ", e);
            return false;
        }
    }

    /**
     * Builds a json list of all artifacts.
     * @param artifactStrings all artifacts.
     * @return a stringified list of all artifacts.
     */
    public String buildJsonList(List<String> artifactStrings) {
        // Build array of JSON objects.
        StringBuffer json = new StringBuffer();
        json.append("[");

        for (String af : artifactStrings) {
            json.append(af + ",");
        }

        json.deleteCharAt(json.length() - 1);
        json.append("]");

        return json.toString();
    }

    /**
     * Constructs a PostRequest.
     * @return a HTTPPost.
     */
    public HttpPost constructPostRequest() {
        return new HttpPost(this.endpoint);
    }

    /**
     * Constructs a HTTPClient.
     * @return a HttpClient.
     */
    public HttpClient constructHttpClient() {
        return HttpClients.createDefault();
    }
}
