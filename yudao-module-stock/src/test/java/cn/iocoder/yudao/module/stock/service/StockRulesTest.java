package cn.iocoder.yudao.module.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.module.stock.domain.*;
import cn.iocoder.yudao.module.stock.mapper.*;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/** 抓取失败保护、库存通知边界及危险输入回归测试。 @author vps */
class StockRulesTest {
    @Test
    void parsesDistinctGroupAndProductKeys() {
        String row =
                "<tr data-url='https://a.test/' data-purchase='https://b.test/' %s><td><span"
                        + " class='status-badge status-in-stock'>有货</span><div"
                        + " class='time-info'>2026-10-20 12:30:00</div></td></tr>";
        var values =
                new SourceParser()
                        .parse(
                                Jsoup.parse(
                                        "<table id='product-table'><tbody>"
                                                + row.formatted("data-group='a'")
                                                + row.formatted("")
                                                + "</tbody></table>"));
        assertEquals(2, values.size());
        assertEquals(
                "IN_STOCK",
                values.get(SourceParser.key("https://a.test/", "https://b.test/", false)).status());
    }

    @Test
    void aggregatesPerDatacenterStock() {
        var doc =
                Jsoup.parse(
                        "<table id='product-table'><tbody><tr data-url='https://a.test'"
                            + " data-purchase='https://b.test'><td><a class='dc-status-item"
                            + " dc-out-stock' href='https://b.test'>纽约</a><a class='dc-status-item"
                            + " dc-in-stock' href='https://b.test'>洛杉矶</a><div"
                            + " class='time-info'>2026-10-20"
                            + " 12:30:00</div></td></tr></tbody></table>");
        var result = new SourceParser().parse(doc).values().iterator().next();
        assertEquals("IN_STOCK", result.status());
        assertEquals(2, result.regions().size());
    }

    @Test
    void rejectsChangedSourceMarkupInsteadOfMarkingSoldOut() {
        assertThrows(
                IllegalStateException.class,
                () -> new SourceParser().parse(Jsoup.parse("<html>upstream error</html>")));
    }

    @Test
    void rejectsInvalidCheckTime() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new SourceParser()
                                .parse(
                                        Jsoup.parse(
                                                "<table id='product-table'><tbody><tr><td><span"
                                                    + " class='status-badge"
                                                    + " status-in-stock'>有货</span><div"
                                                    + " class='time-info'>unknown</div></td></tr></tbody></table>")));
    }

    @Test
    void rejectsExecutableLinks() {
        Link link = new Link();
        link.setUrl("javascript:alert(1)");
        assertThrows(RuntimeException.class, () -> StockValidation.validate(link));
    }

    @Test
    void rejectsPrivateAndSpoofedPushHosts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PushGateway.validate("http://127.0.0.1/admin", "x", "x"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PushGateway.validate(
                                "https://fcm.googleapis.com.attacker.test/push", "x", "x"));
    }

    @Test
    void stableStockDoesNotCreateNotifications() {
        ProductMapper products = mock(ProductMapper.class);
        CheckLogMapper logs = mock(CheckLogMapper.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CatalogCache cache = mock(CatalogCache.class);
        when(products.updateStock(anyLong(), anyString(), anyString(), any())).thenReturn(1);
        Product p = new Product();
        p.setId(1L);
        p.setName("test");
        p.setStockStatus("IN_STOCK");
        p.setEnabled(true);
        p.setGroupFlag(false);
        assertTrue(
                new StockChangeService(products, logs, jdbc, cache)
                        .apply(p, new SourceParser.Snapshot("x", "IN_STOCK", LocalDateTime.now())));
        verifyNoInteractions(jdbc);
        verify(logs).insert(any());
    }

    @Test
    void lostConcurrentUpdateDoesNotEmitNotification() {
        ProductMapper products = mock(ProductMapper.class);
        CheckLogMapper logs = mock(CheckLogMapper.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CatalogCache cache = mock(CatalogCache.class);
        Product p = new Product();
        p.setId(1L);
        p.setStockStatus("OUT_OF_STOCK");
        assertFalse(
                new StockChangeService(products, logs, jdbc, cache)
                        .apply(p, new SourceParser.Snapshot("x", "IN_STOCK", LocalDateTime.now())));
        verifyNoInteractions(logs, jdbc, cache);
    }
}
