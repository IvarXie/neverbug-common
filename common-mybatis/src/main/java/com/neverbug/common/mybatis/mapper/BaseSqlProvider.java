package com.neverbug.common.mybatis.mapper;

import com.neverbug.common.mybatis.annotation.*;
import com.neverbug.common.mybatis.context.MybatisContext;
import com.neverbug.common.mybatis.domain.AbstractDataDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.SQL;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基本的sql生成类
 *
 * @create by neverbug
 * @create on 2017年4月26日下午3:11:28
 */
@SuppressWarnings("all")
public class BaseSqlProvider {
    /**
     * slf4j日志
     */
    protected static Logger logger = LoggerFactory.getLogger(BaseSqlProvider.class);
    /**
     * 实体和字段的缓存
     */
    private static Map<Class<?>, List<Field>> objectFieldCache = Maps.newHashMap();
    /**
     * 实体字段的查询的缓存
     */
    private static Map<Class<?>, String> allFieldStringCache = Maps.newHashMap();
    /**
     * 单个实体的查询的缓存
     */
    private static Map<Class<?>, String> selectStringCache = Maps.newHashMap();

    /**
     * 获取表明名
     *
     * @param obj
     * @return MyTable注解
     */
    public static MyTable getMyTable(Object obj) {
        Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
        for (int i = 0; !Object.class.equals(clazz) && i < 3; i++) {
            MyTable myTable = clazz.getAnnotation(MyTable.class);
            if (myTable != null) {
                return myTable;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * AbstractDataDomain 设置create_time等属性
     *
     * @param dataDomain domain实体
     * @param type       0表示创建，1表示修改或者删除
     */
    private static void assembly(AbstractDataDomain dataDomain, int type) {
        String userid;
        try {
            userid = MybatisContext.getUserId();
        } catch (Exception e) {
            logger.warn("get userId error {}", e.getMessage());
            userid = "";
        }
        if (type == 0) {
            dataDomain.setCreateTime(new Date());
        } else {
            dataDomain.setUpdateTime(new Date());
        }
        if (StringUtils.isNotEmpty(userid)) {
            if (type == 0) {
                dataDomain.setCreateBy(userid);
            } else {
                dataDomain.setUpdateBy(userid);
            }
        }
    }

    /**
     * 插入实体的SQL
     *
     * @param obj insert的实体对象
     * @return 插入的SQL
     * @throws Exception
     */
    public static String insert(Object obj) throws Exception {
        if (obj instanceof AbstractDataDomain) {
            assembly(AbstractDataDomain.class.cast(obj), 0);
        }
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.INSERT_INTO(table.value());
        for (Field field : getAllField(obj)) {
            field.setAccessible(true);
            Object colValue = field.get(obj);
            MyColumn column = field.getAnnotation(MyColumn.class);
            MyId id = field.getAnnotation(MyId.class);
            if (column != null && colValue == null){
                continue;
            }
            if (id != null && colValue == null) {
                colValue = UUID.randomUUID().toString().replace("-", "");
                field.set(obj, colValue);
            }
            String colName;
            if (column != null)
                colName = StringUtils.isBlank(column.value()) ? field.getName() : column.value();
            else
                colName = StringUtils.isBlank(id.value()) ? field.getName() : id.value();
            sql.VALUES(String.format("`%s`", colName), String.format("#{%s}", field.getName()));
        }
        logger.debug("insert sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 更新实体的SQL
     *
     * @param obj update的实体对象
     * @return 修改的SQL
     * @throws Exception
     */
    public static String update(Object obj) throws Exception {
        if (obj instanceof AbstractDataDomain) {
            assembly(AbstractDataDomain.class.cast(obj), 1);
        }
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.UPDATE(table.value());
        Field idField = null;
        for (Field field : getAllField(obj)) {
            field.setAccessible(true);
            Object colValue = field.get(obj);
            MyColumn column = field.getAnnotation(MyColumn.class);
            MyId id = field.getAnnotation(MyId.class);
            if (column != null && colValue == null)
                continue;
            if (id != null) {
                idField = field;
                continue;
            }
            String colName = StringUtils.isBlank(column.value()) ? field.getName() : column.value();
            sql.SET(String.format("`%s` = #{%s}", colName, field.getName()));
        }
        if (idField != null) {
            MyId id = idField.getAnnotation(MyId.class);
            String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
            sql.WHERE(String.format("`%s` = #{%s}", idColName, idField.getName()));
        }
        logger.debug("update sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 批量更新实体的SQL
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public static String batchUpdate(Object obj) throws Exception {
        if (obj instanceof AbstractDataDomain) {
            assembly(AbstractDataDomain.class.cast(obj), 1);
        }
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.UPDATE(table.value());
        for (Field field : getAllField(obj)) {
            field.setAccessible(true);
            Object colValue = field.get(obj);
            MyId id = field.getAnnotation(MyId.class);
            MyWhere where = field.getAnnotation(MyWhere.class);
            MyColumn column = field.getAnnotation(MyColumn.class);
            if (column != null && colValue == null) continue;
            if (where != null || id != null) continue;
            String colName = StringUtils.isBlank(column.value()) ? field.getName() : column.value();
            sql.SET(String.format("`%s` = #{%s}", colName, field.getName()));
        }
        for (Field field : getAllWhere(obj)) {
            field.setAccessible(true);
            Object colValue = field.get(obj);
            MyWhere where = field.getAnnotation(MyWhere.class);
            MyColumn column = field.getAnnotation(MyColumn.class);
            if (where != null && colValue == null)
                continue;
            String columnValue = field.getName();
            if (null != column && StringUtils.isNotBlank(column.value())) {
                columnValue = column.value();
            }
            String whereCol = StringUtils.isBlank(where.value()) ? columnValue : where.value();
            sql.WHERE(String.format("`%s` = #{%s}", whereCol, field.getName()));
        }
        logger.debug("batch update sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 通过ID获取实体的sql
     *
     * @param obj 设置@MyId属性的对象
     * @return 查询的SQL
     * @throws Exception
     */
    public static String getById(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.SELECT(getAllFieldString(obj));
        sql.FROM(table.value());
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        sql.WHERE(String.format("`%s` = #{%s}", idColName, idField.getName()));
        logger.debug("getById sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 根据ID删除的sql
     *
     * @param obj 设置@MyId属性的对象
     * @return 删除的SQL
     * @throws Exception
     */
    public static String deleteById(Object obj) throws Exception {
        if (obj instanceof AbstractDataDomain && MybatisContext.isLogicDelete()) {
            AbstractDataDomain.class.cast(obj).setDelFlag(true);
            return update(obj);
        }
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.DELETE_FROM(table.value());
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        sql.WHERE(String.format("`%s` = #{%s}", idColName, idField.getName()));
        logger.debug("deleteById sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 模糊查询的SQL生成
     *
     * @param paramAlias 参数的前缀，组装SQL的是时候  paramAlias.pro
     * @param obj        模糊查询的实体
     * @return 模糊查询的SQL
     * @throws Exception
     */
    public static String seleteVagueParamAlias(String paramAlias, Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.SELECT(getAllFieldString(obj));
        sql.FROM(table.value());
        assemblyVague(paramAlias, obj, sql);
        Field idField = getIdField(obj);
        if (idField != null) {
            idField.setAccessible(true);
            MyId myId = idField.getAnnotation(MyId.class);
            Object idValue = idField.get(obj);
            String colName = StringUtils.isBlank(myId.value()) ? idField.getName() : myId.value();
            if (idValue != null && StringUtils.isNotEmpty(String.valueOf(idValue)))
                sql.WHERE(String.format("`%s` like %s", colName, "'%" + String.valueOf(idValue) + "%'"));
        }
        assemblyOrderBy(obj, sql);
        logger.debug("seleteVagueParamAlias sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 模糊查询的SQL生成
     *
     * @param obj 模糊查询的实体
     * @return 模糊查询的SQL
     * @throws Exception
     */
    public static String seleteVague(Object obj) throws Exception {
        return seleteVagueParamAlias("", obj);
    }

    /**
     * 模糊查询一条数据的SQL
     *
     * @param obj 模糊查询一条数据 实体
     * @return 模糊查询一条数据的SQL
     * @throws Exception
     */
    public static String seleteOneVague(Object obj) throws Exception {
        return String.format("%s limit 1", seleteVagueParamAlias("", obj));
    }

    /**
     * 精确查询一条的sql生成
     *
     * @param paramAlias 参数的前缀，组装SQL的是时候  paramAlias.pro
     * @param obj        精确查询一条数据 实体
     * @return 精确查询一条的sql
     * @throws Exception
     */
    public static String seleteAccuracyParamAlias(String paramAlias, Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.SELECT(getAllFieldString(obj));
        sql.FROM(table.value());
        assemblyAccuracy(paramAlias, obj, sql);
        assemblyOrderBy(obj, sql);
        logger.debug("seleteAccuracy sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 精确查询数据的sql生成
     *
     * @param obj 精确查询数据 实体
     * @return 精确查询数据的sql
     * @throws Exception
     */
    public static String seleteAccuracy(Object obj) throws Exception {
        return seleteAccuracyParamAlias("", obj);
    }


    /**
     * 精确单条查询的sql生成
     * sql尾部添加 limit 1
     *
     * @param obj 精确查询数据 实体
     * @return 精确单条查询的sql生成
     * @throws Exception
     */
    public static String seleteOneAccuracy(Object obj) throws Exception {
        return String.format("%s limit 1", seleteAccuracyParamAlias("", obj));
    }


    /**
     * 查询所有的sql
     *
     * @param obj 实体对象，可以是 Dict.class ，也可以是new Dict()
     * @return 查询的SQL
     * @throws Exception
     */
    public static String findAll(Object obj) throws Exception {
        return getSelectString(obj);
    }

    /**
     * 删除所有的SQL，支持逻辑删除
     *
     * @param obj 删除的实体
     * @return
     */
    public static String deleteAll(Object obj) throws Exception {
        if (obj instanceof AbstractDataDomain && MybatisContext.isLogicDelete()) {
            Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
            AbstractDataDomain domain = AbstractDataDomain.class.cast(clazz.newInstance());
            domain.setDelFlag(true);
            return update(domain);
        }
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.DELETE_FROM(table.value());
        logger.debug("deleteAll sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 模糊匹配删除的SQL,支持逻辑删除
     *
     * @param obj 模糊匹配删除的逻辑实体
     * @return 删除的SQL
     * @throws Exception
     */
    public static String deleteVague(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        if (obj instanceof AbstractDataDomain && MybatisContext.isLogicDelete()) {
            AbstractDataDomain domain = AbstractDataDomain.class.cast(obj);
            domain.setDelFlag(false);
            assembly(domain, 1);
            sql.UPDATE(table.value());
            sql.SET(String.format("`%s` = #{%s}", "update_time", "updateTime"));
            sql.SET(String.format("`%s` = #{%s}", "update_by", "updateBy"));
            sql.SET(String.format("`%s` = 1", "del_flag"));
            AbstractDataDomain newObj = AbstractDataDomain.class.cast(obj.getClass().newInstance());
            BeanUtils.copyProperties(obj, newObj, "updateTime", "updateBy", "delFlag");
            assemblyVague("", newObj, sql);
        } else {
            sql.DELETE_FROM(table.value());
            assemblyVague("", obj, sql);
            //防止数据全删
            if(!sql.toString().contains("=")){
                sql.WHERE("1 = 2");
            }
        }
        logger.debug("deleteVague sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 精确匹配删除的SQL，支持逻辑删除
     *
     * @param obj 精确匹配删除的逻辑实体
     * @return
     * @throws Exception
     */
    public static String deleteAccuracy(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        if (obj instanceof AbstractDataDomain && MybatisContext.isLogicDelete()) {
            AbstractDataDomain domain = AbstractDataDomain.class.cast(obj);
            domain.setDelFlag(false);
            assembly(domain, 1);
            sql.UPDATE(table.value());
            sql.SET(String.format("`%s` = #{%s}", "update_time", "updateTime"));
            sql.SET(String.format("`%s` = #{%s}", "update_by", "updateBy"));
            sql.SET(String.format("`%s` = 1", "del_flag"));
            AbstractDataDomain newObj = AbstractDataDomain.class.cast(obj.getClass().newInstance());
            BeanUtils.copyProperties(obj, newObj, "updateTime", "updateBy", "delFlag");
            assemblyAccuracy("", newObj, sql);
        } else {
            sql.DELETE_FROM(table.value());
            assemblyAccuracy("", obj, sql);
            //防止数据全删
            if(!sql.toString().contains("=")){
                sql.WHERE("1 = 2");
            }
        }
        logger.debug("deleteAccuracy sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 统计总数的SQL
     *
     * @param obj 实体对象，可以是Dict.class 也可以是new Dict()
     * @return
     */
    public static String countAll(Object obj) {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        sql.SELECT(String.format("count(`%s`)", idColName));
        sql.FROM(table.value());
        logger.debug("countAll sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 模糊匹配统计总数
     *
     * @param obj 模糊匹配的实体
     * @return 模糊匹配统计总数的SQL
     * @throws Exception
     */
    public static String countVague(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        sql.SELECT(String.format("count(`%s`)", idColName));
        sql.FROM(table.value());
        assemblyVague("", obj, sql);
        logger.debug("countVague sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 精确查询统计总数
     *
     * @param obj 精确匹配的实体
     * @return 精确查询统计总数的SQL
     * @throws Exception
     */
    public static String countAccuracy(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        sql.SELECT(String.format("count(`%s`)", idColName));
        sql.FROM(table.value());
        assemblyAccuracy("", obj, sql);
        logger.debug("countAccuracy sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 获取所有的带有@MyId和@MyColumn的列
     *
     * @param obj 实体，可以是Dict.class 也可以是new Dict()
     * @return Field的集合
     */
    public static List<Field> getAllField(Object obj) {
        Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
        Class<?> objectClass = clazz;
        if (objectFieldCache.containsKey(clazz)) {
            return objectFieldCache.get(clazz);
        }
        List<Field> fields = Lists.newArrayList();
        Set<String> nameSet = Sets.newHashSet();
        while (!clazz.equals(Object.class)) {
            List<Field> current = ReflectionUtils.getAllFields(clazz).stream()
                    .filter(field -> ReflectionUtils.getAnnotations(field, input -> input.annotationType().equals(MyId.class) || input.annotationType().equals(MyColumn.class)).size() > 0)
                    .collect(Collectors.toList());
            current.stream().filter(f -> !nameSet.contains(f.getName())).forEach(f -> {
                fields.add(f);
                nameSet.add(f.getName());
            });
            clazz = clazz.getSuperclass();
        }
        objectFieldCache.put(objectClass, fields);
        return fields;
    }

    /**
     * 获取所有的带有@MyWhere的列
     *
     * @param obj
     * @return
     */
    public static List<Field> getAllWhere(Object obj) {
        Class<?> clazz = obj.getClass();
        List<Field> fields = Lists.newArrayList();
        Set<String> nameSet = Sets.newHashSet();
        while (!clazz.equals(Object.class)) {
            List<Field> current = ReflectionUtils.getAllFields(clazz).stream()
                    .filter(field -> ReflectionUtils.getAnnotations(field, input -> input.annotationType().equals(MyWhere.class)).size() > 0)
                    .collect(Collectors.toList());
            current.stream().filter(f -> !nameSet.contains(f.getName())).forEach(f -> {
                fields.add(f);
                nameSet.add(f.getName());
            });
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 获取所有的带有@MyId和@MyColumn的列的，insert，select字符串
     *
     * @param obj 实体，可以是Dict.class 也可以是new Dict()
     * @return 所有字段的sql映射，不带表名
     */
    public static String getAllFieldString(Object obj) {
        Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
        if (allFieldStringCache.containsKey(clazz)) {
            return allFieldStringCache.get(clazz);
        }
        String sql = getAllFieldString(obj, "");
        allFieldStringCache.put(clazz, sql);
        return sql;
    }

    /**
     * 获取所有的带有@MyId和@MyColumn的列的，inser，select字符串
     *
     * @param obj
     * @param alias 表的别名
     * @return
     */
    public static String getAllFieldString(Object obj, String alias) {
        StringBuilder sb = new StringBuilder(500);
        getAllField(obj).forEach(field -> {
            MyColumn column = field.getAnnotation(MyColumn.class);
            MyId id = field.getAnnotation(MyId.class);
            if (column != null)
                sb.append(StringUtils.isEmpty(alias) ? "`" : alias + ".`").append(StringUtils.isBlank(column.value()) ? field.getName() : column.value()).append("`");
            else if (id != null)
                sb.append(StringUtils.isEmpty(alias) ? "`" : alias + ".`").append(StringUtils.isBlank(id.value()) ? field.getName() : id.value()).append("`");
            if (column != null || id != null)
                sb.append(" as ").append("`").append(field.getName()).append("`").append(",");
        });
        sb.setCharAt(sb.length() - 1, ' ');
        return sb.toString();
    }

    /**
     * 获取查询的字符串
     *
     * @param obj 可以是Dict.class 也可以是new Dict()
     * @return select的查询语句
     */
    public static String getSelectString(Object obj) {
        Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
        if (selectStringCache.containsKey(clazz)) {
            return selectStringCache.get(clazz);
        } else {
            String sql = getSelectString(obj, "");
            selectStringCache.put(clazz, sql);
            return sql;
        }
    }

    /**
     * 获取查询的字符串
     *
     * @param obj 可以是Dict.class 也可以是new Dict()
     * @return select的查询语句
     */
    public static String getSelectString(Object obj, String alias) {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.SELECT(getAllFieldString(obj, alias));
        sql.FROM(table.value() + (StringUtils.isEmpty(alias) ? "" : " " + alias));
        logger.debug("getSelectString sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 获取带有@MyId的field
     *
     * @param obj 可以是Dict.class 也可以是new Dict()
     * @return field对象
     */
    public static Field getIdField(Object obj) {
        Field idField = null;
        Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
        while (!clazz.equals(Object.class)) {
            Optional<Field> optional =
                    ReflectionUtils.getFields(clazz, ReflectionUtils.withAnnotation(MyId.class)).stream().findFirst();
            if (optional.isPresent()) {
                idField = optional.get();
                idField.setAccessible(true);
                break;
            }
            clazz = clazz.getSuperclass();
        }
        return idField;
    }

    /**
     * 获取排序字段@MyOrderBy
     *
     * @param obj 可以是Dict.class 也可以是new Dict()
     * @return
     */
    public static Field getOrderByField(Object obj) {
        Field orderbyField = null;
        Class<?> clazz = obj instanceof Class<?> ? (Class) obj : obj.getClass();
        while (!clazz.equals(Object.class)) {
            Optional<Field> optional =
                    ReflectionUtils.getFields(clazz, ReflectionUtils.withAnnotation(MyOrderBy.class)).stream().findFirst();
            if (optional.isPresent()) {
                orderbyField = optional.get();
                orderbyField.setAccessible(true);
                break;
            }
            clazz = clazz.getSuperclass();
        }
        return orderbyField;
    }

    /**
     * 根据Ids删除数据
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public String deleteByIds(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        if (obj instanceof AbstractDataDomain && MybatisContext.isLogicDelete()) {
            assembly(AbstractDataDomain.class.cast(obj), 1);
            sql.UPDATE(table.value());
            sql.SET(String.format("`%s` = #{%s}", "update_time", "updateTime"));
            sql.SET(String.format("`%s` = #{%s}", "update_by", "updateBy"));
            sql.SET(String.format("`%s` = 1", "del_flag"));
        } else {
            sql.DELETE_FROM(table.value());
        }
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        String idColValue = String.valueOf(idField.get(obj));
        List<String> idList = Arrays.stream(idColValue.split(","))
                .map(t -> "'" + t + "'").collect(Collectors.toList());
        sql.WHERE("`" + idColName + "` in (" + String.join(",", idList) + ")");
        logger.debug("deleteByIds sql is {}", sql.toString());
        return sql.toString();
    }

    public String selectByIds(Object obj) throws Exception {
        SQL sql = new SQL();
        MyTable table = getMyTable(obj);
        sql.SELECT(getAllFieldString(obj));
        sql.FROM(table.value());
        Field idField = getIdField(obj);
        MyId id = idField.getAnnotation(MyId.class);
        String idColName = StringUtils.isBlank(id.value()) ? idField.getName() : id.value();
        String idColValue = String.valueOf(idField.get(obj));
        List<String> idList = Arrays.stream(idColValue.split(","))
                .map(t -> "'" + t + "'").collect(Collectors.toList());
        sql.WHERE("`" + idColName + "` in (" + String.join(",", idList) + ")");
        logger.debug("selectByIds sql is {}", sql.toString());
        return sql.toString();
    }

    /**
     * 组装模糊匹配的查询
     *
     * @param obj
     * @param sql
     * @throws Exception
     */
    public static void assemblyVague(String paramAlias, Object obj, SQL sql) throws Exception {
        List<Field> fields = getAllField(obj);
        fields = fields.stream().sorted(getFieldComparator()).collect(Collectors.toList());
        for (Field field : fields) {
            field.setAccessible(true);
            Object colValue = field.get(obj);
            MyColumn column = field.getAnnotation(MyColumn.class);
            if (column != null && colValue != null) {
                if ((colValue instanceof String) && StringUtils.isBlank(String.valueOf(colValue)))
                    continue;
                String colName = StringUtils.isBlank(column.value()) ? field.getName() : column.value();
                if (column.accuracy()) {
                    sql.WHERE(String.format("`%s`= #{%s%s}", colName, StringUtils.isNotEmpty(paramAlias) ? paramAlias + "." : "", field.getName()));
                } else {
                    sql.WHERE(String.format("`%s` like %s", colName, "'%" + String.valueOf(colValue) + "%'"));
                }
            }
        }
    }

    /**
     * 组装 order by
     *
     * @param obj
     * @param sql
     * @throws Exception
     */
    public static void assemblyOrderBy(Object obj, SQL sql) throws Exception {
        Field orderField = getOrderByField(obj);
        if (orderField != null) {
            Object order = orderField.get(obj);
            if (order != null) {
                String orderby[] = String.valueOf(order).split("\\s+");
                if (orderby.length == 2) {
                    String field = orderby[0];
                    try {
                        Field temp = ReflectionUtils.getAllFields(obj.getClass(), ReflectionUtils.withName(field)).iterator().next();
                        temp.setAccessible(true);
                        MyColumn column = temp.getAnnotation(MyColumn.class);
                        if (column != null) {
                            if (StringUtils.isEmpty(column.value()))
                                sql.ORDER_BY(String.valueOf(order));
                            else
                                sql.ORDER_BY(column.value() + " " + orderby[1]);
                        } else
                            sql.ORDER_BY(String.valueOf(order));
                    } catch (Exception e) {
                        sql.ORDER_BY(String.valueOf(order));
                    }
                } else {
                    sql.ORDER_BY(String.valueOf(order));
                }
            }
        }
    }

    /**
     * 组装精确匹配的查询
     *
     * @param obj
     * @param sql
     * @throws Exception
     */
    public static void assemblyAccuracy(String paramAlias, Object obj, SQL sql) throws Exception {
        for (Field field : getAllField(obj)) {
            field.setAccessible(true);
            Object colValue = field.get(obj);
            MyColumn column = field.getAnnotation(MyColumn.class);
            if (column != null && colValue != null) {
                if ((colValue instanceof String) && StringUtils.isBlank(String.valueOf(colValue)))
                    continue;
                String colName = StringUtils.isBlank(column.value()) ? field.getName() : column.value();
                sql.WHERE(String.format("`%s` = #{%s%s}", colName, StringUtils.isNotEmpty(paramAlias) ? paramAlias + "." : "", field.getName()));
            }
        }
    }

    /**
     * 带limit的精确查询
     *
     * @param map
     * @return
     * @throws Exception
     */
    public static String seleteAccuracyLimit(Map map) throws Exception {
        Object obj = map.get("t");
        long[] limits = long[].class.cast(map.get("limits"));
        String sql = BaseSqlProvider.seleteAccuracyParamAlias("t", obj);
        if (limits != null && limits.length > 0) {
            if (limits.length >= 2) {
                sql = String.format("%s limit %s,%s", sql, limits[0], limits[1]);
            } else {
                sql = String.format("%s limit %s", sql, limits[0]);
            }
        }
        return sql;
    }

    /**
     * 带limit的模糊查询
     *
     * @param map
     * @return
     * @throws Exception
     */
    public static String seleteVagueLimit(Map map) throws Exception {
        Object obj = map.get("t");
        long[] limits = long[].class.cast(map.get("limits"));
        String sql = BaseSqlProvider.seleteVagueParamAlias("t", obj);
        if (limits != null && limits.length > 0) {
            if (limits.length >= 2) {
                sql = String.format("%s limit %s,%s", sql, limits[0], limits[1]);
            } else {
                sql = String.format("%s limit %s", sql, limits[0]);
            }
        }
        return sql;
    }

    /**
     * field的比较器
     * 精确查找和带索引的字段在前面
     *
     * @return
     */
    public static Comparator<Field> getFieldComparator() {
        Comparator<Field> comparator = (f1, f2) -> {
            MyColumn col1 = f1.getAnnotation(MyColumn.class);
            MyId id1 = f1.getAnnotation(MyId.class);
            MyColumn col2 = f2.getAnnotation(MyColumn.class);
            MyId id2 = f2.getAnnotation(MyId.class);
            if (col1 != null && col2 != null) {
                return col1.accuracy() ? -1 : col2.accuracy() ? 0 : 1;
            }
            if (col1 == null && id1 != null && col2 != null) {
                return col2.accuracy() ? 1 : -1;
            }
            if (col1 != null && col2 == null && id2 != null) {
                return col1.accuracy() ? -1 : 1;
            }
            return 0;
        };
        return comparator;
    }

    /**
     * 直接返回SQL
     *
     * @param sql
     * @return
     */
    public static String sql(String sql) {
        return sql;
    }

    /**
     * 直接返回SQL,添加 limit 1
     *
     * @param sql
     * @return
     */
    public static String sqlOne(String sql) {
        return sql.trim().endsWith("limit 1") ? sql : sql + " limit 1";
    }

    /**
     * 带问号的参数，示例
     * select * from a where id = ? or id = ?
     *
     * @param map 参数
     * @return
     */
    public static String sqlArgs(Map map) {
        String sql = String.valueOf(map.get("sql"));
        Object[] args = Object[].class.cast(map.get("args"));
        for (int i = 0; i < args.length; i++) {
            sql = sql.replaceFirst("\\?", String.format("#{args[%s]}", i));
        }
        return sql;
    }

    /**
     * 返回参数是SQL的
     *
     * @param map
     * @return
     */
    public static String sqlParam(Map map) {
        return String.valueOf(map.get("sql"));
    }

    /**
     * 返回参数是SQL的
     *
     * @param map
     * @return
     */
    public static String sqlParamOne(Map map) {
        String sql = sqlParam(map);
        return sql.endsWith("limit 1") ? sql : String.format("%s limit 1", sql);
    }

    /**
     * 带问号的参数，示例
     * select * from a where id = ? or id = ?
     *
     * @param map 参数
     * @return sql语句
     */
    public static String sqlArgsOne(Map map) {
        String sql = sqlArgs(map).trim();
        return sql.endsWith("limit 1") ? sql : String.format("%s limit 1", sql);
    }
}


