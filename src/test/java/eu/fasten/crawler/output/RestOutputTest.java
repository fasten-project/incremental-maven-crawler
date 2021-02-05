package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RestOutputTest {

    @Test
    public void testRestOutputFailed() throws Exception {
        RestOutput output = spy(new RestOutput(""));

        HttpClient client = mock(HttpClient.class);
        HttpPost post = mock(HttpPost.class);

        when(output.constructHttpClient()).thenReturn(client);
        when(output.constructPostRequest()).thenReturn(post);

        when(client.execute(post)).thenThrow(IOException.class);
        boolean res = output.send(new MavenArtifact("a", "b", "c", 0L));

        assertFalse(res);
    }

    @Test
    public void testRestOutputFailedStatus() throws Exception {
        RestOutput output = spy(new RestOutput(""));

        HttpClient client = mock(HttpClient.class);
        HttpPost post = mock(HttpPost.class);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine line = mock(StatusLine.class);

        when(output.constructHttpClient()).thenReturn(client);
        when(output.constructPostRequest()).thenReturn(post);
        when(client.execute(post)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(line);
        when(line.getStatusCode()).thenReturn(201);

        boolean res = output.send(new MavenArtifact("a", "b", "c", 0L));

        assertFalse(res);
    }

    @Test
    public void testRestOutputSuccessStatus() throws Exception {
        RestOutput output = spy(new RestOutput(""));

        HttpClient client = mock(HttpClient.class);
        HttpPost post = mock(HttpPost.class);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine line = mock(StatusLine.class);

        when(output.constructHttpClient()).thenReturn(client);
        when(output.constructPostRequest()).thenReturn(post);
        when(client.execute(post)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(line);
        when(line.getStatusCode()).thenReturn(200);

        boolean res = output.send(new MavenArtifact("a", "b", "c", 0L));

        assertTrue(res);
    }

    @Test
    public void testVerifyJSON() throws Exception {
        RestOutput output = spy(new RestOutput(""));
        MavenArtifact af = new MavenArtifact("a", "b", "c", 0L);
        String list = output.buildJsonList(List.of(af));


        assertEquals("[" + af.toString() + "]", list);
    }

    @Test
    public void testVerifyJSONList() throws Exception {
        RestOutput output = spy(new RestOutput(""));
        MavenArtifact af = new MavenArtifact("a", "b", "c", 0L);
        MavenArtifact af2 = new MavenArtifact("a", "b", "d", 0L);
        String list = output.buildJsonList(List.of(af, af2));


        System.out.println(list);

        assertEquals("[" + af.toString() + "," + af2 + "]", list);
    }
}
