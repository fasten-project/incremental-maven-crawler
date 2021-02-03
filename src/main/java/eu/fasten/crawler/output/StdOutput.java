package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;

import java.util.List;

public class StdOutput implements Output {

    @Override
    public void open() {}

    @Override
    public void close() {}

    @Override
    public void flush() {}

    /**
     * Prints the artifacts to the screen.
     * @param artifact list of artifacts.
     */
    @Override
    public void send(List<MavenArtifact> artifact) {
        artifact.stream().map((a) -> a.toString()).forEach((a) -> {System.out.println(a);});
    }
}
