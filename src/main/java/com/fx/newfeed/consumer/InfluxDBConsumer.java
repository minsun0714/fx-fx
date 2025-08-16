package com.fx.newfeed.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfluxDBConsumer {

    private final InfluxDBClient influxDBClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String orgName;

    @KafkaListener(topics = "exchange-rate", groupId = "influxdb-writer")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            for (JsonNode rateNode : root) {
                String base = rateNode.path("base").asText();
                String target = rateNode.path("target").asText();
                double rate = rateNode.path("rate").asDouble();
                long timestamp = rateNode.path("timestamp").asLong();

                // measurement 이름을 "USD_KRW" 형태로
                String measurementName = base + "_" + target;

                Point point = Point.measurement(measurementName)
                        .addField("value", rate)
                        .time(Instant.ofEpochMilli(timestamp), WritePrecision.MS);

                log.info("bucket: {}, orgName: {}", bucket, orgName);
                writeApi.writePoint(bucket, orgName, point);
                log.info("Saved to InfluxDB: {} -> {} = {}", base, target, rate);
            }

        } catch (Exception e) {
            log.error("Failed to save exchange rates to InfluxDB", e);
        }
    }
}
