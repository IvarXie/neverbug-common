package com.baj.newretail.common.mybatis.domain;


import com.baj.newretail.common.mybatis.annotation.MyColumn;
import com.baj.newretail.common.mybatis.annotation.MyId;

import java.util.Date;

/**
 * 基础类的实体
 * ID 创建者修改者，创建时间修改时间等
 * `id`					varchar(128)  primary key COMMENT '工程构建的ID',
 * `create_by`             VARCHAR(128)  COMMENT '创建者ID',
 * `update_by`     		VARCHAR(128)  COMMENT '修改者ID',
 * `create_time` 			TIMESTAMP 	  DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 * `update_time` 			TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 * `del_flag`				SMALLINT(1)   DEFAULT 0  COMMENT '0表示未删除，1表示已经删除'
 *
 * @author neverbug
 * Created on 2018/5/7 9:30
 */
public abstract class AbstractDataDomain extends AbstractOrderBy {
    /**
     * 实体的ID
     */
    @MyId("id")
    private String id;
    /**
     * 创建者ID
     */
    @MyColumn("create_by")
    private String createBy;
    /**
     * 修改者ID
     */
    @MyColumn("update_by")
    private String updateBy;
    /**
     * 创建时间
     */
    @MyColumn("create_time")
    private Date createTime;
    /**
     * 修改时间
     */
    @MyColumn("update_time")
    private Date updateTime;
    /**
     * 删除的标志位,只支持精确查找
     */
    @MyColumn(value = "del_flag", accuracy = true)
    private Boolean delFlag = false;

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Boolean delFlag) {
        this.delFlag = delFlag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
