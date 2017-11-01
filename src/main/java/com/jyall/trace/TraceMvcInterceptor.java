/*
                            _ooOoo_  
                           o8888888o  
                           88" . "88  
                           (| -_- |)  
                            O\ = /O  
                        ____/`---'\____  
                      .   ' \\| |// `.  
                       / \\||| : |||// \  
                     / _||||| -:- |||||- \  
                       | | \\\ - /// | |  
                     | \_| ''\---/'' | |  
                      \ .-\__ `-` ___/-. /  
                   ___`. .' /--.--\ `. . __  
                ."" '< `.___\_<|>_/___.' >'"".  
               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
                 \ \ `-. \_ __\ /__ _/ .-` / /  
         ======`-.____`-.___\_____/___.-`____.-'======  
                            `=---='  
  
         .............................................  
                  佛祖镇楼                  BUG辟易  
          佛曰:  
                  写字楼里写字间，写字间里程序员；  
                  程序人员写程序，又拿程序换酒钱。  
                  酒醒只在网上坐，酒醉还来网下眠；  
                  酒醉酒醒日复日，网上网下年复年。  
                  但愿老死电脑间，不愿鞠躬老板前；  
                  奔驰宝马贵者趣，公交自行程序员。  
                  别人笑我忒疯癫，我笑自己命太贱；  
                  不见满街漂亮妹，哪个归得程序员？
*/
package com.jyall.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Set;

/**
 * springMvc的trace的拦截器
 * <p>
 * 捞取header 和pro 映射
 *
 * @author zhao.weiwei
 * Created on 2017/10/31 17:59
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
@Component
public class TraceMvcInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private Tracer tracer;
    @Autowired
    private TraceProperty traceProperty;

    /**
     * 主要是添加trace的Tag，配置的属性
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        logger.debug("mvc preHandle add trace span start");
        Set<String> set = traceProperty.getHeaders();
        //先获取header的属性
        Enumeration<String> parameterNames = request.getHeaderNames();
        //使用等于2的循环，第一次是header，第二是请求参数，避免重复代码
        for (int i = 0; i < 2; i++) {
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                if (set.contains(name)) {
                    set.remove(name);
                    String value = i == 1 ? request.getParameter(name) : request.getHeader(name);
                    logger.debug("add trace tag [{}={}]", name, value);
                    tracer.getCurrentSpan().tag(name, value);
                }
            }
            //在获取请求参数的属性
            parameterNames = request.getParameterNames();
        }
        logger.debug("mvc preHandle add trace span end");
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
    }

    /**
     * 方法将在整个请求结束之后，也就是在DispatcherServlet 渲染了对应的视图之后执行。
     * 这个方法的主要作用是用于进行资源清理工作的。
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
