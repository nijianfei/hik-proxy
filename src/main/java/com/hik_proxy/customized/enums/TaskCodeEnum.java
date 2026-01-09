package com.hik_proxy.customized.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskCodeEnum {
    T10("10","下载未开始"),
    T11("11","预下载开始"),
    T20("20","下载进行中"),
    T70("70","下载成功"),
    T80("80","下载警告"),
    T90("90","下载失败"),
    ;
    private String code;
    private String name;
}
