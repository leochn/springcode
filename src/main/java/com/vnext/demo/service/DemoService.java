package com.vnext.demo.service;

import com.vnext.mvcframework.annotation.MyService;

/**
 * @author leo
 * @version 2018/5/26 16:36
 * @since 1.0.0
 */
@MyService
public class DemoService {

    private String str = "hello";

    public String get(String name){
        return "My name is " + name;
    }

}