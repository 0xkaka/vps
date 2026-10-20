package cn.iocoder.yudao.module.stock.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.iocoder.yudao.module.stock.domain.Product;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class SpaceVpsSourceServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Instant now = Instant.parse("2026-10-21T00:00:00Z");

    private Product product() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Basic");
        p.setPurchaseUrl("https://app.vmiss.com/cart.php?a=add&pid=50");
        p.setSourceKey("sku");
        return p;
    }

    private com.fasterxml.jackson.databind.JsonNode data(String time, String plans)
            throws Exception {
        return mapper.readTree(
                "{\"stockUpdatedAt\":\""
                        + time
                        + "\",\"providers\":[{\"slug\":\"vmiss\",\"lines\":[{\"plans\":["
                        + plans
                        + "]}]}]}");
    }

    private String plan(String host, int pid, String stock) {
        return "{\"name\":\"Basic\",\"buyUrl\":\"https://"
                + host
                + "/aff.php?aff=9&pid="
                + pid
                + "\",\"stockStatus\":\""
                + stock
                + "\"}";
    }

    @Test
    void matchesExactMerchantPidAndKeepsUpstreamTime() throws Exception {
        var p = product();
        var result =
                SpaceVpsSourceService.parse(
                        data("2026-10-21T07:59:00+08:00", plan("app.vmiss.com", 50, "out")),
                        List.of(p),
                        now);
        assertEquals("OUT_OF_STOCK", result.get(1L).status());
        assertEquals("2026-10-21T07:59", result.get(1L).checked().toString());
        assertEquals("https://app.vmiss.com/cart.php?a=add&pid=50", p.getPurchaseUrl());
    }

    @Test
    void unknownMissingAndWrongMerchantNeverSupplyInventory() throws Exception {
        for (String plans :
                List.of(
                        plan("app.vmiss.com", 50, "unknown"),
                        plan("app.vmiss.com", 51, "in"),
                        plan("www.dmit.io", 50, "in"),
                        plan("app.vmiss.com.evil.example", 50, "in"),
                        "")) {
            assertTrue(
                    SpaceVpsSourceService.parse(
                                    data(now.toString(), plans), List.of(product()), now)
                            .isEmpty());
        }
    }

    @Test
    void conflictingDuplicatePidIsNotMatched() throws Exception {
        assertTrue(
                SpaceVpsSourceService.parse(
                                data(
                                        now.toString(),
                                        plan("app.vmiss.com", 50, "in")
                                                + ","
                                                + plan("app.vmiss.com", 50, "out")),
                                List.of(product()),
                                now)
                        .isEmpty());
    }

    @Test
    void staleFutureOrMissingTimestampIsRejected() {
        for (String time :
                List.of(now.minusSeconds(601).toString(), now.plusSeconds(121).toString(), "")) {
            assertThrows(
                    Exception.class,
                    () ->
                            SpaceVpsSourceService.parse(
                                    data(time, plan("app.vmiss.com", 50, "in")),
                                    List.of(product()),
                                    now));
        }
    }

    @Test
    void groupCannotAcquireIndividualProductStock() throws Exception {
        var p = product();
        p.setGroupFlag(true);
        assertTrue(
                SpaceVpsSourceService.parse(
                                data(now.toString(), plan("app.vmiss.com", 50, "in")),
                                List.of(p),
                                now)
                        .isEmpty());
    }
}
