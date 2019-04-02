package com.neverbug.common.utils.http;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * <p>Title: 处理http请求抽象类</p>
 * <p>Description: </p>
 * <p>Email is Wenbo.Xie@b-and-qchina.com</p>
 * <p>Company: http://www.bnq.com.cn</p>
 *
 * @author xie.wenbo
 * @date 2018-12-27 17:52
 */
public interface HttpResponseProcess {
    /**
     * 处理http请求
     * @param closeableHttpResponse 响应对象
     */
    void process(CloseableHttpResponse closeableHttpResponse);
}
