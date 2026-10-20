package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.module.stock.domain.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

/** 星空 VPS 公开快照适配，仅按商家和产品编号匹配，不采用推广链接。 @author vps */
@Service
public class SpaceVpsSourceService {
    public static final String URL = "https://spacevps.cc/api/data.json";
    public static final String TYPE = "THIRD_PARTY_API";
    private static final Set<String> MERCHANTS = Set.of("vmiss", "dmit", "gomami", "dedirock");
    private static final Logger LOG = LoggerFactory.getLogger(SpaceVpsSourceService.class);
    private final ObjectMapper mapper;

    public SpaceVpsSourceService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static boolean supports(Product product) {
        var target = MerchantParser.target(product);
        return target != null && MERCHANTS.contains(target.merchant());
    }

    public Map<Long, SourceParser.Snapshot> read(List<Product> products) {
        if (products.stream().noneMatch(SpaceVpsSourceService::supports)) return Map.of();
        try {
            var response =
                    Jsoup.connect(URL)
                            .ignoreContentType(true)
                            .followRedirects(false)
                            .timeout(15000)
                            .maxBodySize(8 * 1024 * 1024)
                            .userAgent("VPS-Stock-Monitor/1.0")
                            .execute();
            if (response.statusCode() != 200) throw new IllegalArgumentException("上游响应异常");
            return parse(mapper.readTree(response.body()), products, Instant.now());
        } catch (Exception e) {
            LOG.warn("星空 VPS 快照不可用，回退原有来源：{}", e.getClass().getSimpleName());
            return Map.of();
        }
    }

    static Map<Long, SourceParser.Snapshot> parse(
            JsonNode root, List<Product> products, Instant now) {
        // stockUpdatedAt 是上游全局库存同步时间，不用 generatedAt 或本站读取时间代替。
        Instant updated = OffsetDateTime.parse(root.path("stockUpdatedAt").asText()).toInstant();
        if (updated.isBefore(now.minusSeconds(600))
                || updated.isAfter(now.plusSeconds(120))
                || !root.path("providers").isArray()) {
            throw new IllegalArgumentException("上游库存快照过期或格式变化");
        }
        Map<String, String> states = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (JsonNode provider : root.path("providers")) {
            String merchant = provider.path("slug").asText();
            if (!MERCHANTS.contains(merchant)) continue;
            for (JsonNode line : provider.path("lines")) {
                for (JsonNode plan : line.path("plans")) {
                    Product candidate = new Product();
                    candidate.setName(plan.path("name").asText());
                    candidate.setPurchaseUrl(plan.path("buyUrl").asText());
                    var target = MerchantParser.target(candidate);
                    if (target == null || !merchant.equals(target.merchant())) continue;
                    String key = merchant + ":" + target.pid();
                    String state = plan.path("stockStatus").asText("unknown");
                    String old = states.putIfAbsent(key, state);
                    if (old != null && !old.equals(state)) ambiguous.add(key);
                }
            }
        }
        Map<Long, SourceParser.Snapshot> result = new HashMap<>();
        for (Product product : products) {
            var target = MerchantParser.target(product);
            if (target == null || !MERCHANTS.contains(target.merchant())) continue;
            String key = target.merchant() + ":" + target.pid();
            String state = states.get(key);
            if (ambiguous.contains(key)
                    || !Set.of("in", "out").contains(Objects.toString(state, ""))) continue;
            result.put(
                    product.getId(),
                    new SourceParser.Snapshot(
                            product.getSourceKey(),
                            "in".equals(state) ? "IN_STOCK" : "OUT_OF_STOCK",
                            LocalDateTime.ofInstant(updated, ZoneId.of("Asia/Shanghai"))));
        }
        return result;
    }
}
