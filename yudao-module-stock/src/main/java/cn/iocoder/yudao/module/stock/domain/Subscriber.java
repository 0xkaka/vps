package cn.iocoder.yudao.module.stock.domain;

import jakarta.validation.constraints.*;

/**
 * Subscriber 数据对象。
 *
 * @author vps
 */
public class Subscriber {
    private Long id;

    /** 邮箱。 */
    @NotBlank
    @Email
    @Size(max = 254)
    private String name;

    /** 状态。 */
    @NotBlank
    @Pattern(regexp = "PENDING|ACTIVE|UNSUBSCRIBED")
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
