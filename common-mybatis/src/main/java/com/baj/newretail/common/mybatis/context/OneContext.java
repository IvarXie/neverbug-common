package com.baj.newretail.common.mybatis.context;

import com.baj.newretail.common.mybatis.annotation.MyColumn;
import com.baj.newretail.common.mybatis.annotation.MyId;
import com.baj.newretail.common.mybatis.annotation.MyOne;
import com.baj.newretail.common.mybatis.domain.AbstractDataDomain;
import com.baj.newretail.common.mybatis.exception.One2OneException;
import com.baj.newretail.common.mybatis.service.BaseService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 一对一关系的上下文
 *
 * @author neverbug
 * Created on 2018/5/24 13:41
 */
public class OneContext extends AbstractContext {
    /**
     * slf4j日志
     */
    private static Logger logger = LoggerFactory.getLogger(OneContext.class);

    /**
     * 私有构造函数
     */
    private OneContext() {

    }

    /**
     * 是否是另一个表的主键,
     * 如果是主键，调用getById
     * 如果不是主键，设置属性，调用seleteOneAccuracy
     */
    private boolean primary;

    /**
     * 私有的方法，只能在本类中调用
     *
     * @return
     */
    private boolean isPrimary() {
        return primary;
    }

    /**
     * 私有的方法，只能在本类中调用
     *
     * @param primary
     */
    private void setPrimary(boolean primary) {
        this.primary = primary;
    }

    @SuppressWarnings("all")
    public static List<OneContext> analysisMetedata(Class<?> clazz) throws Exception {
        Set<Field> one2oneField = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(MyOne.class));
        List<OneContext> contextList = Lists.newArrayList();
        for (Field field : one2oneField) {
            if (!Collection.class.isAssignableFrom(field.getType()) && !field.getType().isArray()) {
                MyOne one = field.getAnnotation(MyOne.class);
                Class<?> oneClass = getOtherClass(field);
                String refrenceOwnFieldName = one.refrenceOwnField();
                String refrenceOtherFieldName = one.refrenceOtherField();
                Field refrenceOwnField = getOwnField(clazz, refrenceOwnFieldName);
                if (refrenceOwnField != null) {
                    Set<Field> refrenceOtherFieldSet = one.primary() ?
                            ReflectionUtils.getAllFields(oneClass, ReflectionUtils.withName(refrenceOtherFieldName), ReflectionUtils.withAnnotation(MyId.class)) :
                            ReflectionUtils.getAllFields(oneClass, ReflectionUtils.withName(refrenceOtherFieldName), ReflectionUtils.withAnnotation(MyColumn.class));
                    Field refrenceOtherField = refrenceOtherFieldSet.isEmpty() ? null : refrenceOtherFieldSet.iterator().next();
                    if (refrenceOtherField != null) {
                        OneContext oneContext = new OneContext();
                        oneContext.setOwnClazz(clazz);
                        oneContext.setRelationField(field);
                        oneContext.setOwnField(refrenceOwnField);
                        oneContext.setOtherField(refrenceOtherField);
                        oneContext.setOtherClass(oneClass);
                        oneContext.setPrimary(one.primary());
                        contextList.add(oneContext);
                    } else {
                        /*其他实体的的字段找不到*/
                        logger.error("[{}] @MyOne refrence other field [{}] error,it with not @MyColumn or @MyId ", clazz.getName(), refrenceOtherFieldName);
                        throw new One2OneException(String.format(
                                "[%s] @MyOne refrence other field [%s] error,it with not @MyColumn or @MyId ", clazz.getName(), refrenceOtherFieldName));
                    }
                } else {
                    /*映射的自己的字段找不到*/
                    logger.error("[{}] @MyOne refrence own field [{}] error,it with not @MyColumn or @MyId ", clazz.getName(), refrenceOwnFieldName);
                    throw new One2OneException(String.format(
                            "[%s] @MyOne refrence own field [%s] error,it with not @MyColumn or @MyId ", clazz.getName(), refrenceOwnFieldName));
                }
            } else {
                /*一对一的关系的实体的字段类别不是数组或者集合*/
                logger.error("[{}] @MyOne field [{}] type rerror [{}], not collection or array", clazz.getName(), field.getName(), field.getType());
                throw new One2OneException(String.format(
                        "[{%s] @MyOne field [%s] type error [%s],, not collection or array", clazz.getName(), field.getName(), field.getType()));
            }
        }
        return contextList;
    }

    @Override
    @SuppressWarnings("all")
    public void assembly(Object t) {
        try {
            Field refrenceOneField = getOwnField();
            Field refrenceOtherField = getOtherField();
            Object refrenceOwnFieldValue = refrenceOneField.get(t);
            Class<?> otherClass = getOtherClass();
            Field field = getRelationField();
            if (refrenceOwnFieldValue != null && StringUtils.isNotEmpty(String.valueOf(refrenceOwnFieldValue))) {
                /*执行查询赋值*/
                BaseService baseService = MybatisContext.getBaseServiceByEntityClass(otherClass);
                if (baseService != null) {
                    Object obj;
                    if (isPrimary()) {
                        /*如果是主键，根据id查询*/
                        obj = baseService.getById(refrenceOwnFieldValue);
                    } else {
                        /*如果不是主键，反射调用无参数的构造方法创建对象*/
                        Object refrence = getOtherClass().newInstance();
                        if (refrence instanceof AbstractDataDomain) {
                            AbstractDataDomain.class.cast(refrence).setOrderBy("create_time desc");
                        }
                        /*设置关联属性的值*/
                        refrenceOtherField.set(refrence, refrenceOwnFieldValue);
                        /*调用精确查询第一条的方法*/
                        obj = baseService.seleteOneAccuracy(refrence);
                    }
                    if (obj != null) {
                        field.set(t, obj);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }
}
