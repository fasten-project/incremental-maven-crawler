package eu.fasten.crawler;

import com.google.common.collect.Lists;
import eu.fasten.crawler.output.Output;
import eu.fasten.crawler.data.MavenArtifact;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexDataReader;
import org.codehaus.plexus.*;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CrawlIndex {

    // Get logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Id of the index.
    private final int index;

    // Input files and streams.
    private final File indexFile;
    private BufferedInputStream is;
    private IndexDataReader reader;

    // Plexus stuff.
    private PlexusContainer plexusContainer;
    private List<IndexCreator> indexers;
    private IndexingContext context;

    // Used to keep track of nonUnique artifacts
    private static int nonUnique = 0;

    /**
     * Crawls a Maven Central (Lucene) index and outputs it.
     *
     * @param index the index number.
     * @param indexFile the index file (we expect a `.gz` Lucene index, generated by the Maven Indexer API).
     */
    public CrawlIndex(int index, File indexFile) {
        this.index = index;
        this.indexFile = indexFile;
        try {
            this.is = new BufferedInputStream(new FileInputStream(this.indexFile));
            this.reader = new IndexDataReader(is);
        } catch (IOException e) {
            throw new IllegalArgumentException("Maven repository file can't be found for index: " + this.index, e);
        }

        setupPlexus();
    }

    /**
     * Setup Plexus, somehow we can't avoid this unfortunately.
     */
    private void setupPlexus() {
        try {
            this.plexusContainer = new DefaultPlexusContainer();
            final DefaultContainerConfiguration configuration = new DefaultContainerConfiguration();
            configuration.setClassWorld( ( (DefaultPlexusContainer) this.plexusContainer ).getClassWorld() );
            configuration.setClassPathScanning( PlexusConstants.SCANNING_INDEX );

            this.plexusContainer = new DefaultPlexusContainer(configuration);
        } catch (PlexusContainerException e) {
            throw new RuntimeException("Cannot construct PlexusContainer for MavenCrawler.", e);
        }

        try {
            this.indexers = new ArrayList<IndexCreator>();
            for (Object component : plexusContainer.lookupList(IndexCreator.class)) {
                indexers.add((IndexCreator) component);
            }
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Cannot add IndexCreators for MavenCrawler.", e);
        }

        this.context = (IndexingContext) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[] { IndexingContext.class }, new PartImplementation()
                {
                    public List<IndexCreator> getIndexCreators()
                    {
                        return indexers;
                    }
                });
    }

    /**
     * Crawls the index and outputs the results.
     * Note: only _unique_ artifacts are outputted. The uniqueness is defined in the MavenArtifact `equals` method.
     *
     * @param output the class to output to (E.g. Kafka).
     * @param batchSize the batch size to send to the output.
     */
    public boolean crawlAndSend(Output output, int batchSize) {
        nonUnique = 0;
        Set<MavenArtifact> artifactSet = new HashSet<>();

        // Setup output.
        output.open();

        IndexDataReader.IndexDataReadVisitor visitor = (doc) -> {
            MavenArtifact artifact = MavenArtifact.fromDocument(doc, context);
            if (artifact == null) {
                logger.warn("Couldn't construct artifact info for document: " + doc.toString() + ". We will skip it.");
                return;
            }

            if (artifactSet.contains(artifact)) {
                nonUnique += 1;
            } else {
                artifactSet.add(artifact);
            }
        };

        try {
            IndexDataReader.IndexDataReadResult result = reader.readIndex(visitor, context);

            // Send to output.
            final List<List<MavenArtifact>> batchedLists = Lists.partition(Lists.newArrayList(artifactSet), batchSize);
            for (List<MavenArtifact> artifacts : batchedLists) {
                boolean res = output.send(artifacts);

                if (!res) {
                    logger.error("Failed sending batch to ouput for index " + index  + ". Exiting current crawl session.");
                    return false;
                }
            }

            // Flush and close output.
            output.flush();
            output.close();

            logger.info("-- Finished crawling! --");
            logger.info("Index publish date: " + result.getTimestamp().toString());
            logger.info("Duplicate documents: " + nonUnique);
            logger.info("Unique documents: " + artifactSet.size());
            logger.info("Total documents: " + result.getDocumentCount());
        } catch (IOException e) {
            logger.error("IOException while reading from the index. " + index + ". Exiting current crawl session.", e);
            return false;
        } finally {
            nonUnique = 0;
        }

        return true;
    }

}
