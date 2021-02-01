package eu.fasten.crawler.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;

import java.util.Objects;

public class MavenArtifact {

    private static ObjectMapper mapper = new ObjectMapper();

    // Unique identifier of a Maven artifact.
    private final String artifactId;
    private final String groupId;
    private final String version;
    private final String repositoryUrl;
    private final Long timestamp;

    private static final String SEPARATOR = ":";

    public MavenArtifact(String artifactId, String groupId, String version, Long timestamp) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
        this.timestamp = timestamp;
        this.repositoryUrl = "https://repo.maven.apache.org/maven2/";
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public Long getTimestamp () {
        return timestamp;
    }

    public String toString() {
        ObjectNode node = mapper.createObjectNode();
        node.put("artifactId", this.getArtifactId());
        node.put("groupId", this.getGroupId());
        node.put("version", this.getVersion());
        node.put("timestamp", this.getTimestamp());
        node.put("artifactRepository", this.getRepositoryUrl());
        return node.toString();
    }

    public static MavenArtifact fromDocument(Document document, IndexingContext context) {
        ArtifactInfo ai = IndexUtils.constructArtifactInfo(document, context);

        if (ai == null) {
            return null;
        }

        return new MavenArtifact(ai.getArtifactId(), ai.getGroupId(), ai.getVersion(), ai.getLastModified());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenArtifact artifact = (MavenArtifact) o;
        return Objects.equals(artifactId, artifact.artifactId) &&
                Objects.equals(groupId, artifact.groupId) &&
                Objects.equals(version, artifact.version) &&
                Objects.equals(repositoryUrl, artifact.repositoryUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, groupId, version, repositoryUrl);
    }
}
