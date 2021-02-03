[![BCH compliance](https://bettercodehub.com/edge/badge/fasten-project/incremental-maven-crawler?branch=main)](https://bettercodehub.com/) ![Java CI with Maven](https://github.com/fasten-project/incremental-maven-crawler/workflows/Java%20CI%20with%20Maven/badge.svg)
# Incremental Maven Crawler
Checkpointing: up to and not including.

## Usage
```
usage: IncrementalMavenCrawler
 -i,--interval <hours>            Time to wait between crawl attempts (in
                                  hours). Defaults to 1 hour.
 -o,--output <[std|kafka|rest]>   Output to send the crawled artifacts to.
                                  Defaults to std.
 -bs,--batch_size <amount>        Size of batches to send to output.
                                  Defaults to 50.
 -cd,--checkpoint_dir <hours>     Directory to checkpoint/store latest
                                  crawled index. Used for recovery on
                                  crash or restart. Optional.
 -kb,--kafka_brokers <brokers>    Kafka brokers to connect with. I.e.
                                  broker1:port,broker2:port,... Optional.
 -kt,--kafka_topic <topic>        Kafka topic to produce to.
```

### Outputs
**StdOutput**:   
Outputs to the console using `System.out.println` in a JSON format.

**KafkaOutput**:  
Outputs to a Kafka topic. Requires arguments: `--output kafka`, `--kafka_topic TOPIC`, `--kafka_brokers BROKER1,BROKER2`.
An example JSON output message:
```json
{
   "artifactId":"config",
   "groupId":"software.amazon.awssdk",
   "version":"2.15.58",
   "timestamp":1609791717000,
   "artifactRepository":"https://repo.maven.apache.org/maven2/"
}
```

## Deployment
To build the image:
```bash
mvn clean package
docker build . -t crawler
```
Alternatively, download from GitHub Packages. This requires your token to be [installed](https://docs.github.com/en/packages/guides/configuring-docker-for-use-with-github-packages).
```bash
docker pull URL:latest 
docker tag URL:latest crawler
```

To run:
```bash
docker run crawler [arguments]
```

For example:
```bash
docker run crawler --start_index 682 --output std
```