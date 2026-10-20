package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.module.stock.domain.Product;

import com.fasterxml.jackson.databind.JsonNode;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/** 商家只读数据适配。缺失产品、验证页和未知字段都不是无货。 @author vps */
public final class MerchantParser {
    private MerchantParser() {}

    public record Target(String merchant, String pid, String url, String kind) {}

    private static final Pattern PID = Pattern.compile("(?:[?&/]pid=)([0-9]+)(?:[&#]|$)");
    private static final Pattern ID = Pattern.compile("(?:[?&]id=)([0-9]+)(?:[&#]|$)");
    private static final Map<String, String> WHMCS =
            Map.of(
                    "my.racknerd.com",
                    "racknerd",
                    "cloud.colocrossing.com",
                    "colocrossing",
                    "app.vmiss.com",
                    "vmiss",
                    "gomami.io",
                    "gomami",
                    "billing.dedirock.com",
                    "dedirock",
                    "www.dmit.io",
                    "dmit",
                    "bandwagonhost.com",
                    "bandwagon");

    public static Target target(Product p) {
        if (Boolean.TRUE.equals(p.getGroupFlag())
                || Objects.toString(p.getName(), "").contains("全系列")
                || Objects.toString(p.getName(), "").contains("抽奖")
                || Objects.toString(p.getPurchaseUrl(), "").startsWith("https://stock."))
            return null;
        // 不按名称猜编号，也不跟随推广站任意跳转。所有请求地址由固定商家域名生成。
        for (String raw :
                List.of(
                        Objects.toString(p.getPurchaseUrl(), ""),
                        Objects.toString(p.getSourceUrl(), ""))) {
            try {
                URI u = URI.create(raw);
                String host = u.getHost();
                if (host == null || u.getUserInfo() != null) continue;
                var pid = PID.matcher(raw);
                var id = ID.matcher(raw);
                if (Set.of("bwh91.com", "bwh81.net", "bandwagonhost.com").contains(host)
                        && pid.find())
                    return new Target(
                            "bandwagon",
                            pid.group(1),
                            "https://bandwagonhost.com/order/catalog.json",
                            "OFFICIAL_API");
                if ("dmitea.com".equals(host) && pid.find())
                    return whmcs("www.dmit.io", "dmit", pid.group(1));
                if (WHMCS.containsKey(host) && pid.find())
                    return whmcs(host, WHMCS.get(host), pid.group(1));
                if ("clients.zgovps.com".equals(host) && id.find())
                    return new Target(
                            "zgo",
                            id.group(1),
                            "https://clients.zgovps.com/index.php?/cart/special-offer/",
                            "OFFICIAL_PAGE");
                if ("vps.hosting".equals(host) && id.find())
                    return new Target(
                            "vps",
                            id.group(1),
                            "https://vps.hosting/?cmd=cart&action=add&id=" + id.group(1),
                            "OFFICIAL_PAGE");
                if ("www.vmrack.net".equals(host) && id.find())
                    return new Target(
                            "vmrack",
                            id.group(1),
                            "https://www.vmrack.net/zh-CN/pricing",
                            "OFFICIAL_PAGE");
            } catch (IllegalArgumentException ignored) {
                /* 非法地址不参与请求。 */
            }
        }
        return null;
    }

    private static Target whmcs(String host, String merchant, String pid) {
        return new Target(
                merchant, pid, "https://" + host + "/cart.php?a=add&pid=" + pid, "OFFICIAL_PAGE");
    }

    public static SourceParser.Snapshot bandwagon(Product p, Target target, JsonNode root) {
        if (root.path("schema_version").asInt(-1) != 1 || !root.path("offers").isArray())
            throw new IllegalArgumentException("商家目录格式变化");
        String dc = explicitDatacenter(p.getName());
        Map<String, Boolean> available = new LinkedHashMap<>();
        Map<String, String> urls = new HashMap<>();
        for (JsonNode offer : root.path("offers")) {
            if (!target.pid().equals(offer.path("product_id").asText())) continue;
            String location = offer.path("location").path("datacenter_id").asText();
            if (dc != null && !dc.equals(location)) continue;
            if (location.isBlank() || !offer.path("available").isBoolean())
                throw new IllegalArgumentException("库存字段缺失");
            available.merge(location, offer.path("available").booleanValue(), Boolean::logicalOr);
            String cart = offer.path("cart_url").asText();
            if (cart.startsWith("https://bandwagonhost.com/cart.php?")) urls.put(location, cart);
        }
        if (available.isEmpty()) throw new IllegalArgumentException("官方目录未收录该产品或机房");
        List<SourceParser.RegionState> regions =
                available.entrySet().stream()
                        .map(
                                e ->
                                        new SourceParser.RegionState(
                                                e.getKey(),
                                                status(e.getValue()),
                                                urls.getOrDefault(e.getKey(), "")))
                        .toList();
        return new SourceParser.Snapshot(
                p.getSourceKey(),
                status(available.containsValue(true)),
                LocalDateTime.now(),
                regions);
    }

    private static String explicitDatacenter(String name) {
        if (name == null || !name.startsWith("搬瓦工 ")) return null;
        String token = name.split(" ")[1];
        Map<String, String> aliases =
                Map.of(
                        "DC5", "USCA_5", "HKHK_8", "HK_8", "JPTYO_8", "JPTY_8", "JPOS6", "JPOS_6",
                        "EUNL1", "EUNL_1");
        if (aliases.containsKey(token)) return aliases.get(token);
        return token.matches("[A-Z]+[A-Z0-9_]*[0-9]") ? token : null;
    }

    public static SourceParser.Snapshot page(Product p, Target t, Document doc) {
        String text = doc.text().toLowerCase(Locale.ROOT);
        if (text.contains("verify you are human")
                || text.contains("cloudflare ray id")
                || text.contains("performing security verification")
                || doc.title().contains("Just a moment"))
            throw new IllegalArgumentException("商家访问验证，需白名单或授权");
        if ("vps".equals(t.merchant())) {
            Element card = doc.selectFirst(".cart-product[data-value='" + t.pid() + "']");
            if (card == null || card.selectFirst(".product-price") == null)
                throw new IllegalArgumentException("官方页面未匹配产品编号");
            return snapshot(p, !card.hasClass("outofstock"));
        }
        if ("zgo".equals(t.merchant())) {
            for (Element form : doc.select("form")) {
                Element id = form.selectFirst("input[name=id]");
                if (id == null || !t.pid().equals(id.val())) continue;
                Element button = form.selectFirst("button[type=submit]");
                if (button != null
                        && button.hasAttr("disabled")
                        && button.text().toLowerCase(Locale.ROOT).contains("out of stock"))
                    return snapshot(p, false);
                if (button != null
                        && !button.hasAttr("disabled")
                        && "Continue".equals(button.text().trim())
                        && form.selectFirst("select[name=cycle]") != null) return snapshot(p, true);
            }
            throw new IllegalArgumentException("官方分类页未收录该产品编号");
        }
        // 只接受明确的缺货标题；模板页脚或其他套餐的缺货文字不构成证据。
        if (doc.select("h1,h2,h3,.alert-danger,.alert-warning").stream()
                .anyMatch(e -> e.text().trim().equalsIgnoreCase("Out of Stock")))
            return snapshot(p, false);
        Element form = doc.selectFirst("form#frmConfigureProduct");
        if (form != null
                && form.selectFirst("input[name=configure][value=true]") != null
                && form.selectFirst(
                                "#btnCompleteProductConfig,button[type=submit],input[type=submit]")
                        != null) return snapshot(p, true);
        throw new IllegalArgumentException("未找到明确库存证据，可能下架或需要登录");
    }

    /** 官网 Nuxt SSR 数据中的套餐库存，数值与可见的售罄/库存紧张标签交叉核验。 */
    public static SourceParser.Snapshot vmrack(Product p, Target t, JsonNode array) {
        if (!array.isArray()) throw new IllegalArgumentException("官网数据格式变化");
        for (JsonNode item : array) {
            if (!item.isObject()
                    || !item.has("id")
                    || !item.has("stock_status")
                    || !item.has("name")) continue;
            JsonNode id = reference(array, item.path("id"));
            if (!t.pid().equals(id.asText())) continue;
            JsonNode status = reference(array, item.path("stock_status"));
            if (!status.isIntegralNumber() || status.intValue() < 1 || status.intValue() > 3)
                throw new IllegalArgumentException("未知库存枚举");
            return snapshot(p, status.intValue() != 3);
        }
        throw new IllegalArgumentException("官方定价页未收录该套餐编号");
    }

    private static JsonNode reference(JsonNode array, JsonNode index) {
        if (!index.isIntegralNumber() || index.intValue() < 0 || index.intValue() >= array.size())
            throw new IllegalArgumentException("官网数据引用无效");
        return array.get(index.intValue());
    }

    static SourceParser.Snapshot snapshot(Product p, boolean available) {
        return new SourceParser.Snapshot(p.getSourceKey(), status(available), LocalDateTime.now());
    }

    private static String status(boolean available) {
        return available ? "IN_STOCK" : "OUT_OF_STOCK";
    }
}
