/*This is a windowing application written using Kafka streams DSL.
It performs a windowed word count operation on data consumed form input topic*/

package org.apache.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.kstream.TimeWindowedDeserializer;
import org.apache.kafka.streams.kstream.TimeWindowedSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class HoppingWindow {

    public static void main(String[] args) {

        StringSerializer stringSerializer = new StringSerializer();
        StringDeserializer stringDeserializer = new StringDeserializer();
        TimeWindowedSerializer<String> windowedSerializer = new TimeWindowedSerializer<>(stringSerializer);
        TimeWindowedDeserializer<String> windowedDeserializer = new TimeWindowedDeserializer<>(stringDeserializer);
        Serde<Windowed<String>> windowSerde = Serdes.serdeFrom(windowedSerializer, windowedDeserializer);

        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "Any name of your choice");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "List of FQDN of your vm's:9092, eg: ssc-vm-1234.colo.seagate.com:9092, ssc...:9092");
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "(earliest/latest) depending on your requirement");
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        Duration windowSizeMs = Duration.ofMinutes(5);
        Duration advanceMs =    Duration.ofMinutes(1);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> initialstream = builder.stream("NAME OF INPUT TOPIC", Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, String> Hstream = initialstream
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")));

        KGroupedStream<String, String> HgroupedStream = Hstream
                .groupBy((key, word) -> word, Grouped.with(Serdes.String(), Serdes.String()));

        KTable<Windowed<String>, Long> Htable = HgroupedStream
                .windowedBy(TimeWindows.of(windowSizeMs).advanceBy(advanceMs))
                .count();
        Htable
                .toStream()
                .selectKey((key, word) -> key.key())
                .to("NAME OF OUTPUT TOPIC", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
