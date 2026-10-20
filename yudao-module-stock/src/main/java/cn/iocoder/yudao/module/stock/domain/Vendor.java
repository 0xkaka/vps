package cn.iocoder.yudao.module.stock.domain;

import jakarta.validation.constraints.*;

/**
 * Vendor 数据对象。
 *
 * @author vps
 */
public class Vendor {
    private Long id;

    /** 商家名称。 */
    @NotBlank
    @Size(max = 120)
    private String name;

    /** 商家标识。 */
    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9_-]{1,60}")
    private String code;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
