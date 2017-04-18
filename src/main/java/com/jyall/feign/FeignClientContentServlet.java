package com.jyall.feign;


import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@WebServlet(value = "/api", name = "api")
public class FeignClientContentServlet extends HttpServlet {
	private static final long serialVersionUID = 8189039516331722747L;
	private static final Logger logger = LoggerFactory.getLogger(FeignClientContentServlet.class);
    @Value("${spring.application.name:}")
    private String serviceId;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(FeignClientContentUtil.getFeignClientContent(serviceId));
        } catch (IOException e) {
            logger.error("", e);
        }
    }
}
