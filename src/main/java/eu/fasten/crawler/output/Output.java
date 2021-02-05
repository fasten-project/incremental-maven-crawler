package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;

import java.util.Arrays;
import java.util.List;

public interface Output {

    /** Helper methods for constructing and cleaning up the output instance. **/
    default void open() {}
    default void close() {}
    default void flush() {}

    /** Send records to output. **/
    default boolean send(MavenArtifact artifact) {
        return send(Arrays.asList(artifact));
    }

    boolean send(List<MavenArtifact> artifact);
}
