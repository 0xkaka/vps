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
    public static final String MAIN_PAGE = "https://stock.vpszdm.com/";
    public static final Set<String> PAGES =
            Set.of(
                    MAIN_PAGE,
                    "https://stock.bwh91.com/",
                    "https://stock.dmitea.com/",
                    "https://matrix.bwh91.com/",
                    "https://matrix.dmitea.com/");

    public Map<String, Snapshot> parsePage(String page, Document document) {
        if (!PAGES.contains(page)) {
            throw new IllegalArgumentException("不支持的来源");
        }
        if (MAIN_PAGE.equals(page)) {
            return parse(document);
        }
        Map<String, Snapshot> result = new LinkedHashMap<>(256);
        if (page.contains("matrix.")) {
            for (Element row : document.select("#vpsTable tbody tr")) {
                var cells = row.select("td");
                if (cells.size() != 25
                        || !Set.of("搬瓦工", "DMIT").contains(cells.get(0).text().trim())) {
                    continue;
                }
                String model = cells.get(3).text().trim();
                if (model.isBlank()
                        || cells.get(2).text().isBlank()
                        || "nan".equalsIgnoreCase(model)) {
                    continue;
                }
                Element buy = cells.get(14).selectFirst("a[href]");
                String key =
                        matrixKey(
                                cells.get(0).text().trim(),
                                cells.get(2).text().trim(),
                                model,
                                buy == null ? "" : buy.attr("href"));
                String label = cells.get(4).text().trim();
                String status =
                        "有货".equals(label)
                                ? "IN_STOCK"
                                : "无货".equals(label) ? "OUT_OF_STOCK" : "UNKNOWN";
                // 大全页面没有提供库存检查时间，不能将抓取时间冒充商家检查时间。
                result.put(key, new Snapshot(key, status, null));
            }
        } else {
            for (Element row : document.select("table tbody tr")) {
                Element name = row.selectFirst(".product-name");
                Element status = row.selectFirst(".status-badge");
                Element purchase = row.selectFirst(".purchase-btn");
                var times = row.select(".time-info");
                if (name == null || status == null || purchase == null || times.isEmpty()) {
                    continue;
                }
                String key = dedicatedKey(name.text().trim(), purchase.attr("href"));
                try {
                    String state =
                            status.hasClass("status-in-stock")
                                    ? "IN_STOCK"
                                    : status.hasClass("status-out-of-stock")
                                            ? "OUT_OF_STOCK"
                                            : "UNKNOWN";
                    result.put(
                            key,
                            new Snapshot(
                                    key,
                                    state,
                                    LocalDateTime.parse(times.last().text().trim(), TIME)));
                } catch (java.time.format.DateTimeParseException ignored) {
                    /* 保留原值。 */
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("来源结构未匹配，保留现有库存");
        }
        return result;
    }

    public static String matrixKey(String vendor, String dc, String model, String purchase) {
        return key("matrix:" + vendor + "\n" + dc, model.trim() + "\n" + purchase, false);
    }

    public static String dedicatedKey(String name, String purchase) {
        return key("stock:" + name.trim(), purchase, false);
    }

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
