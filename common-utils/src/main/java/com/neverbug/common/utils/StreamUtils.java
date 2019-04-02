package com.neverbug.common.utils;


import org.springframework.cglib.beans.BeanCopier;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author xie.wenbo
 * @Description stream操作工具类
 * @CreationDate: 2018-08-30 9:27
 */
public class StreamUtils {

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/9 18:45
     * @Description 抽取字段为list
     * @param list 集合
     * @param function function
     * @return java.util.List<R>
     */
    public static <R,T> List<R> extractFeildToList(List<T> list, Function<? super T, ? extends R> function) {
        return extractFeildToList(list.stream(),function);
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/9 18:46
     * @Description 抽取字段为list
     * @param stream stream
     * @param function function
     * @return java.util.List<R>
     */
    public static <R,T> List<R> extractFeildToList(Stream<T> stream, Function<? super T, ? extends R> function) {
        return stream.map(function).collect(Collectors.toList());
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 10:11
     * @Description TD
     * @param stream stream
     * @param targetSupplier targetSupplier
     * @return List<R>
     */
    public static <R,T> List<R> copyToList(Stream<T> stream, Supplier<R> targetSupplier) {
        return stream.map(obj -> copyProperties(obj, targetSupplier.get())).collect(Collectors.toList());
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 10:21
     * @Description TD
     * @param source 源对象
     * @param t 目标对象
     * @return T
     */
    private static <T> T copyProperties(Object source, T t){
        final BeanCopier beanCopier =  BeanCopier.create(source.getClass(),t.getClass(),false);
        beanCopier.copy(source,t,null);
        return t;
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 10:21
     * @Description stream去重
     * @param keyExtractor function
     * @return java.util.function.Predicate<T>
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}