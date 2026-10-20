# 部署与维护

## 服务管理

```sh
systemctl status vps-stock
journalctl -u vps-stock -n 100 --no-pager
systemctl restart vps-stock
nginx -t && systemctl reload nginx
```

生产 JVM 堆上限 768 MB；MySQL 连接池上限 8，Redis 上限 128 MB。Redis 使用 noeviction 防止令牌、确认链接和锁被随机淘汰，应监控使用量。Java 日志保留 14 天并压缩，总上限 200 MB；库存检查和投递记录保留 30 天，后台访问日志保留 14 天。未匹配产品不被误标为无货；详情在“库存检查记录”。

站点仅用 IP 访问时没有可信 HTTPS；请勿把管理员账号分发给不可信人员。绑定域名后应配置 TLS、更新 `VPS_PUBLIC_URL`、Nginx server_name，并重启服务。浏览器通知需 HTTPS；iOS 还需将网站添加到主屏幕。

## 邮件设置

通过 SSH 编辑 `/etc/vps-stock.env`，增加以下变量，再重启 `vps-stock`：

```ini
VPS_SMTP_HOST=smtp.example.com
VPS_SMTP_PORT=587
VPS_SMTP_USERNAME=your-account
VPS_SMTP_PASSWORD=your-smtp-app-password
VPS_SMTP_FROM=your-account@example.com
VPS_SMTP_SSL=false
```

587 使用强制 STARTTLS；465 设置 `VPS_SMTP_SSL=true`。邮件服务需由你提供有效授权码，并完成发信域名的必要设置。凭据不要复制到 Git。后台“通知与运行状态”展示是否配置和投递结果。订阅者必须在邮件中确认；运营人员也可以在后台停用订阅。

VAPID 密钥已在首次初始化时随机生成，保存在环境文件中。换钥会导致已有浏览器订阅失效，请勿随意重新生成。Web Push 接受标准 Chrome、Firefox、Safari、Windows 浏览器推送端点，拒绝任意内网地址，HTTP 重定向被禁用。

通知采用队列与有限重试；若进程在远端已收到消息但本地尚未记账时中断，恢复后可能重复发送。24 小时前未发出的补货提醒会过期，避免发送过时库存信息。

## 更新

1. 备份数据库及环境文件到其他设备。20 GB 磁盘不宜长期保存多份完整备份。
2. 构建并测试新版本；上传新 JAR 到 `/opt/vps/app.jar.new`。
3. `systemctl stop vps-stock`，保留一份旧 JAR 后原子替换运行包，再启动服务。
4. 上传 `public` 和 `admin-ui/dist-prod` 的构建产物，确认 API、后台登录、菜单与前台读取正常。
5. 若有表结构变更，编写增量迁移。**不要在已有数据的环境重新导入 bootstrap.sql**，其包含建表初始化语句。

首次部署的 `bootstrap.sh` 会主动拒绝已有 `vps_stock` 数据库。后台初始密码为随机值，框架限制长度为 4–16 位；首次登录后可在个人中心修改。

## 数据说明

原站不是本项目拥有的库存 API。同步使用固定公开页面，保留原站的检查时间，不能保证商家结算时仍有货。新增本地产品可以关闭“同步库存”进行手动维护；如要匹配参考站，提供原站 `data-url` 和 `data-purchase`，创建时自动计算匹配键。后续编辑推广链接不改变已保存的匹配键。

“机房库存”包含原站机房标识和展示名称；同步按产品匹配键与原站机房标识更新状态。可修改显示名称和购买链接、停用展示；删除仍由原站监控的机房记录后，下次同步会重新建立，若需隐藏请关闭展示。
