package eu.fasten.crawler.output;

import eu.fasten.crawler.util.MavenArtifact;

import java.util.Arrays;
import java.util.List;

public interface Output {

    /** Helper methods for constructing and cleaning up the output instance. **/
    void open();
    void close();
    void flush();

    /** Send records to output. **/
    default void send(MavenArtifact artifact) {
        send(Arrays.asList(artifact));
    }

    void send(List<MavenArtifact> artifact);
}
