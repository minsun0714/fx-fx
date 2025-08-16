package com.fx.newfeed.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL =
            "https://v6.exchangerate-api.com/v6/d642f0f3eb22b5ae2f4d44d5/latest/USD";

    private static final List<String> TARGET_CURRENCIES = List.of(
            "KRW", // 대한민국 원
            "JPY", // 일본 엔
            "EUR", // 유로
            "CNY", // 중국 위안
            "GBP", // 영국 파운드
            "AUD", // 호주 달러
            "CAD", // 캐나다 달러
            "CHF", // 스위스 프랑
            "NZD", // 뉴질랜드 달러
            "SGD", // 싱가포르 달러
            "RUB", // 러시아 루블 (전쟁/제재 영향 큼)
            "UAH", // 우크라이나 흐리브냐 (전쟁 지역)
            "TRY", // 터키 리라 (정치/경제 불안정)
            "SAR", // 사우디 리얄 (중동 원유국)
            "AED", // 아랍에미리트 디르함 (중동 금융 허브)
            "IRR"  // 이란 리알 (제재 + 정치 불안정)
    );



    @Scheduled(fixedRate = 60000)
    public void publishExchangeRates() {
        log.info("==== ExchangeRateProducer scheduled triggered ====");
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(API_URL, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode rates = root.path("conversion_rates");

            ArrayNode arrayNode = objectMapper.createArrayNode();
            long now = System.currentTimeMillis();

            for (String target : TARGET_CURRENCIES) {
                double rate = rates.path(target).asDouble();
                ObjectNode item = objectMapper.createObjectNode()
                        .put("base", "USD")
                        .put("target", target)
                        .put("rate", rate)
                        .put("timestamp", now);
                arrayNode.add(item);
            }

            String payload = objectMapper.writeValueAsString(arrayNode);
            kafkaTemplate.send("exchange-rate", payload);
            log.info("Sent to Kafka: {}", payload);

        } catch (Exception e) {
            log.error("Failed to fetch/publish exchange rates", e);
        }
    }
}
