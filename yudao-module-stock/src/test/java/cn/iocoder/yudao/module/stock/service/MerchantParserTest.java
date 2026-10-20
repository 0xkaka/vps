package cn.iocoder.yudao.module.stock.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.iocoder.yudao.module.stock.domain.Product;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class MerchantParserTest {
    private final ObjectMapper json = new ObjectMapper();

    private Product product(String name, String purchase, String source) {
        Product p = new Product();
        p.setName(name);
        p.setGroupFlag(false);
        p.setPurchaseUrl(purchase);
        p.setSourceUrl(source);
        p.setSourceKey("test");
        return p;
    }

    @Test
    void onlyKnownMerchantLinksProduceFixedTargets() {
        Product p =
                product(
                        "RN",
                        "https://vpszdm.com/rn912",
                        "https://my.racknerd.com/aff.php?aff=0&pid=912");
        assertEquals(
                "https://my.racknerd.com/cart.php?a=add&pid=912", MerchantParser.target(p).url());
        p.setSourceUrl("https://my.racknerd.com.attacker.example/?pid=912");
        assertNull(MerchantParser.target(p));
        p.setSourceUrl("http://127.0.0.1/?pid=912");
        assertNull(MerchantParser.target(p));
    }

    @Test
    void seriesAndLotteryAreNotMistakenForAProduct() {
        assertNull(
                MerchantParser.target(
                        product(
                                "搬瓦工全系列",
                                "https://stock.bwh91.com",
                                "https://bwh81.net/cart.php?a=add&pid=151")));
        assertNull(
                MerchantParser.target(
                        product("抽奖", "", "https://bwh81.net/cart.php?a=add&pid=151")));
    }

    @Test
    void bwhAggregatesBillingPeriodsButHonorsExplicitDatacenter() throws Exception {
        var root =
                json.readTree(
                        """
                        {"schema_version":1,"offers":[
                        {"product_id":44,"location":{"datacenter_id":"USCA_9"},"available":false},
                        {"product_id":44,"location":{"datacenter_id":"EUNL_2"},"available":true}]}
                        """);
        Product p = product("Basic", "https://bwh91.com/pid=44", "");
        assertEquals(
                "IN_STOCK", MerchantParser.bandwagon(p, MerchantParser.target(p), root).status());
        p.setName("搬瓦工 USCA_9 Basic");
        assertEquals(
                "OUT_OF_STOCK",
                MerchantParser.bandwagon(p, MerchantParser.target(p), root).status());
        p.setName("搬瓦工 DC99 Basic");
        assertThrows(
                IllegalArgumentException.class,
                () -> MerchantParser.bandwagon(p, MerchantParser.target(p), root));
    }

    @Test
    void missingOrNonBooleanAvailabilityNeverBecomesSoldOut() throws Exception {
        Product p = product("Basic", "https://bwh91.com/pid=44", "");
        for (String data :
                new String[] {
                    "{\"schema_version\":1,\"offers\":[]}",
                    "{\"schema_version\":1,\"offers\":[{\"product_id\":44,\"location\":{\"datacenter_id\":\"USCA_9\"},\"available\":\"false\"}]}"
                }) {
            var root = json.readTree(data);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MerchantParser.bandwagon(p, MerchantParser.target(p), root));
        }
    }

    @Test
    void hostbillMatchesExactPidAndExplicitDisabledButton() {
        Product p = product("ZGO", "", "https://clients.zgovps.com/?cmd=cart&id=160");
        var t = MerchantParser.target(p);
        var doc =
                Jsoup.parse(
                        "<form><input name=id value=160><select"
                            + " name=cycle><option>a</option></select><button"
                            + " type=submit>Continue</button></form><form><input name=id"
                            + " value=161><button disabled type=submit>Out of"
                            + " stock!</button></form>");
        assertEquals("IN_STOCK", MerchantParser.page(p, t, doc).status());
        p.setSourceUrl("https://clients.zgovps.com/?cmd=cart&id=161");
        assertEquals(
                "OUT_OF_STOCK", MerchantParser.page(p, MerchantParser.target(p), doc).status());
        p.setSourceUrl("https://clients.zgovps.com/?cmd=cart&id=999");
        assertThrows(
                IllegalArgumentException.class,
                () -> MerchantParser.page(p, MerchantParser.target(p), doc));
    }

    @Test
    void vpsUsesProductCardNotAnotherProductsStock() {
        Product p = product("VPS", "", "https://vps.hosting/?cmd=cart&id=144");
        var doc =
                Jsoup.parse(
                        "<div class='cart-product outofstock' data-value=145><span"
                            + " class=product-price>10</span></div><div class=cart-product"
                            + " data-value=144><span class=product-price>20</span></div>");
        assertEquals("IN_STOCK", MerchantParser.page(p, MerchantParser.target(p), doc).status());
    }

    @Test
    void whmcsChallengeLoginAndCatalogDoNotImplyStock() {
        Product p = product("CC", "", "https://cloud.colocrossing.com/aff.php?pid=112");
        var t = MerchantParser.target(p);
        for (String html :
                new String[] {
                    "<title>Just a moment...</title>",
                    "<form>Login</form>",
                    "<p>Buy now</p><footer>Out of Stock</footer>"
                })
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MerchantParser.page(p, t, Jsoup.parse(html)));
        assertEquals(
                "OUT_OF_STOCK",
                MerchantParser.page(p, t, Jsoup.parse("<h2>Out of Stock</h2>")).status());
    }

    @Test
    void vmrackResolvesReferencesAndRejectsUnknownEnum() throws Exception {
        Product p =
                product(
                        "VMRack",
                        "https://www.vmrack.net/zh-CN/deploy-new-instance/vps-activation-deals?id=123",
                        "");
        var root = json.readTree("[{\"id\":1,\"name\":2,\"stock_status\":3},\"123\",\"Test\",3]");
        assertEquals(
                "OUT_OF_STOCK", MerchantParser.vmrack(p, MerchantParser.target(p), root).status());
        var unknown =
                json.readTree("[{\"id\":1,\"name\":2,\"stock_status\":3},\"123\",\"Test\",0]");
        assertThrows(
                IllegalArgumentException.class,
                () -> MerchantParser.vmrack(p, MerchantParser.target(p), unknown));
    }
}
