package com.jyall.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/***
 *
 * @author sheng.xiandong
 * 对request、response、session进行统一访问接口封装
 */
public class ContextWrapper {
    private ContextWrapper() {
    }
    /**
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest(){
        return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     *
     * @return HttpServletResponse
     */
    public static HttpServletResponse getResponse(){
        return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
    }

    /**
     *
     * @return String
     */
    public static String getSessionId(){
        return RequestContextHolder.getRequestAttributes().getSessionId();
    }

    /**
     *
     * @return HttpSession
     */
    public static HttpSession getSession(){
        HttpServletRequest request = getRequest();
        return request.getSession();
    }

    /**
     *
     * @param name
     * @return Object
     */
    public static Object getSessionAttribute(String name){
        HttpSession session = getSession();
        return session.getAttribute(name);
    }

    /**
     *
     * @param name
     * @return Object
     */
    public static Object getRequestAttribute(String name){
        HttpServletRequest request = getRequest();
        return request.getAttribute(name);
    }
}
