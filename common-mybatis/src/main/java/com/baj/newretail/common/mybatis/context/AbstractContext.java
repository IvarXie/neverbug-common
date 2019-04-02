package com.baj.newretail.common.mybatis.context;

import com.baj.newretail.common.mybatis.annotation.MyColumn;
import com.baj.newretail.common.mybatis.annotation.MyId;
import com.baj.newretail.common.mybatis.domain.AbstractToString;
import com.baj.newretail.common.mybatis.exception.BaseServiceNotFoundException;
import com.baj.newretail.common.mybatis.service.BaseService;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Set;

/**
 * 抽象的一对一一对多的上下文关系
 * set方法使用protected的访问类别，只能在子类中使用
 *
 * @author neverbug
 */
public abstract class AbstractContext extends AbstractToString {

    /**
     * 自己的clazz
     */
    private Class<?> ownClazz;
    /**
     * 多的 实体类的class
     */
    private Class<?> otherClass;
    /**
     * 自己的属性
     */
    private Field ownField;
    /**
     * 另一个实体的属性
     */
    private Field otherField;
    /**
     * 数组或者Collection的字段，或者一对一实体的类型
     */
    private Field relationField;
    /**
     * 关联的另外一个实体操作的Service
     */
    private BaseService baseService;

    public Class<?> getOtherClass() {
        return otherClass;
    }

    protected void setOtherClass(Class<?> otherClass) {
        this.otherClass = otherClass;
    }

    public Class<?> getOwnClazz() {
        return ownClazz;
    }

    protected void setOwnClazz(Class<?> ownClazz) {
        this.ownClazz = ownClazz;
    }

    public Field getOwnField() {
        return ownField;
    }

    protected void setOwnField(Field ownField) {
        ownField.setAccessible(true);
        this.ownField = ownField;
    }

    public Field getOtherField() {
        return otherField;
    }

    protected void setOtherField(Field otherField) {
        otherField.setAccessible(true);
        this.otherField = otherField;
    }

    public Field getRelationField() {
        return relationField;
    }

    protected void setRelationField(Field relationField) {
        relationField.setAccessible(true);
        this.relationField = relationField;
    }

    public BaseService getBaseService() {
        return baseService;
    }

    protected void setBaseService(BaseService baseService) {
        this.baseService = baseService;
    }

    /**
     * 获取自己的关联字段
     *
     * @param clazz
     * @param refrenceOwnFieldName
     * @return
     */
    @SuppressWarnings("all")
    protected static Field getOwnField(Class<?> clazz, String refrenceOwnFieldName) {
        Set<Field> refrenceOwnFieldSet = ReflectionUtils.getAllFields(clazz,
                ReflectionUtils.withName(refrenceOwnFieldName), ReflectionUtils.withAnnotation(MyColumn.class));
        refrenceOwnFieldSet = refrenceOwnFieldSet.isEmpty() ? ReflectionUtils.getAllFields(clazz,
                ReflectionUtils.withName(refrenceOwnFieldName), ReflectionUtils.withAnnotation(MyId.class)) : refrenceOwnFieldSet;
        return refrenceOwnFieldSet.isEmpty() ? null : refrenceOwnFieldSet.iterator().next();
    }

    /**
     * 获取关联的类别
     *
     * @param field 关联的字段
     * @return
     */
    protected static Class<?> getOtherClass(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) ?
                Class.class.cast(ParameterizedType.class.cast(field.getGenericType()).getActualTypeArguments()[0]) :
                field.getType().isArray() ? field.getType().getComponentType() : field.getType();
    }

    /**
     * 组装一对一一对多的关系
     *
     * @param obj
     */
    protected abstract void assembly(Object obj);

    /**
     * 分析获取关联的BaseService
     */
    public void analysisBaseService() {
        baseService = MybatisContext.getBaseServiceByEntityClass(otherClass);
        if (baseService == null) {
            throw new BaseServiceNotFoundException(otherClass.getSimpleName() + "'s baseService not found");
        }
    }
}
