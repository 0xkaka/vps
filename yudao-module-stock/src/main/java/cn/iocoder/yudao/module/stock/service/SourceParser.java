package cn.iocoder.yudao.module.stock.service;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 原站公开 HTML 的结构适配器，不执行页面脚本。 @author vps */
@Component
public class SourceParser {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record RegionState(String name, String status, String url) {}

    public record Snapshot(
            String key, String status, LocalDateTime checked, List<RegionState> regions) {
        public Snapshot(String key, String status, LocalDateTime checked) {
            this(key, status, checked, List.of());
        }
    }

    public Map<String, Snapshot> parse(Document document) {
        Map<String, Snapshot> result = new LinkedHashMap<>(256);
        for (Element row : document.select("#product-table tbody tr")) {
            Element status = row.selectFirst(".status-badge");
            Element time = row.selectFirst(".time-info");
            List<RegionState> regions = new ArrayList<>();
            for (Element dc : row.select(".dc-status-item")) {
                regions.add(
                        new RegionState(
                                dc.text(),
                                dc.hasClass("dc-in-stock")
                                        ? "IN_STOCK"
                                        : dc.hasClass("dc-out-stock") ? "OUT_OF_STOCK" : "UNKNOWN",
                                dc.attr("href")));
            }
            if ((status == null && regions.isEmpty()) || time == null) {
                continue;
            }
            String state =
                    status == null
                            ? (regions.stream().anyMatch(r -> "IN_STOCK".equals(r.status()))
                                    ? "IN_STOCK"
                                    : regions.stream()
                                                    .allMatch(
                                                            r -> "OUT_OF_STOCK".equals(r.status()))
                                            ? "OUT_OF_STOCK"
                                            : "UNKNOWN")
                            : status.hasClass("status-in-stock")
                                    ? "IN_STOCK"
                                    : status.hasClass("status-out-of-stock")
                                            ? "OUT_OF_STOCK"
                                            : "UNKNOWN";
            String key =
                    key(row.attr("data-url"), row.attr("data-purchase"), row.hasAttr("data-group"));
            try {
                result.put(
                        key,
                        new Snapshot(
                                key,
                                state,
                                LocalDateTime.parse(time.text().trim(), TIME),
                                regions));
            } catch (java.time.format.DateTimeParseException ignored) {
                /* 不使用无效时间覆盖正常库存。 */
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("原站页面结构未匹配，保留现有库存");
        }
        return result;
    }

    public static String key(String source, String purchase, boolean group) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            (source + "\n" + purchase + "\n" + group)
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
