package com.neverbug.common.utils.http;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Title: http请求工具</p>
 * <p>Description: </p>
 * <p>Email is Wenbo.Xie@b-and-qchina.com</p>
 * <p>Company: http://www.bnq.com.cn</p>
 *
 * @author xie.wenbo
 * @date 2018-12-27 15:04
 */
public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    /**
     * 请求编码 默认UTF-8
     */
    private String requestCharset = "UTF-8";
    /**
     * 响应编码 默认UTF-8
     */
    private String responseCharset = "UTF-8";
    /**
     * 请求方式 默认get
     */
    private HttpMethod httpMethod = HttpMethod.GET;
    /**
     * 请求url
     */
    private String url;
    /**
     * 请求头
     */
    private List<Header> headers = new ArrayList<>();
    /**
     * 请求参数
     */
    private Map<String,String> params = new HashMap<>();
    /**
     * 请求配置（超时时间等配置）
     */
    private RequestConfig requestConfig;
    /**
     * httpClient
     */
    private CloseableHttpClient httpClient;
    /**
     * entity
     */
    private HttpEntity entity;
    /**
     * @author xie.wenbo
     * @date Created on 2018-12-27 17:48
     * @Description 获取具体的请求对象
     * @return org.apache.http.client.methods.HttpRequestBase
     */
    private HttpRequestBase getHttpRequest(){
        HttpRequestBase httpRequestBase=null;
        if (StringUtils.isBlank(url)){
            throw new NullPointerException("url can't be null");
        }
        if(httpMethod== HttpMethod.GET){
            httpRequestBase = new HttpGet(url);
        }else if(httpMethod== HttpMethod.POST){
            httpRequestBase = new HttpPost(url);
            if(entity!=null){
                ((HttpPost) httpRequestBase).setEntity(entity);
            }
        }else if(httpMethod== HttpMethod.PUT){
            httpRequestBase = new HttpPut(url);
        }else if(httpMethod== HttpMethod.PATCH){
            httpRequestBase = new HttpPatch(url);
        }else if(httpMethod== HttpMethod.DELETE){
            httpRequestBase = new HttpDelete(url);
        }else if(httpMethod== HttpMethod.HEAD){
            httpRequestBase = new HttpHead(url);
        }else if(httpMethod== HttpMethod.OPTIONS){
            httpRequestBase = new HttpOptions(url);
        }
        return httpRequestBase;
    }
    /**
     * @author xie.wenbo
     * @date Created on 2018-12-27 17:49
     * @Description 设置参数
     * @param httpRequestBase 请求对象,不同请求类型设置参数方式不同
     * @return void
     */
    private void settingParams(HttpRequestBase httpRequestBase) {
        //判断是否支持设置entity(仅HttpPost、HttpPut、HttpPatch支持)
        if (HttpEntityEnclosingRequestBase.class.isAssignableFrom(Objects.requireNonNull(httpRequestBase).getClass())) {
            StringEntity entity = null;
            if (params.containsKey(ContentType.APPLICATION_JSON.getMimeType())) {
                entity = new StringEntity(params.get(ContentType.APPLICATION_JSON.getMimeType()), requestCharset);
                //设置参数到请求对象中
                ((HttpEntityEnclosingRequestBase) httpRequestBase).setEntity(entity);
            } else {
                try {
                    entity = new UrlEncodedFormEntity(params.entrySet().stream().
                            map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList()), requestCharset);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            ((HttpEntityEnclosingRequestBase) httpRequestBase).setEntity(entity);
        } else {
            try {
                URIBuilder uriBuilder = new URIBuilder(url);
                uriBuilder.addParameters(params.entrySet().stream().
                        map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
                httpRequestBase.setURI(uriBuilder.build());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * @author xie.wenbo
     * @date Created on 2018-12-27 17:50
     * @Description 发起请求
     * @return java.lang.String
     */
    public String execute(){
        return execute(null);
    }
    /**
     * @author xie.wenbo
     * @date Created on 2018-12-27 17:50
     * @Description 发起请求
     * @return java.lang.String
     */
    public String execute(HttpResponseProcess httpResponseProcess){
        if(httpClient==null){
            throw new NullPointerException("httpClient can't be null");
        }
        HttpRequestBase httpRequest = getHttpRequest();
        if(params!=null&&params.size()>0) {
            settingParams(httpRequest);
        }
        if(requestConfig!=null){
            httpRequest.setConfig(requestConfig);
        }
        if(headers!=null){
            httpRequest.setHeaders(headers.toArray(new Header[]{}));
        }
        String json=null;
        CloseableHttpResponse response=null;
        try {
            response = httpClient.execute(httpRequest);
            logger.info("```` HttpUtil execute url:{} \r\n params:{} \n headers:{} \n response:{}",url,params,headers,JSONObject.toJSONString(response));
            if(httpResponseProcess!=null){
                httpResponseProcess.process(response);
            }else{
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    json = EntityUtils.toString(entity, responseCharset);
                }
                Objects.requireNonNull(response.getEntity()).getContent().close();
            }
        } catch (UnsupportedOperationException | IOException e) {
            e.printStackTrace();
        } finally {
            if(response!=null){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return json;
    }


    public HttpUtil(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getRequestCharset() {
        return requestCharset;
    }

    public void setRequestCharset(String requestCharset) {
        this.requestCharset = requestCharset;
    }

    public String getResponseCharset() {
        return responseCharset;
    }

    public void setResponseCharset(String responseCharset) {
        this.responseCharset = responseCharset;
    }

    public String getUrl() {
        return url;
    }

    public HttpUtil setUrl(String url) {
        this.url = url;
        return this;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public HttpUtil addHeader(String key, String value) {
        headers.add(new BasicHeader(key, value));
        return this;
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    public void setRequestConfig(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public HttpUtil setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }
    public HttpUtil setParams(Map params){
        this.params = params;
        return this;
    }

    public HttpUtil addParams(String key, String value){
        params.put(key,value);
        return this;
    }

    public Map<String, String> getParams() {
        return params;
    }
    public HttpUtil setJson(String json){
        params.put(ContentType.APPLICATION_JSON.getMimeType(),json);
        return this;
    }
    public HttpUtil setJsonObject(JSONObject jsonObject){
        return setJson(jsonObject.toJSONString());
    }

    public HttpUtil setEntity(HttpEntity entity) {
        this.entity = entity;
        return this;
    }
}
