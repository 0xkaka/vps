package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.module.stock.domain.Product;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** 商家优先、按域名限流、失败退避；仅发送公开只读请求。 @author vps */
@Service
public class MerchantSourceService {
    public record Result(SourceParser.Snapshot snapshot, String kind, String url, String error) {}

    private final ObjectMapper json;
    private final StringRedisTemplate redis;
    private final ThreadPoolExecutor workers =
            new ThreadPoolExecutor(
                    4,
                    4,
                    30,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(16),
                    r -> {
                        Thread t = new Thread(r, "merchant-stock-reader");
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.AbortPolicy());
    private static final Set<String> HOSTS =
            Set.of(
                    "bandwagonhost.com",
                    "www.dmit.io",
                    "my.racknerd.com",
                    "cloud.colocrossing.com",
                    "clients.zgovps.com",
                    "www.vmrack.net",
                    "app.vmiss.com",
                    "gomami.io",
                    "vps.hosting",
                    "billing.dedirock.com");
    private static final List<String> ZGO_CATEGORIES =
            List.of(
                    "special-offer",
                    "hongkong-amd-vps",
                    "tokyo-intel-vps",
                    "de-frankfurt-amd-vps",
                    "los-angeles-amd-optimised-vps",
                    "los-angeles-isp-vps",
                    "los-angeles-amd-intel-vps",
                    "los-angeles-intel-performance-vps",
                    "los-angeles-ryzen9-performance-vps",
                    "los-angeles-global-vps",
                    "los-angeles-amd-vds",
                    "osaka-amd-performance-vps",
                    "osaka-amd-ryzen9-performance-vps");

    public MerchantSourceService(ObjectMapper json, StringRedisTemplate redis) {
        this.json = json;
        this.redis = redis;
    }

    @PreDestroy
    public void close() {
        workers.shutdownNow();
    }

    public Map<Long, Result> read(List<Product> products) {
        Map<String, List<Product>> groups = new LinkedHashMap<>();
        for (Product p : products) {
            var target = MerchantParser.target(p);
            if (target != null)
                groups.computeIfAbsent(target.merchant(), k -> new ArrayList<>()).add(p);
        }
        Map<Long, Result> results = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        groups.forEach(
                (merchant, entries) ->
                        futures.add(
                                workers.submit(
                                        () -> readMerchant(merchant, entries, results, deadline))));
        for (Future<?> future : futures) {
            try {
                future.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.cancel(true);
            } catch (ExecutionException | TimeoutException e) {
                future.cancel(true);
            }
        }
        // 返回快照副本，超时工作线程不能在主同步途中改写结果。
        Map<Long, Result> finished = new HashMap<>(results);
        groups.values()
                .forEach(
                        list ->
                                list.forEach(
                                        p ->
                                                finished.putIfAbsent(
                                                        p.getId(),
                                                        new Result(
                                                                null,
                                                                null,
                                                                MerchantParser.target(p).url(),
                                                                "商家查询超时，本轮使用备用来源"))));
        return finished;
    }

    private void readMerchant(
            String merchant, List<Product> entries, Map<Long, Result> results, long deadline) {
        Map<String, String> bodies = new HashMap<>();
        String backoffKey = "vps:merchant:backoff:" + merchant;
        String blocked = redis.opsForValue().get(backoffKey);
        Map<String, Document> zgoPages = new LinkedHashMap<>();
        for (Product p : entries) {
            var target = MerchantParser.target(p);
            if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) return;
            if (blocked != null) {
                results.put(p.getId(), new Result(null, null, target.url(), blocked));
                continue;
            }
            try {
                SourceParser.Snapshot snapshot;
                String sourceUrl = target.url();
                if ("zgo".equals(merchant)) {
                    if (zgoPages.isEmpty())
                        for (String category : ZGO_CATEGORIES) {
                            if (System.nanoTime() > deadline)
                                throw new IllegalStateException("分类查询超时");
                            String url =
                                    "https://clients.zgovps.com/index.php?/cart/" + category + "/";
                            zgoPages.put(url, Jsoup.parse(fetch(url), url));
                        }
                    snapshot = null;
                    for (var page : zgoPages.entrySet()) {
                        if (page.getValue()
                                        .selectFirst("input[name=id][value='" + target.pid() + "']")
                                == null) continue;
                        snapshot = MerchantParser.page(p, target, page.getValue());
                        sourceUrl = page.getKey();
                        break;
                    }
                    if (snapshot == null) throw new IllegalArgumentException("官方分类目录未收录该老套餐编号");
                } else {
                    String body = bodies.get(sourceUrl);
                    if (body == null) {
                        body = fetch(sourceUrl);
                        bodies.put(sourceUrl, body);
                    }
                    if ("bandwagon".equals(merchant))
                        snapshot = MerchantParser.bandwagon(p, target, json.readTree(body));
                    else if ("vmrack".equals(merchant)) {
                        var element = Jsoup.parse(body).selectFirst("script#__NUXT_DATA__");
                        if (element == null) throw new IllegalArgumentException("官网未提供库存数据");
                        snapshot = MerchantParser.vmrack(p, target, json.readTree(element.data()));
                    } else snapshot = MerchantParser.page(p, target, Jsoup.parse(body, sourceUrl));
                }
                results.put(p.getId(), new Result(snapshot, target.kind(), sourceUrl, null));
            } catch (MerchantAccessException e) {
                blocked = e.getMessage() + "，已退避15分钟";
                redis.opsForValue().set(backoffKey, blocked, Duration.ofMinutes(15));
                results.put(p.getId(), new Result(null, null, target.url(), blocked));
            } catch (Exception e) {
                String message =
                        e instanceof IllegalArgumentException
                                ? e.getMessage()
                                : "商家请求或解析失败：" + e.getClass().getSimpleName();
                results.put(p.getId(), new Result(null, null, target.url(), message));
                if (!(e instanceof IllegalArgumentException)) {
                    blocked = message + "，已退避2分钟";
                    redis.opsForValue().set(backoffKey, blocked, Duration.ofMinutes(2));
                }
            }
        }
    }

    /** 每次跳转均校验同一商家域名，避免后台 URL 或跳转引入 SSRF。 */
    static String fetch(String url) throws java.io.IOException {
        URI original = URI.create(url);
        validate(original);
        URI current = original;
        Map<String, String> cookies = new HashMap<>();
        for (int hop = 0; hop < 5; hop++) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.IOException("任务取消");
            validate(current);
            if (!original.getHost().equals(current.getHost()))
                throw new java.io.IOException("拒绝跨域跳转");
            Connection.Response response =
                    Jsoup.connect(current.toString())
                            .timeout(10000)
                            .maxBodySize(2 * 1024 * 1024)
                            .userAgent("Mozilla/5.0 (compatible; VPSStockMonitor/1.0)")
                            .cookies(cookies)
                            .followRedirects(false)
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .execute();
            cookies.putAll(response.cookies());
            int status = response.statusCode();
            if (status == 403 || status == 401 || status == 429)
                throw new MerchantAccessException("商家返回 HTTP " + status);
            if (status >= 300 && status < 400 && response.header("Location") != null) {
                current = current.resolve(response.header("Location"));
                continue;
            }
            if (status != 200) throw new java.io.IOException("商家响应异常");
            String body = response.body();
            if (body.contains("cf-chl-")
                    || body.contains("Just a moment...")
                    || body.contains("Sorry, you have been blocked"))
                throw new MerchantAccessException("商家访问验证，需白名单或授权");
            return body;
        }
        throw new java.io.IOException("重定向过多");
    }

    private static void validate(URI uri) {
        if (!"https".equals(uri.getScheme())
                || !HOSTS.contains(Objects.toString(uri.getHost(), ""))
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new IllegalArgumentException("商家地址不在白名单");
    }

    private static class MerchantAccessException extends java.io.IOException {
        MerchantAccessException(String message) {
            super(message);
        }
    }
}
