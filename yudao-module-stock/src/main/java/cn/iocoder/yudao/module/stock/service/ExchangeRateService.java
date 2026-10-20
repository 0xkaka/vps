package cn.iocoder.yudao.module.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/** 缓存每日参考汇率，展示折合月价；不用于结算。 @author vps */
@Service
public class ExchangeRateService {
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final String KEY = "vps:fx:cny:v1";
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public ExchangeRateService(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 5000)
    public void refresh() {
        try {
            String body =
                    Jsoup.connect("https://api.frankfurter.dev/v1/latest?base=CNY")
                            .ignoreContentType(true)
                            .followRedirects(false)
                            .timeout(5000)
                            .maxBodySize(65536)
                            .userAgent("VPS-Stock-Monitor/1.0")
                            .execute()
                            .body();
            Map<String, Object> value = parse(json.readTree(body), LocalDate.now(ZoneOffset.UTC));
            redis.opsForValue().set(KEY, json.writeValueAsString(value), Duration.ofDays(7));
        } catch (Exception e) {
            LOG.warn("参考汇率更新失败，保留已缓存汇率: {}", e.getClass().getSimpleName());
        }
    }

    public Map<String, Object> current() {
        try {
            String value = redis.opsForValue().get(KEY);
            return value == null ? Map.of() : json.readValue(value, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    static Map<String, Object> parse(JsonNode root, LocalDate today) {
        LocalDate date = LocalDate.parse(root.path("date").asText());
        if (!"CNY".equals(root.path("base").asText())
                || root.path("amount").asDouble() != 1.0
                || date.isAfter(today.plusDays(1))
                || date.isBefore(today.minusDays(10))) {
            throw new IllegalArgumentException("无效参考汇率快照");
        }
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("CNY", 1.0);
        root.path("rates")
                .fields()
                .forEachRemaining(
                        entry -> {
                            double rate = entry.getValue().asDouble();
                            if (entry.getKey().matches("[A-Z]{3}")
                                    && Double.isFinite(rate)
                                    && rate > 0) {
                                rates.put(entry.getKey(), 1.0 / rate);
                            }
                        });
        if (!rates.containsKey("USD") || !rates.containsKey("EUR") || !rates.containsKey("CAD")) {
            throw new IllegalArgumentException("参考汇率缺少主要币种");
        }
        return Map.of("date", date.toString(), "provider", "Frankfurter", "cnyPerUnit", rates);
    }
}
