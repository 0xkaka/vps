package cn.iocoder.yudao.module.stock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/** SMTP 密钥仅从部署环境注入。 @author vps */
@Component
public class MailGateway {
    private final JavaMailSenderImpl sender = new JavaMailSenderImpl();
    private final String from;

    public MailGateway(
            @Value("${stock.smtp.host:}") String host,
            @Value("${stock.smtp.port:587}") int port,
            @Value("${stock.smtp.username:}") String username,
            @Value("${stock.smtp.password:}") String password,
            @Value("${stock.smtp.from:}") String from,
            @Value("${stock.smtp.ssl:false}") boolean ssl) {
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        this.from = from;
        var properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", String.valueOf(!ssl));
        properties.put("mail.smtp.starttls.required", String.valueOf(!ssl));
        properties.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        properties.put("mail.smtp.ssl.checkserveridentity", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
    }

    public boolean available() {
        return sender.getHost() != null && !sender.getHost().isBlank() && !from.isBlank();
    }

    public void send(String to, String subject, String content) {
        if (!available()) {
            throw new IllegalStateException("SMTP未配置");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        sender.send(message);
    }
}
