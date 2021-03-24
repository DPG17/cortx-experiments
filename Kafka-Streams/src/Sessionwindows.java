/*This is an application to demonstrate the use of session windowing in kafka streams.
This application does a simple word count operation. 
Main motive was to understand Kafka streams DSL, Kafka and try out different windowing approaches for same.*/

package org.apache.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class SessionWindow {

    public static void main(String[] args) {

        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "Any name of your choice but should follow the app id variable name rules, eg: SEAGATEAPP");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "list of FQDN of your vm's:9092, eg: ssc-vm-1234.colo.seagate.com:9092, ssc..:9092,");
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "(earliest/latest) depending on your requirement");
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> initialstream = builder.stream("NAME OF YOUR INPUT TOPIC", Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, String> Sstream = initialstream
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")));

        KGroupedStream<String, String> SgroupedStream = Sstream
                .groupBy((key, word) -> word, Grouped.with(Serdes.String(), Serdes.String()));

        KTable<Windowed<String>, Long> Stable = SgroupedStream
                .windowedBy(SessionWindows.with(Duration.ofMinutes(5)))
                .count();
        Stable
                .toStream()
                .selectKey((key, word) -> key.key())
                .to("NAME OF THE OUTPUT TOPIC", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
        streams.start();
    }
}
