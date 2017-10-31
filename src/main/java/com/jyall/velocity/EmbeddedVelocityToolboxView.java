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
package com.jyall.velocity;


import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.view.velocity.VelocityToolboxView;

/**
 * Extended version of {@link VelocityToolboxView} that can load toolbox locations from
 * the classpath as well as the servlet context. This is useful when running in an
 * embedded web server.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.5
 */
@SuppressWarnings("deprecation")
public class EmbeddedVelocityToolboxView extends VelocityToolboxView {

    @Override
    protected Context createVelocityContext(Map<String, Object> model,
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        org.apache.velocity.tools.view.context.ChainedContext context = new org.apache.velocity.tools.view.context.ChainedContext(
                new VelocityContext(model), getVelocityEngine(), request, response,
                getServletContext());
        if (getToolboxConfigLocation() != null) {
            setContextToolbox(context);
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    private void setContextToolbox(
            org.apache.velocity.tools.view.context.ChainedContext context) {
        org.apache.velocity.tools.view.ToolboxManager toolboxManager = org.apache.velocity.tools.view.servlet.ServletToolboxManager
                .getInstance(getToolboxConfigFileAwareServletContext(),
                        getToolboxConfigLocation());
        Map<String, Object> toolboxContext = toolboxManager.getToolbox(context);
        context.setToolbox(toolboxContext);
    }

    private ServletContext getToolboxConfigFileAwareServletContext() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(getServletContext());
        factory.addAdvice(new GetResourceMethodInterceptor(getToolboxConfigLocation()));
        return (ServletContext) factory.getProxy(getClass().getClassLoader());
    }

    /**
     * {@link MethodInterceptor} to allow the calls to getResourceAsStream() to resolve
     * the toolboxFile from the classpath.
     */
    private static class GetResourceMethodInterceptor implements MethodInterceptor {

        private final String toolboxFile;

        GetResourceMethodInterceptor(String toolboxFile) {
            if (toolboxFile != null && !toolboxFile.startsWith("/")) {
                toolboxFile = "/" + toolboxFile;
            }
            this.toolboxFile = toolboxFile;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            if (invocation.getMethod().getName().equals("getResourceAsStream")
                    && invocation.getArguments()[0].equals(this.toolboxFile)) {
                InputStream inputStream = (InputStream) invocation.proceed();
                if (inputStream == null) {
                    try {
                        inputStream = new ClassPathResource(this.toolboxFile,
                                Thread.currentThread().getContextClassLoader())
                                .getInputStream();
                    }
                    catch (Exception ex) {
                        // Ignore
                    }
                }
                return inputStream;
            }
            return invocation.proceed();
        }

    }

}
