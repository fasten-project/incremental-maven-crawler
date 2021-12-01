[![BCH compliance](https://bettercodehub.com/edge/badge/fasten-project/incremental-maven-crawler?branch=main)](https://bettercodehub.com/) ![Java CI with Maven](https://github.com/fasten-project/incremental-maven-crawler/workflows/Java%20CI%20with%20Maven/badge.svg)
# Incremental Maven Crawler
This application crawls from [Maven Central Incremental Index Repository](https://repo1.maven.org/maven2/.index/) with a certain interval. 
Running this application will follow this repository and outputs the __unique artifacts__ released on Maven central.
Currently, Maven Central releases a new (incremental) index __every week__. 

Several outputs exist including Kafka and HTTP support. Moreover, a checkpointing mechanism is added to support persistence across restarts.
More specifically, the `checkpointDir` stores an `INDEX.index` file where the `INDEX` is the _next_ index to crawl. E.g. when `800.index` is stored, the crawler will start crawling _including_ index 800.

## Usage
```
usage: IncrementalMavenCrawler
 -i,--interval <hours>            Time to wait between crawl attempts (in
                                  hours). Defaults to 1 hour.
 -o,--output <[std|kafka|rest]>   Output to send the crawled artifacts to.
                                  Defaults to std.
 -si,start_index                  Index to start crawling from (inclusive). Required.
 -bs,--batch_size <amount>        Size of batches to send to output.
                                  Defaults to 50.
 -cd,--checkpoint_dir <hours>     Directory to checkpoint/store latest
                                  crawled index. Used for recovery on
                                  crash or restart. Optional.
 -kb,--kafka_brokers <brokers>    Kafka brokers to connect with. I.e.
                                  broker1:port,broker2:port,...
                                  Required for Kafka output.
 -kt,--kafka_topic <topic>        Kafka topic to produce to.
                                  Required for Kafka output.
 -re,--rest_endpoint <url>        HTTP endpoint to post crawled batches to.
                                  Required for Rest output.

```

### Outputs
An example JSON output message:
```json
{
   "artifactId":"config",
   "groupId":"software.amazon.awssdk",
   "version":"2.15.58",
   "date":1609791717000,
   "artifactRepository":"https://repo.maven.apache.org/maven2/"
}
```

**StdOutput**:   
Outputs to the console using `System.out.println` in a JSON format.

**KafkaOutput**:  
Outputs to a Kafka topic.  
Requires the arguments: 
- `--output kafka`; switch output mode to kafka
- `--kafka_topic TOPIC`; the kafka topic to send to.
`--kafka_brokers BROKER1,BROKER2`; the brokers to connect to.

## Deployment
To build the image:
```bash
mvn clean package
docker build . -t crawler
```
Alternatively, download from GitHub Packages. This requires your token to be [installed](https://docs.github.com/en/packages/guides/configuring-docker-for-use-with-github-packages).
```bash
docker pull docker.pkg.github.com/fasten-project/incremental-maven-crawler/crawler:latest
docker tag docker.pkg.github.com/fasten-project/incremental-maven-crawler/crawler:latest crawler
```

To run:
```bash
docker run crawler [arguments]
```

For example:
```bash
docker run crawler --start_index 682 --output std
```