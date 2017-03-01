package com.jyall.feign;

import com.wordnik.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.*;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class FeignClientContentUtil {
    private static final Logger logger = LoggerFactory.getLogger(FeignClientContentUtil.class);
    // 前缀
    private static String applicationPath = "/v1";
//    @Value("${spring.application.name:}")
//    private static String serviceId;


    private FeignClientContentUtil() {
    }

    public static String getFeignClientContent(String serviceId) {
        StringBuilder content = new StringBuilder("@FeignClient(\"" + serviceId + "\")\n");
        content.append("public interface DemoFeignClient {\n");
        Set<Class<?>> classes = getJyallClass();
        for (Class resourceClass : classes) {
            // 获取类@path注解，取出前缀
            String classPath = "";
            Path classPathAnnotation = (Path) resourceClass.getAnnotation(Path
                    .class);
            if (classPathAnnotation != null) {
                classPath = classPathAnnotation.value();
            }
            for (Method method : resourceClass.getDeclaredMethods()) {
                // 获取HTTP方法注解
                if (method.getAnnotation(GET.class) != null) {
                    content.append("\t@GET\n");
                } else if (method.getAnnotation(POST.class) != null) {
                    content.append("\t@POST\n");
                } else if (method.getAnnotation(PUT.class) != null) {
                    content.append("\t@PUT\n");
                } else if (method.getAnnotation(DELETE.class) != null) {
                    content.append("\t@DELETE\n");
                } else {
                    continue;
                }
                // 获取方法@Path注解
                Path methodPathAnnotation = method.getAnnotation(Path.class);
                // 方法上若无@Path注解，路径以类的@Path注解值为准
                content.append("\t@Path(\"").append(applicationPath).append
                        (classPath);
                if (methodPathAnnotation != null) {
                    content.append(methodPathAnnotation.value());
                }
                content.append("\")\n");
                // 获取方法@Consumes注解
                Consumes methodConsumesAnnotation = method.getAnnotation
                        (Consumes.class);
                if (methodConsumesAnnotation != null) {
                    content.append("\t@Consumes(");
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
                if (apiOperationAnnotation != null && apiOperationAnnotation
                        .response() != Void.class) {
                    try {
                        String responseClass = apiOperationAnnotation.response().getSimpleName();
                        content.append("<").append(responseClass).append(">");
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
                content.append(" ")
                        .append(methodName)
                        .append("(");
                // 获取参数
                for (Parameter param : method.getParameters()) {
                    // 添加参数注解
                    for (Annotation paramAnnotation : param.getAnnotations()) {
                        if (paramAnnotation.annotationType().getPackage()
                                .getName().startsWith("javax.ws.rs")) {
                            // Jersey注解
                            content.append("@")
                                    .append(paramAnnotation.annotationType().getSimpleName());
                            Class paramAnnotationClass = paramAnnotation.annotationType();
                            try {
                                @SuppressWarnings("unchecked")
                                String v = paramAnnotationClass.getMethod("value")
                                        .invoke(paramAnnotation).toString();
                                content.append("(\"").append(v).append("\")");
                            } catch (IllegalAccessException | InvocationTargetException |
                                    NoSuchMethodException e) {
                                logger.debug("此注解不包含value属性{}",
                                        paramAnnotation.annotationType().getSimpleName(), e);
                            }
                            content.append(" ");
                        } else if (paramAnnotation.annotationType() == RequestBody.class) {
                            // @RequestBody注解
                            content.append("@RequestBody(required=")
                                    .append(((RequestBody) paramAnnotation).required())
                                    .append(") ");
                        }
                    }
                    // 添加参数类型
                    content.append(param.getType().getSimpleName()).append(" ");
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
        return content.toString();
    }

    public static Set<Class<?>> getJyallClass() {
        Set<Class<?>> classes = new HashSet<>();
        for (Package p : Package.getPackages()) {
            String packageName = p.getName();
            if (packageName.startsWith("com.jyall.")) {
                // 只搜索jyall下的包
                if (packageName.endsWith("resource")) {
                    // 需要生成FeignClient的接口
                    classes.addAll(getClasses(packageName));
                } else if (packageName.endsWith("config")) {
                    // 获取JerseyCinfig前缀
                    for (Class<?> configClass : getClasses(packageName)) {
                        ApplicationPath applicationPathAnnotation = configClass.getAnnotation
                                (javax.ws.rs.ApplicationPath.class);
                        if (applicationPathAnnotation != null) {
                            applicationPath = applicationPathAnnotation.value();
                            break;
                        }
                    }
                }
            }
        }
        return classes;
    }


    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName 包名
     * @param dir         包路径对应目录对象
     * @param recursive   是否循环查找
     * @param classes     类结果集合
     */
    public static void findAndAddClassesInPackageByFile(String packageName, File dir,
                                                        final boolean recursive, Set<Class<?>>
                                                                classes) {
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirFiles = dir.listFiles(file -> (recursive && file.isDirectory())
                || (file.getName().endsWith(".class")));
        // 循环所有文件
        if (dirFiles != null && dirFiles.length > 0)
            Arrays.stream(dirFiles).forEach(file -> {
                // 如果是目录 则继续扫描
                if (file.isDirectory()) {
                    findAndAddClassesInPackageByFile(packageName + "."
                                    + file.getName(), file,
                            recursive,
                            classes);
                } else {
                    // 如果是java类文件 去掉后面的.class 只留下类名
                    String className = file.getName().substring(0,
                            file.getName().length() - 6);
                    try {
                        // 添加到集合中去
                        //classes.add(Class.forName(packageName + '.' + className));
                        //经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader
                        // 的load干净
                        classes.add(Thread.currentThread().getContextClassLoader
                                ().loadClass(packageName + '.' + className));
                    } catch (ClassNotFoundException e) {
                        logger.error("添加用户自定义视图类错误 找不到此类的.class文件", e);
                    }
                }
            });
    }


    /**
     * 从包package中获取所有的Class
     *
     * @param packageName 包名
     * @return 包里的所有类集合
     */
    @SuppressWarnings("ConstantConditions")
    public static Set<Class<?>> getClasses(String packageName) {

        // 第一个class类的集合
        Set<Class<?>> classes = new LinkedHashSet<>();
        // 是否循环迭代
        boolean recursive = false;
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(
                    packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
//					System.err.println("file类型的扫描");
                    // 获取包的物理路径
//                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, new File(url.toURI()),
                            recursive, classes);
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件
                    // 定义一个JarFile
                    JarFile jar;
                    // 获取jar
                    jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    // 从此jar包 得到一个枚举类
                    Enumeration<JarEntry> entries = jar.entries();
                    // 同样的进行循环迭代
                    while (entries.hasMoreElements()) {
                        // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        // 如果是以/开头的
                        if (name.charAt(0) == '/') {
                            // 获取后面的字符串
                            name = name.substring(1);
                        }
                        // 如果前半部分和定义的包名相同
                        if (name.startsWith(packageDirName)) {
                            int idx = name.lastIndexOf('/');
                            // 如果以"/"结尾 是一个包
                            String finalPackageName = name;
                            if (idx != -1) {
                                // 获取包名 把"/"替换成"."
                                finalPackageName = name.substring(0, idx).replace('/', '.');
                            }
                            // 如果可以迭代下去 并且是一个包
                            if ((idx != -1 || recursive)
                                    // 如果是一个.class文件 而且不是目录
                                    && name.endsWith(".class") && !entry.isDirectory()) {
                                // 去掉后面的".class" 获取真正的类名
                                String className = name.substring(finalPackageName.length() + 1,
                                        name.length() - ".class".length());
                                addClasses(classes, finalPackageName + '.' + className);
                            }
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("", e);
        }
        return classes;
    }

    private static void addClasses(Set<Class<?>> classes, String name) {
        try {
            // 添加到classes
            classes.add(Class.forName(name));
        } catch (ClassNotFoundException e) {
            logger.error("添加用户自定义视图类错误 找不到此类的.class文件", e);
        }
    }
}


