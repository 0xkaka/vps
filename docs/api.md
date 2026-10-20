# 接口与数据

管理接口使用 `Authorization: Bearer <accessToken>`；登录 `/admin-api/system/auth/login`。统一返回 `{code,msg,data}`，`code=0` 表示成功。

## 公共接口

- `GET /app-api/stock/catalog`：`vendors/products/regions/channels/links/settings`。只返回启用商家、启用产品与所属可见分组的数据，不公开邮箱、推送端点或凭据。
- `GET /app-api/stock/runtime`：最近同步时间、累计成功检查次数。
- `GET /app-api/stock/capabilities`：邮件和 Web Push 配置状态、VAPID 公钥。
- `POST /app-api/stock/email/subscribe`、`/email/unsubscribe`：`{email}`，发送确认邮件。
- `POST /app-api/stock/email/confirm`：`{token}`，确认订阅或退订；24 小时验证链接，退订通知链接为 30 天。
- `POST /app-api/stock/webpush/subscribe`、`/webpush/unsubscribe`：`{endpoint,keys:{p256dh,auth}}`，兼容浏览器 PushSubscription JSON。

订阅请求按访问 IP 限流，邮件请求另按邮箱限制。敏感订阅请求体不记录到访问日志。

## 管理接口

以下资源位于 `/admin-api/stock/{resource}`：

| resource | 用途 |
|---|---|
| vendor | 商家分类 |
| product | 产品及一级分组 |
| region | 产品机房库存 |
| channel | 订阅渠道链接，可关联产品或作为全局渠道 |
| link | 导航/产品目录/页脚链接 |
| setting | 标题、描述、公告、刷新间隔、页脚 |
| subscriber | 邮件订阅者和状态 |
| checklog | 库存检查记录，只读 |

`GET /page?name=&pageNo=1&pageSize=20` 返回 `{list,total}`；`POST /save` 创建/更新（更新携带 id）；`DELETE /delete?id=...` 删除。分页参数范围受校验。权限分别为 `stock:{resource}:query` 与 `stock:{resource}:edit`。

`POST /admin-api/stock/sync` 强制执行一次参考站同步，至少间隔 120 秒，需 `stock:product:edit`。

`GET /admin-api/stock/notification/status` 返回配置状态、最近 100 条投递和浏览器订阅记录。`POST /notification/retry?id=&type=email|push` 重试失败记录；`DELETE /notification/subscription?id=` 停用浏览器订阅。

## 关键字段

- Product：`vendorId/parentId/groupFlag` 控制分组；禁止自引用、跨商家分组、嵌套分组。
- `specification/routeTags/description` 分别管理配置、线路、说明。
- `purchaseUrl/speedUrl/lookingGlassUrl/reviewUrl` 为独立链接，限制 HTTP/HTTPS。
- `stockStatus`：`IN_STOCK / OUT_OF_STOCK / UNKNOWN`。
- `intervalSeconds` 最少 120 秒；`monitorEnabled` 控制原站同步；`lastCheckedAt` 为原站检查时间，`lastSyncedAt` 为本系统同步时间。
- `sourceKey` 是来源地址、原始购买地址和是否分组的 SHA-256；原站 URL 变化后需重新匹配。
- Region：`sourceName` 用于同步匹配，`name` 用于显示，`stockStatus` 和 `purchaseUrl` 独立管理。
- 时间由框架 JSON 序列化为毫秒时间戳；数据库和 Java 运行时使用 Asia/Shanghai。

数据库采用 `utf8mb4`；业务表有主键、索引、创建/更新时间和注释。商家/分组被引用时禁止删除；删除产品时级联删除机房与渠道。变更日志与投递记录按保留期限清理。
