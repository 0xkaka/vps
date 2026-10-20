# 商家库存来源适配

核验日期：2026-10-21（北京时间）。在部署服务器运行只读全量检查，不以搜索结果或 HTTP 200 直接判断有货。

## 已验证来源与覆盖

| 商家 | 数据入口 | 首轮直连成功 / 可识别条目 | 限制 |
|---|---|---:|---|
| 搬瓦工 | https://bandwagonhost.com/order/catalog.json | 41 / 72 | 官方公开 JSON；特价、邀请码和历史套餐不一定在目录中，缺项不等于无货 |
| RackNerd | https://my.racknerd.com/cart.php?a=add&pid=产品编号 | 18 / 18 | 官方 WHMCS 购买页；包含“其他”分类中的 1 条产品 |
| ColoCrossing | https://cloud.colocrossing.com/cart.php?a=add&pid=产品编号 | 10 / 10 | 官方 WHMCS 购买页；包含“其他”分类中的产品 |
| ZGO | https://clients.zgovps.com/index.php?/cart/special-offer/ 及官网其余 12 个分类页 | 3 / 12 | 9 个历史产品编号未在现售分类目录找到 |
| V.PS | https://vps.hosting/?cmd=cart&action=add&id=产品编号 | 8 / 8 | 官方 HostBill 产品卡片，按编号匹配 |
| VMRack | https://www.vmrack.net/zh-CN/pricing | 2 / 5 | 读取官方页面内 JSON；3 个旧活动套餐未收录 |
| DMIT | https://www.dmit.io/cart.php?a=add&pid=产品编号 | 0 / 97 | 服务器返回 HTTP 403；定价页也受访问限制 |
| Vmiss | https://app.vmiss.com/cart.php?a=add&pid=产品编号 | 0 / 25 | HTTP 403 / 浏览器验证 |
| GoMami | https://gomami.io/cart.php?a=add&pid=产品编号 | 0 / 4 | HTTP 403 / 浏览器验证；已查到的官方 API 文档为账户和实例管理，不能据此推断在售库存权限 |
| Dedirock | https://billing.dedirock.com/cart.php?a=add&pid=产品编号 | 0 / 3 | HTTP 403 / 浏览器验证 |

合计：254 条可识别产品，首轮 82 条直连成功，耗时 20 秒。11 个前台分类实际包含 10 个具名商家，“其他”并非独立商家。分组、系列入口、抽奖、缺少产品编号的条目不伪装成可独立监控的 SKU。覆盖数字是该次验证结果，不保证未来每次请求成功。

## 判断规则

- 搬瓦工：`schema_version=1`、产品编号、机房和布尔值 `available` 必须有效。合并同机房的付款周期；名称明确指定机房时只读取该机房。价格币种和周期不混用，本次不自动覆盖人工维护的产品名称、描述和参考价。
- WHMCS：明确的缺货标题表示无货；有效的产品配置表单表示可继续配置。登录、验证码、通用目录、空响应、错误页均不能推断库存。
- ZGO：只匹配表单里的产品编号，明确禁用的缺货按钮表示无货；带付款周期的正常 Continue 按钮表示可订购。
- V.PS：按 `.cart-product[data-value]` 匹配编号，读取 `outofstock` 状态。
- VMRack：解析 `__NUXT_DATA__` 引用数组，匹配套餐 ID。以官网可见标签核对：1 正常、2 库存紧张、3 售罄；未知枚举报错。套餐详情接口 `/v1/product/set/compute/activity?id=...` 虽可读取，但实测对已售罄产品也返回 `stock_status=0`，因此**不将其作为库存证据**。

## 同步和降级

Java 定时任务按 120 秒固定周期触发，单轮超时不并发执行；两分钟产品每轮检查，更长间隔产品仍按自身配置检查。4 个固定工作线程按商家分组查询，同一商家串行、同轮同 URL 复用响应。官方查询总预算 90 秒；单次请求超时 10 秒，最多 5 次同域跳转。HTTP 401/403/429 或验证页退避 15 分钟；其他网络错误退避 2 分钟。退避期间继续查询原有备用来源。

官方成功 → 使用官方库存；官方不可用/缺项 → 尝试原有聚合来源；两者均失败 → 保留上次状态、时间和来源，同时记录异常。失败不是无货。首次接入、官方与备用来源切换时静默建立基线；同一来源后续明确从无货变有货才进入通知队列。

产品 API 和管理列表显示 `lastSourceType`、`lastSourceUrl`、`lastSyncError`、`lastAttemptAt`。类型分别为 `OFFICIAL_API`、`OFFICIAL_PAGE`、`THIRD_PARTY_API`、`THIRD_PARTY_PAGE`。前台显示实际来源，整体失败时注明“显示上次结果”。官方成功后替换采集生成的机房条目，避免混用旧聚合机房状态，保留 `source_name IS NULL` 的手工机房。

## 尚未完成的外部条件

DMIT、Vmiss、GoMami、Dedirock 的商家直连需要商家解除该服务器的访问限制、提供适用的只读库存授权，或提供可核验的公开库存接口。程序没有绕过验证码，也没有将普通用户的实例管理 API 当作商品库存 API。后续已为部分产品接入下述星空 VPS 第三方 API；未匹配或未知的产品继续使用原有备用来源。

官方参考：
- [搬瓦工目录入口](https://bandwagonhost.com/order/)
- [WHMCS GetProducts（需商家 API 权限）](https://developers.whmcs.com/api-reference/getproducts/)
- [GoMami API Token 文档](https://docs.gomami.io/account/api-token/)

## 四家商家追加调查（2026-10-21）

此次官方接口调查检查了官方文档、官网实际订单链接以及浏览器网络请求；当时未获得新的可验证库存响应，因此未增加官方适配器。后续第三方接入见文末。以下结果是本次访问环境的观察，不代表接口永远不存在。

| 商家 | 新核验入口 | 结果与下一步条件 |
|---|---|---|
| DMIT | https://docs.dmit.io/guide/getting-started 、 https://www.dmit.io/pages/pricing | 官方文档可读，目前导航主要为实例、SSH 密钥、账户和计费指南，未发现公开库存 API 文档；定价页浏览器请求仍为 Cloudflare 拦截页。需可访问的官方库存入口或商家授权。 |
| Vmiss | https://www.vmiss.com/ 、 https://app.vmiss.com/ | 营销官网可读，观察到的请求未暴露库存 JSON；客户区仍显示浏览器验证。WordPress 内容接口不属于商品库存接口。 |
| GoMami | https://docs.gomami.io/account/api-token/ → https://cp.gomami.io/account/api | 官方 API Reference 跳转登录；公开说明使用 Bearer Token，列出的能力为服务器、SSH、VNC、ISO 和任务管理。尚未证实在售产品或库存查询能力；不能仅凭存在 Token 就接入库存。 |
| Dedirock | https://billing.dedirock.com/index.php/store/vps-storage/storage-starter | 官网实际链接发现 `/index.php/store/...` 订单入口，但浏览器访问仍为验证页；未取得当前目录产品与现有 PID 265、197、199 的可靠映射，不按相似名称替换。 |

GoMami 的公开实例 API 示例包含 `/api/server/{id}/build/templates` 等已购服务器操作路径，未执行任何修改实例的请求。其文档说明 Token 具有完整账户权限；继续调查优先取得登录后的 API 文档或仅库存授权，而不是收集账户全权限 Token。

WHMCS 官方 [GetProducts 文档](https://developers.whmcs.com/api-reference/getproducts/) 中的 `quantity_available` 从 8.12 加入，调用需要商家侧 API 凭据。通用文档不能证明某家已经开放该权限、使用相应版本或提供完整机房库存；在取得真实响应并核验枚举前，不实现猜测性判断。

## 部署与验证

现有库先备份，执行 `sql/mysql/migrations/20261021_merchant_sources.sql` 一次，再替换服务 JAR。新库初始化使用已包含新字段的 `sql/mysql/vps.sql`，不要重复执行该迁移。Nginx 后台接口等待时间和管理端手动同步等待时间为 180 秒。

JUnit 覆盖域名伪装、系列入口误匹配、缺项/未知字段、机房范围、HostBill 编号匹配、验证页/登录页、Nuxt 引用和库存枚举，以及切换来源不误发补货通知。

生产 JDBC 会话时区与 Java 的 Asia/Shanghai 对齐，避免新写入的同步时间显示早 8 小时。历史库存检查时间仍按各来源原值保留。

## 星空 VPS 第三方 API（2026-10-21 接入）

固定请求 `https://spacevps.cc/api/data.json`，每轮只读取一次，覆盖 VMISS、DMIT、GoMami、Dedirock。对这四家，有效且匹配的 API 快照优先，其次官方来源，最后原有聚合页面。未改写商品购买链接或引入上游推广参数。

匹配必须同时满足来源商家 slug、受信商家域名和相同产品 PID。名称相似、只有新套餐 slug 的记录不能替代旧 PID。重复 PID 库存冲突、未知枚举、缺项全部回退。Dedirock 当前上游库存全为 unknown，不能视为已获得有效库存。

使用 `stockUpdatedAt` 记录上游全局库存快照时间；不冒充逐款商家检查时间。前台显示“上游快照”和“本站同步”，来源链接注明“第三方 API · 星空VPS”。快照超过 10 分钟、未来超过 2 分钟、缺失或无法解析时拒绝整份快照。此时回退原有来源；所有来源失败则保留库存并标记更新异常。每两分钟读取不意味着上游每款商品都有两分钟内的真实观测。

首次启用/切换来源静默建立基线，避免口径差异触发补货通知。单次请求超时 15 秒、响应最大 8 MiB、禁止跟随重定向。使用规则见 [星空 VPS API 文档](https://spacevps.cc/api)。

部署前服务器只读试跑：现有商品按 PID 成功匹配 80 条（VMISS 25、DMIT 53、GoMami 2）。上游目录总数不等于本站匹配数，未自动新增或替换旧型号。5 项新快照适配测试与原 21 项测试全部通过。
