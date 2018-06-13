package com.vnext.demo.controller;

import com.vnext.mvcframework.annotation.MyAutowired;
import com.vnext.mvcframework.annotation.MyController;
import com.vnext.mvcframework.annotation.MyRequestMapping;
import com.vnext.mvcframework.annotation.MyRequestParam;
import com.vnext.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author leo
 * @version 2018/5/26 16:35
 * @since 1.0.0
 */
@MyController
@MyRequestMapping("/demo")
public class DemoController {

    @MyAutowired
    private DemoService demoService;

    private String pName;

    @MyRequestMapping("/query.json")
    public void query(HttpServletRequest request , HttpServletResponse response,
                      @MyRequestParam("name") String name){
        System.out.println("query.json..........................");
        String result = demoService.get(name);

        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @MyRequestMapping("/edit.json")
    public void edit(HttpServletRequest request , HttpServletResponse response,
                      @MyRequestParam("id") Integer id){

    }

    @MyRequestMapping("/remove.json")
    public void remove(HttpServletRequest request , HttpServletResponse response,
                       @MyRequestParam("id") Integer id){

    }




















}