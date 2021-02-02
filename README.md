[![BCH compliance](https://bettercodehub.com/edge/badge/fasten-project/incremental-maven-crawler?branch=main)](https://bettercodehub.com/) ![Java CI with Maven](https://github.com/fasten-project/incremental-maven-crawler/workflows/Java%20CI%20with%20Maven/badge.svg)
# Incremental Maven Crawler
Add description here

## Usage
```
usage: IncrementalMavenCrawler
 -bs,--batch_size <amount>        Size of batches to send to output.
                                  Defaults to 50.
 -cd,--checkpoint_dir <hours>     Directory to checkpoint/store latest
                                  crawled index. Used for recovery on
                                  crash or restart.
 -i,--interval <hours>            Time to wait between crawl attempts (in
                                  hours). Defaults to 1 hour.
 -kb,--kafka_brokers <brokers>    Kafka brokers to connect with. I.e.
                                  broker1:port,broker2:port,...
 -kt,--kafka_topic <topic>        Kafka topic to produce to.
 -o,--output <[std|kafka|rest]>   Output to send the crawled artifacts to.
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

## Local deployment
todo

## Production deployment
todo

## Influx integration
todo