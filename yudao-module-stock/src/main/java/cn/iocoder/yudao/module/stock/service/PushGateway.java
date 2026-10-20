package cn.iocoder.yudao.module.stock.service;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.*;

/** Web Push 只允许标准浏览器厂商端点。 @author vps */
@Component
public class PushGateway {
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public PushGateway(
            @Value("${stock.push.public-key:}") String publicKey,
            @Value("${stock.push.private-key:}") String privateKey,
            @Value("${stock.public-url:http://68.68.249.248}") String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        Security.addProvider(new BouncyCastleProvider());
    }

    public boolean available() {
        return !publicKey.isBlank() && !privateKey.isBlank();
    }

    public String getPublicKey() {
        return publicKey;
    }

    public static void validate(String endpoint, String key, String auth) {
        URI uri = URI.create(endpoint);
        String host = uri.getHost();
        boolean allowed =
                host != null
                        && (host.equals("fcm.googleapis.com")
                                || host.equals("updates.push.services.mozilla.com")
                                || host.equals("web.push.apple.com")
                                || host.endsWith(".notify.windows.com"));
        if (!"https".equals(uri.getScheme())
                || !allowed
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("不支持的浏览器推送端点");
        }
        if (Base64.getUrlDecoder().decode(key).length != 65
                || Base64.getUrlDecoder().decode(auth).length != 16) {
            throw new IllegalArgumentException("无效订阅密钥");
        }
    }

    public int send(String endpoint, String key, String auth, String payload) throws Exception {
        validate(endpoint, key, auth);
        PushService service = new PushService(publicKey, privateKey, subject);
        RequestConfig config =
                RequestConfig.custom()
                        .setConnectTimeout(10000)
                        .setSocketTimeout(10000)
                        .setConnectionRequestTimeout(10000)
                        .setRedirectsEnabled(false)
                        .build();
        try (var client =
                        HttpClients.custom()
                                .setDefaultRequestConfig(config)
                                .disableRedirectHandling()
                                .build();
                var response =
                        client.execute(
                                service.preparePost(
                                        new Notification(
                                                endpoint,
                                                key,
                                                auth,
                                                payload.getBytes(StandardCharsets.UTF_8),
                                                3600),
                                        Encoding.AES128GCM))) {
            return response.getStatusLine().getStatusCode();
        }
    }
}
