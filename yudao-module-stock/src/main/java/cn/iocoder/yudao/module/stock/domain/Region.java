package cn.iocoder.yudao.module.stock.domain;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 产品的机房库存明细。 @author vps */
@Data
public class Region {
    private Long id;
    @NotNull @Positive private Long productId;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 120)
    private String sourceName;

    @NotBlank
    @Pattern(regexp = "IN_STOCK|OUT_OF_STOCK|UNKNOWN")
    private String stockStatus;

    @NotBlank
    @Size(max = 2000)
    private String purchaseUrl;

    @NotNull
    @Min(0)
    private Integer sortOrder;

    @NotNull private Boolean enabled;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
}
