package com.wz.mvc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wz on 2017-07-17.
 */
public class DispatcherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        out(resp, "Received request!");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("init ...");
    }

    private void out(HttpServletResponse response, String str){
        try{
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(str);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
