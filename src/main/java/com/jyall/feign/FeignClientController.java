package com.jyall.feign;


import com.google.common.collect.Sets;
import com.jyall.annotation.EnableJersey;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@ConditionalOnBean(annotation = EnableJersey.class)
public class FeignClientController {
    private static final Logger logger = LoggerFactory.getLogger(FeignClientController.class);
    @Value("${spring.application.name:}")
    private String serviceId;
    @Autowired
    private ApplicationContext applicationContext;
    private String applicationPath = "/v1";

    @PostConstruct
    public void assemblyApplicationPath() throws BeansException {
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(ApplicationPath.class);
        if (!map.isEmpty()) {
            Optional<Object> optional = map.values().stream().filter(value -> {
                try {
                    ApplicationPath app = Thread.currentThread().getContextClassLoader().loadClass(value.getClass().getName()).getAnnotation(ApplicationPath.class);
                    return app != null && StringUtils.isNotEmpty(app.value());
                } catch (Exception e) {
                    return false;
                }
            }).findFirst();
            if (optional.isPresent()) {
                try {
                    ApplicationPath applicationPathAnnotation = Thread.currentThread().getContextClassLoader().loadClass(optional.get().getClass().getName()).getAnnotation(ApplicationPath.class);
                    this.applicationPath = applicationPathAnnotation.value();
                } catch (Exception e) {
                }
            }
        }
    }

    @RequestMapping(path = "/api", method = RequestMethod.GET)
    public void api(HttpServletResponse httpServletResponse) throws Exception {
        httpServletResponse.getWriter().write(getFeignClientString());
    }


    private String getFeignClientString() {
        StringBuilder content = new StringBuilder();
        content.append("@FeignClient(\"" + serviceId + "\")\n");
        content.append("public interface DemoFeignClient {\n");
        Set<Class<?>> classes = getJerseyResourceClass();
        Set<Class<?>> importClasses = Sets.newHashSet();
        importClasses.add(Path.class);
        importClasses.add(FeignClient.class);
        importClasses.add(ResponseEntity.class);
        for (Class resourceClass : classes) {
            // 获取类@path注解，取出前缀
            String classPath = "";
            Path classPathAnnotation = (Path) resourceClass.getAnnotation(Path.class);
            if (classPathAnnotation != null) {
                classPath = classPathAnnotation.value();
            }
            for (Method method : ReflectionUtils.getAllMethods(resourceClass)) {
                if (method.getAnnotation(GET.class) != null) {
                    importClasses.add(GET.class);
                    content.append("\t@GET\n");
                } else if (method.getAnnotation(POST.class) != null) {
                    importClasses.add(POST.class);
                    content.append("\t@POST\n");
                } else if (method.getAnnotation(PUT.class) != null) {
                    importClasses.add(PUT.class);
                    content.append("\t@PUT\n");
                } else if (method.getAnnotation(DELETE.class) != null) {
                    importClasses.add(DELETE.class);
                    content.append("\t@DELETE\n");
                } else {
                    continue;
                }
                // 获取方法@Path注解
                Path methodPathAnnotation = method.getAnnotation(Path.class);
                // 方法上若无@Path注解，路径以类的@Path注解值为准
                content.append("\t@Path(\"").append(applicationPath).append(classPath);
                if (methodPathAnnotation != null) {
                    content.append(methodPathAnnotation.value());
                }
                content.append("\")\n");
                // 获取方法@Consumes注解
                Consumes methodConsumesAnnotation = method.getAnnotation(Consumes.class);
                if (methodConsumesAnnotation != null) {
                    content.append("\t@Consumes(");
                    importClasses.add(Consumes.class);
                    if (methodConsumesAnnotation.value().length > 0) {
                        for (String v : methodConsumesAnnotation.value()) {
                            content.append("\"").append(v).append("\", ");
                        }
                        content.setLength(content.length() - ", ".length());
                    }
                    content.append(")\n");
                }
                content.append("\tResponseEntity");
                // 获取返回值泛型
                ApiOperation apiOperationAnnotation = method.getAnnotation(ApiOperation.class);
                if (apiOperationAnnotation != null && apiOperationAnnotation.response() != Void.class) {
                    try {
                        String responseClass = apiOperationAnnotation.response().getSimpleName();
                        importClasses.add(apiOperationAnnotation.response());
                        String responseContainer = apiOperationAnnotation.responseContainer();
                        if (StringUtils.isNotBlank(responseContainer)) {
                            responseContainer = responseContainer.replace(responseContainer.substring(0, 1), responseContainer.substring(0, 1).toUpperCase());
                            content.append("<").append(responseContainer).append("<").append(responseClass).append(">>");
                        } else {
                            content.append("<").append(responseClass).append(">");
                        }
                    } catch (Exception e) {
                        logger.trace("返回值无泛型", e);
                    }
                }
                // 获取方法名
                String methodName = classPath;
                if (methodName.length() > 0) {
                    // 增加前缀，避免重名
                    methodName = methodName.replaceAll("/", "_");
                    if (methodName.startsWith("_")) {
                        methodName = methodName.substring(1);
                    }
                    if (!methodName.endsWith("_")) {
                        methodName += "_";
                    }
                }
                methodName += method.getName();
                content.append(" ").append(methodName).append("(");
                // 获取参数
                for (Parameter param : method.getParameters()) {
                    // 添加参数注解
                    for (Annotation paramAnnotation : param.getAnnotations()) {
                        //@Context注解属于jersey的上下文。生成feign的时候需要排除掉
                        if (!paramAnnotation.annotationType().equals(Context.class)
                                && paramAnnotation.annotationType().getPackage().getName().startsWith("javax.ws.rs")) {
                            // Jersey注解
                            content.append("@").append(paramAnnotation.annotationType().getSimpleName());
                            Class<?> paramAnnotationClass = paramAnnotation.annotationType();
                            importClasses.add(paramAnnotation.annotationType());
                            try {
                                String v = paramAnnotationClass.getMethod("value").invoke(paramAnnotation).toString();
                                content.append("(\"").append(v).append("\")");
                            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                logger.debug("此注解不包含value属性{}", paramAnnotation.annotationType().getSimpleName(), e);
                            }
                            content.append(" ");
                        } else if (paramAnnotation.annotationType() == RequestBody.class) {
                            // @RequestBody注解
                            content.append("@RequestBody(required=").append(((RequestBody) paramAnnotation).required()).append(") ");
                            importClasses.add(RequestBody.class);
                        }
                    }
                    // 添加参数类型
                    content.append(param.getType().getSimpleName()).append(" ");
                    importClasses.add(param.getType());
                    // 添加参数名称
                    content.append(param.getName()).append(", ");
                }
                // 去除多余后缀连接符
                if (method.getParameterCount() > 0) {
                    content.setLength(content.length() - ", ".length());
                }
                content.append(");\n\n");
            }
        }
        content.append("}");
        StringBuilder sb = new StringBuilder();
        sb.append("package com.jyall.feignclient;\n\n");
        importClasses.stream().filter(s -> !s.isPrimitive()).forEach(s -> sb.append("import ").append(s.getName()).append(";\n"));
        sb.append("import java.util.*;\n");
        sb.append("\n/**\n*用户注解\n*/\n");
        return sb.toString() + content.toString();
    }

    /**
     * 获取所有的带jersey注解的Path的类
     *
     * @return
     */
    private Set<Class<?>> getJerseyResourceClass() {
        Set<Class<?>> set = Sets.newHashSet();
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(Path.class);
        map.values().forEach(o -> {
            Class<?> clazz = AopUtils.isAopProxy(o) ? AopUtils.getTargetClass(o) : o.getClass();
            try {
                set.add(Thread.currentThread().getContextClassLoader().loadClass(clazz.getName()));
            } catch (Exception e) {
            }
        });
        return set;
    }
}
