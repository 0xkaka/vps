package cn.iocoder.yudao.module.stock.domain;

import jakarta.validation.constraints.*;

/**
 * Link 数据对象。
 *
 * @author vps
 */
public class Link {
    private Long id;

    /** 链接名称。 */
    @NotBlank
    @Size(max = 120)
    private String name;

    /** 链接地址。 */
    @NotBlank
    @Size(max = 2000)
    private String url;

    /** 位置。 */
    @NotBlank
    @Pattern(regexp = "HEADER|PRODUCT|FOOTER")
    private String position;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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
