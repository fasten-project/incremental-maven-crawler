# Incremental Maven Crawler
Add description here

## Usage
```
usage: IncrementalMavenCrawler
 -bs,--batch_size <amount>             Size of batches to send to output.
                                       Defaults to 50.
 -i,--start_index <index>              Index to start crawling from
                                       (inclusive).
 -o,--output_type <[std|kafka|rest]>   Output to send the crawled
                                       artifacts to.
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