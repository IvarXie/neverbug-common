package com.baj.newretail.common.mybatis.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 执行SQL的mapper
 * 实体mapepr继承了该mapper
 *
 * @create by neverbug
 * @create on 2017年4月26日下午3:34:22
 */
public interface ExecuteBaseMapper {

    /**
     * 执行查询返回map
     *
     * @param sql
     * @return
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "sqlOne")
    Map<String, Object> selectOneMap(String sql);

    /**
     * @param sql select * from a where id = #{param.id}
     * @param obj
     * @return
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "sqlParam")
    List<Map<String, Object>> selectParamMap(@Param("sql") String sql, @Param("param") Object obj);


    /**
     * @param sql select * from a where id = #{param.id} limit 1
     * @param obj
     * @return
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "sqlParamOne")
    Map<String, Object> selectParamOneMap(@Param("sql") String sql, @Param("param") Object obj);

    /**
     * 执行查询返回map
     *
     * @param sql
     * @param args
     * @return
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "sqlArgsOne")
    Map<String, Object> selectOneMapArgs(@Param("sql") String sql, @Param("args") Object... args);

    /**
     * sql查询 Map 列表
     *
     * @param sql
     * @return
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "sql")
    List<Map<String, Object>> selectMap(String sql);


    /**
     * sql查询 Map 列表
     *
     * @param sql
     * @return
     */
    @SelectProvider(type = BaseSqlProvider.class, method = "sqlArgs")
    List<Map<String, Object>> selectMapArgs(@Param("sql") String sql, @Param("args") Object... args);

    /**
     * 执行update语句
     *
     * @param sql
     * @return
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "sql")
    long executeUpdate(String sql);

    /**
     * 执行update语句
     *
     * @param sql
     * @param args
     * @return
     */
    @UpdateProvider(type = BaseSqlProvider.class, method = "sqlArgs")
    long executeUpdateArgs(@Param("sql") String sql, @Param("args") Object... args);

    /**
     * 执行delete语句
     *
     * @param sql
     * @return
     */
    @DeleteProvider(type = BaseSqlProvider.class, method = "sql")
    long executeDelete(String sql);

    /**
     * 执行delete语句
     *
     * @param sql
     * @param args
     * @return
     */
    @DeleteProvider(type = BaseSqlProvider.class, method = "sqlArgs")
    long executeDeleteArgs(@Param("sql") String sql, @Param("args") Object... args);

    /**
     * 执行insert语句
     *
     * @param sql
     * @return
     */
    @InsertProvider(type = BaseSqlProvider.class, method = "sql")
    long executeInsert(String sql);

    /**
     * 执行insert语句
     *
     * @param sql
     * @param args
     * @return
     */
    @InsertProvider(type = BaseSqlProvider.class, method = "sqlArgs")
    long executeInsertArgs(@Param("sql") String sql, @Param("args") Object... args);

}
