package cn.iocoder.yudao.module.stock.domain;

import jakarta.validation.constraints.*;

/**
 * Channel 数据对象。
 *
 * @author vps
 */
public class Channel {
    private Long id;

    /** 渠道名称。 */
    @NotBlank
    @Size(max = 120)
    private String name;

    /** 渠道类型。 */
    @NotBlank
    @Pattern(regexp = "TELEGRAM|BARK|NTFY|WXPUSHER|WECHAT|DISCORD|OTHER")
    private String type;

    /** 产品ID(空为全局)。 */
    @Positive private Long productId;

    /** 订阅链接。 */
    @NotBlank
    @Size(max = 2000)
    private String url;

    /** 排序。 */
    @NotNull
    @Min(0)
    private Integer sortOrder;

    /** 启用。 */
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
