package cn.iocoder.yudao.module.stock.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

class ExchangeRateServiceTest {
    private final ObjectMapper json = new ObjectMapper();

    private String snapshot(String date) {
        return "{\"amount\":1,\"base\":\"CNY\",\"date\":\""
                + date
                + "\",\"rates\":{\"USD\":0.125,\"EUR\":0.1,\"CAD\":0.2,\"BAD\":0}}";
    }

    @Test
    void convertsQuoteDirectionAndRetainsObservationDate() throws Exception {
        Map<String, Object> value =
                ExchangeRateService.parse(
                        json.readTree(snapshot("2026-10-20")), LocalDate.of(2026, 10, 21));
        Map<?, ?> rates = (Map<?, ?>) value.get("cnyPerUnit");
        assertEquals(8.0, rates.get("USD"));
        assertEquals(5.0, rates.get("CAD"));
        assertEquals(1.0, rates.get("CNY"));
        assertFalse(rates.containsKey("BAD"));
        assertEquals("2026-10-20", value.get("date"));
    }

    @Test
    void rejectsStaleFutureAndIncompleteSnapshots() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 21);
        assertThrows(
                IllegalArgumentException.class,
                () -> ExchangeRateService.parse(json.readTree(snapshot("2026-09-01")), today));
        assertThrows(
                IllegalArgumentException.class,
                () -> ExchangeRateService.parse(json.readTree(snapshot("2026-10-25")), today));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ExchangeRateService.parse(
                                json.readTree(snapshot("2026-10-20").replace("CAD", "XXX")),
                                today));
    }
}
