package com.hik_proxy.customized.vo;

import lombok.Data;

import java.util.List;

@Data
public class BaseVo<T> {
    private String code;
    private String msg;
    private T data;
    private List<T> list;

    public boolean isSuccess() {
        return "0".equals(code);
    }
}
