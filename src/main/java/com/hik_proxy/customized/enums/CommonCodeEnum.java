package com.hik_proxy.customized.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonCodeEnum {
    SUCCESS("70","操作成功"),
    FAILURE("90","操作失败"),
        ;
    private String code;
    private String name;

}
