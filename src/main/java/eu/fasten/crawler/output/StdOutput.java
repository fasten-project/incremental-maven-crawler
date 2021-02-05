package eu.fasten.crawler.output;

import eu.fasten.crawler.data.MavenArtifact;

import java.util.List;

public class StdOutput implements Output {

    /**
     * Prints the artifacts to the screen.
     * @param artifact list of artifacts.
     */
    @Override
    public boolean send(List<MavenArtifact> artifact) {
        artifact.stream().map((a) -> a.toString()).forEach((a) -> {System.out.println(a);});
        return true;
    }
}
