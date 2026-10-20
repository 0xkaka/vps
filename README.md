# VPS 库存运营平台

基于芋道 `yudao-boot-mini`（Java 17 / Spring Boot 3）与官方 Vue 3 管理前端，使用 MySQL 8、Redis 和 Nginx。保留上游 MIT 许可证。业务代码位于 `yudao-module-stock`，前台静态资源位于 `public`，后台位于 `admin-ui`。

## 功能

- 公共展示：商家筛选、产品分组、搜索、规格与线路、产品及机房库存、检查时间、购买/测速/透镜/测评链接、订阅渠道、深浅主题、自动刷新。
- 运营管理：商家、产品、机房库存、订阅渠道、导航链接、站点配置、邮件订阅者、检查日志、浏览器订阅与投递记录。
- 账户权限：使用芋道原生登录、用户、角色、菜单和接口权限；自定义业务请求有参数校验、关联约束、事务和访问日志。
- 同步：每两分钟查看到期产品，读取综合首页、搬瓦工/DMIT 独立库存页和两个产品大全页的公开 HTML，按产品设置的检查周期执行。只同步库存和原站检查时间，保留管理员编辑的文案、链接、排序和展示开关。
- 通知：从无货变成有货时创建邮件/Web Push 队列；邮件双重确认、退订确认、有限重试、无效浏览器订阅停用、24 小时后过期。

当前资料：261 个非分组条目、17 个分组（合计 278 条）、172 条机房库存、234 个订阅链接、9 条导航/目录链接。外部渠道链接来自参考站，第三方平台的实际订阅者和凭据不属于本项目，未尝试读取这些私有数据。

## 当前部署

- 展示：`http://68.68.249.248/`
- 后台：`http://68.68.249.248/admin/`
- Java：仅监听 `127.0.0.1:48080`，MySQL/Redis 仅在本机访问。
- 服务：`vps-stock.service`；代码 `/opt/vps/source`；运行包 `/opt/vps/app.jar`；前端 `/opt/vps/www`。
- 凭据：只保存在服务器 `/etc/vps-stock.env` 和 `/etc/vps-stock-access.json`，不进入 Git。

邮件功能需配置自己的 SMTP；公网浏览器后台推送需可信 HTTPS。没有配置时，展示页明确说明不可用，不会提示虚假的订阅成功。暂未对真实收件人或第三方推送服务进行发送测试。

## 构建

```sh
mvn -B -ntp verify
cd admin-ui
pnpm install --frozen-lockfile
pnpm exec vite build --mode prod
```

环境：JDK 17、Maven 3.8+、Node 22、pnpm 10.15.1。GitHub Actions 执行后端测试和前端构建，不自动部署。

首次空库初始化见 `deploy/bootstrap.sh`，脚本检测已有数据库时拒绝覆盖。生产配置模板为 `deploy/application-prod.yaml`；数据库导入顺序为 `bootstrap.sql`、`vps.sql`、`vps-region.sql`、`vps-seed.sql`、`vps-region-seed.sql`、`vps-extended-seed.sql`。

## 开发约定

新增业务按阿里巴巴 Java 开发手册的分层、命名、参数校验、SQL 参数化、显式事务回滚、异常处理、密钥隔离与时间格式约定组织；Java 使用四空格格式化。这里不声称上游全部代码已通过 P3C 全量扫描。

详细说明：[部署运维](docs/operations.md)、[接口与数据结构](docs/api.md)。

## 上游来源

- 后端：<https://gitee.com/yudaocode/yudao-boot-mini>，`master-jdk17`，初始化提交 `c533150ad5491a1f772951fd81683b1e23ebaac1`。
- 框架主项目：<https://github.com/YunaiV/ruoyi-vue-pro>。
- 管理前端：<https://github.com/yudaocode/yudao-ui-admin-vue3>。保留登录、权限、系统管理和基础设施，移除未启用业务的前端页面。
- 公开页面参考：<https://stock.vpszdm.com/>，2026-10-20。

扩展来源的去重规则、导入与限制见 [多来源整合](docs/multi-source.md)。
