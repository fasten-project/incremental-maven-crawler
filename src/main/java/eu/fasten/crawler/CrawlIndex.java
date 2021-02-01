package eu.fasten.crawler;

import com.google.common.collect.Lists;
import eu.fasten.crawler.output.Output;
import eu.fasten.crawler.util.MavenArtifact;
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

    // Used to keep track of nonUnique artifacts;
    private static int nonUnique = 0;

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

    // Does not support batches.
    public void crawlAndSend(Output output) {
        IndexDataReader.IndexDataReadVisitor visitor = (doc) -> {
            MavenArtifact artifact = MavenArtifact.fromDocument(doc, context);

            if (artifact != null) {
                output.send(artifact);
            } else {
                logger.warn("Couldn't construct artifact info for document: " + doc.toString() + ". We will skip it.");
            }
        };

        try {
            IndexDataReader.IndexDataReadResult result = reader.readIndex(visitor, context);

            logger.info("-- Finished crawling! --");
            logger.info("Crawl date: " + result.getTimestamp().toString());
            logger.info("Total documents: " + result.getDocumentCount());
        } catch (IOException e) {
            logger.error("IOException while reading from the index", e);
            throw new RuntimeException("Now exiting due to IOExcepton.");
        }
    }

    public void crawlAndSendUnique(Output output, int batchSize) {
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
            batchedLists.forEach((l) -> output.send(l));

            // Flush and close output.
            output.flush();
            output.close();

            logger.info("-- Finished crawling! --");
            logger.info("Index publish date: " + result.getTimestamp().toString());
            logger.info("Duplicate documents: " + nonUnique);
            logger.info("Unique documents: " + artifactSet.size());
            logger.info("Total documents: " + result.getDocumentCount());
        } catch (IOException e) {
            logger.error("IOException while reading from the index", e);
            throw new RuntimeException("Now exiting due to IOExcepton.");
        } finally {
            nonUnique = 0;
        }
    }

}
