package com.baj.newretail.common.mybatis.service;

import com.baj.newretail.common.mybatis.mapper.ExecuteMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 执行SQL的service
 * 添加@Primary注解，注入的时候使用单一使用这个
 *
 * @author neverbug
 * Created on 2018/5/26 12:48
 */
@Primary
@Service
@SuppressWarnings("all")
public class ExecuteService {
    /**
     * slf4j日志
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 执行SQL的executeMapper
     */
    @Autowired(required = false)
    private ExecuteMapper executeMapper;

    /**
     * 执行update语句
     *
     * @param sql
     * @param args
     * @return
     */
    @Transactional
    public long executeUpdate(String sql, Object... args) {
        return args.length == 0 ? executeMapper.executeUpdate(sql) : executeMapper.executeUpdateArgs(sql, args);
    }


    /**
     * 执行delete语句
     *
     * @param sql
     * @param args
     * @return
     */
    @Transactional
    public long executeDelete(String sql, Object... args) {
        return args.length == 0 ? executeMapper.executeDelete(sql) : executeMapper.executeDeleteArgs(sql, args);
    }

    /**
     * 执行insert语句
     *
     * @param sql
     * @param args
     * @return
     */
    @Transactional
    public long executeInsert(String sql, Object... args) {
        return args.length == 0 ? executeMapper.executeInsert(sql) : executeMapper.executeInsertArgs(sql, args);
    }


    /**
     * sql带参数查询单条
     *
     * @param sql
     * @return
     */
    @Transactional(readOnly = true)
    public <E> E selectOne(String sql, Class<E> clazz, Object... args) {
        try {
            return assemblyOne(selectOneMap(sql, args), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询单个Map
     *
     * @param sql
     * @param args
     * @return
     */
    @Transactional(readOnly = true)
    public Map<String, Object> selectOneMap(String sql, Object... args) {
        return args.length == 0 ? executeMapper.selectOneMap(sql) : executeMapper.selectOneMapArgs(sql, args);
    }

    /**
     * @param sql
     * @param obj
     * @return
     */
    @Transactional(readOnly = true)
    public Map<String, Object> selectParamOneMap(String sql, Object obj) {
        return executeMapper.selectParamOneMap(sql, obj);
    }

    /**
     * 查询自定义的实体
     *
     * @param sql
     * @param clazz
     * @param args
     * @param <E>
     * @return
     */
    @Transactional(readOnly = true)
    public <E> List<E> select(String sql, Class<E> clazz, Object... args) {
        List<Map<String, Object>> list = selectMap(sql, args);
        List<E> collection = Lists.newArrayList();
        try {
            for (Map<String, Object> map : list) {
                collection.add(assemblyOne(map, clazz));
            }
            return collection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * select * from a where id = #{param.id}
     *
     * @param sql
     * @param clazz
     * @param obj
     * @param <E>
     * @return
     */
    public <E> List<E> selectParam(String sql, Class<E> clazz, Object obj) {
        List<Map<String, Object>> list = selectParamMap(sql, obj);
        List<E> collection = Lists.newArrayList();
        try {
            for (Map<String, Object> map : list) {
                collection.add(assemblyOne(map, clazz));
            }
            return collection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <E> E selectParamOne(String sql, Class<E> clazz, Object obj) {
        try {
            return assemblyOne(selectParamOneMap(sql, obj), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SQL查询Map的集合
     *
     * @return
     */
    public List<Map<String, Object>> selectMap(String sql, Object... args) {
        return args.length == 0 ? executeMapper.selectMap(sql) : executeMapper.selectMapArgs(sql, args);
    }

    /**
     * SQL查询Map的集合
     *
     * @return
     */
    public List<Map<String, Object>> selectParamMap(String sql, Object obj) {
        return executeMapper.selectParamMap(sql, obj);
    }


    /**
     * 自定义SQL的分页查询
     *
     * @param clazz
     * @param pageNum
     * @param pageSize
     * @param sql
     * @param args
     * @param
     * @return
     */
    @Transactional(readOnly = true)
    public <E> PageInfo<E> pageSQL(Class<E> clazz, int pageNum, int pageSize, String sql, Object... args) {
        PageHelper.startPage(pageNum, pageSize);
        List<E> collection = select(sql, clazz, args);
        return new PageInfo<>(collection);
    }

    /**
     * 根据class组装实例
     *
     * @param map
     * @param clazz
     * @param <E>
     * @return
     * @throws Exception
     */
    @SuppressWarnings("all")
    public <E> E assemblyOne(Map<String, Object> map, Class<E> clazz) throws Exception {
        if (map == null) {
            return null;
        }
        E e = clazz.newInstance();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            Set<Field> set = ReflectionUtils.getFields(clazz, ReflectionUtils.withName(entry.getKey()));
            for (Field f : set) {
                f.setAccessible(true);
                try {
                    f.set(e, entry.getValue());
                } catch (Exception ee) {
                    if (value != null) {
                        try {
                            /*如果是String类型的，转string*/
                            if (String.class.equals(f.getType())) {
                                f.set(e, String.valueOf(value));
                            } else if (boolean.class.equals(f.getType()) || Boolean.class.equals(f.getType())) {
                                /*boolean类型的true和false的字符串转，还有int 的0和1互转*/
                                if (value instanceof String && ("true".equals(String.valueOf(value).trim()) || "false".equals(String.valueOf(value).trim()))) {
                                    f.set(e, Boolean.valueOf(String.valueOf(value)));
                                } else if (value instanceof Integer || value instanceof Long) {
                                    f.set(e, Integer.parseInt(String.valueOf(value)) > 0 ? true : false);
                                }
                            } else if (char.class.equals(f.getType()) || Character.class.equals(f.getType())) {
                                /*char 类型的转字符串取第一个字母*/
                                String strValue = String.valueOf(value);
                                if (strValue.length() > 0) {
                                    f.set(e, Character.valueOf(strValue.charAt(0)));
                                }
                            }
                        } catch (Exception e1) {
                        }
                    }
                }
            }
        }
        return e;
    }
}
