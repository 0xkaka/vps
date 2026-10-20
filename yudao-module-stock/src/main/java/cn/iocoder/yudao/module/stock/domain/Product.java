package cn.iocoder.yudao.module.stock.domain;

import jakarta.validation.constraints.*;

/**
 * Product 数据对象。
 *
 * @author vps
 */
public class Product {
    private Long id;

    /** 原站稳定匹配键，管理链接更改不影响同步。 */
    @Size(max = 64)
    private String sourceKey;

    private java.time.LocalDateTime lastSyncedAt;

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public java.time.LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(java.time.LocalDateTime value) {
        lastSyncedAt = value;
    }

    /** 产品名称。 */
    @NotBlank
    @Size(max = 255)
    private String name;

    /** 商家ID。 */
    @NotNull @Positive private Long vendorId;

    /** 上级分组ID。 */
    @Positive private Long parentId;

    /** 是否分组。 */
    @NotNull private Boolean groupFlag;

    /** 配置。 */
    @Size(max = 500)
    private String specification;

    /** 线路标签。 */
    @Size(max = 255)
    private String routeTags;

    /** 说明。 */
    @Size(max = 1000)
    private String description;

    /** 购买链接。 */
    @Size(max = 2000)
    private String purchaseUrl;

    /** 原站匹配地址。 */
    @Size(max = 2000)
    private String sourceUrl;

    /** 测速链接。 */
    @Size(max = 2000)
    private String speedUrl;

    /** 透镜链接。 */
    @Size(max = 2000)
    private String lookingGlassUrl;

    /** 测评链接。 */
    @Size(max = 2000)
    private String reviewUrl;

    /** 库存状态。 */
    @NotBlank
    @Pattern(regexp = "IN_STOCK|OUT_OF_STOCK|UNKNOWN")
    private String stockStatus;

    /** 检查间隔(秒)。 */
    @NotNull
    @Min(120)
    private Integer intervalSeconds;

    /** 原站检查时间。 */
    private java.time.LocalDateTime lastCheckedAt;

    /** 同步库存。 */
    @NotNull private Boolean monitorEnabled;

    /** 排序。 */
    @NotNull
    @Min(0)
    private Integer sortOrder;

    /** 展示。 */
    @NotNull private Boolean enabled;

    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getGroupFlag() {
        return groupFlag;
    }

    public void setGroupFlag(Boolean groupFlag) {
        this.groupFlag = groupFlag;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getRouteTags() {
        return routeTags;
    }

    public void setRouteTags(String routeTags) {
        this.routeTags = routeTags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurchaseUrl() {
        return purchaseUrl;
    }

    public void setPurchaseUrl(String purchaseUrl) {
        this.purchaseUrl = purchaseUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSpeedUrl() {
        return speedUrl;
    }

    public void setSpeedUrl(String speedUrl) {
        this.speedUrl = speedUrl;
    }

    public String getLookingGlassUrl() {
        return lookingGlassUrl;
    }

    public void setLookingGlassUrl(String lookingGlassUrl) {
        this.lookingGlassUrl = lookingGlassUrl;
    }

    public String getReviewUrl() {
        return reviewUrl;
    }

    public void setReviewUrl(String reviewUrl) {
        this.reviewUrl = reviewUrl;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public java.time.LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(java.time.LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Boolean getMonitorEnabled() {
        return monitorEnabled;
    }

    public void setMonitorEnabled(Boolean monitorEnabled) {
        this.monitorEnabled = monitorEnabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public java.time.LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.time.LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public java.time.LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.time.LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
