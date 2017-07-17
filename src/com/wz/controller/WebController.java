package com.wz.controller;

import com.wz.annotation.Autowired;
import com.wz.annotation.Controller;
import com.wz.annotation.RequestMapping;
import com.wz.annotation.RequestParam;
import com.wz.service.ModifyService;
import com.wz.service.QueryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wz on 2017-07-17.
 */
@Controller
@RequestMapping("/web")
public class WebController {
    @Autowired
    private QueryService queryService;
    @Autowired("modify")
    private ModifyService modifyService;

    @RequestMapping("/search")
    public void search(@RequestParam("name") String name, HttpServletRequest request, HttpServletResponse response) {
        out(response, queryService.search(name));
    }

    @RequestMapping("/add")
    public void add(@RequestParam("name") String name,
                    @RequestParam("age") Integer age,
                    HttpServletRequest request, HttpServletResponse response) {
        out(response, modifyService.add(name, age));
    }

    @RequestMapping("/update")
    public void update(String name, HttpServletResponse response) {
        out(response, modifyService.update(name));
    }

    private void out(HttpServletResponse response, String str) {
        try {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
