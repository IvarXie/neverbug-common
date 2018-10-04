package com.neverbug.util;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import com.neverbug.exception.ErrorCode;

public class JerseyClientUtil {
	// 单例
	private static MyJerseyClient singleton = new MyJerseyClient();

	private JerseyClientUtil() {}
	/**
	 * 通过Jersey Client发送HTTP GET请求
	 * 
	 * @param url
	 *            目标资源URL
	 * 
	 * @return Response对象
	 */
	public static Response httpGet(String url) {
		return singleton.httpGet(url);
	}

	public static Response httpGet(String url, MultivaluedMap<String, Object> headers) {
		return singleton.httpGet(url, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP GET请求
	 * 
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * 
	 * @return Response对象
	 */
	public static Response httpGet(String host, int port, String resource) {
		return singleton.httpGet(getHttpUrl(host, port, resource));
	}
	public static Response httpGet(String host, int port, String resource,
								   MultivaluedMap<String, Object> headers) {
		return singleton.httpGet(getHttpUrl(host, port, resource), headers);
	}

	/**
	 * 通过Jersey Client发送HTTP DELETE请求
	 * 
	 * @param url
	 *            目标资源URL
	 * 
	 * @return Response对象
	 */
	public static Response httpDelete(String url) {
		return singleton.httpDelete(url);
	}
	public static Response httpDelete(String url, MultivaluedMap<String, Object> headers) {
		return singleton.httpDelete(url, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP DELETE请求
	 * 
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * 
	 * @return Response对象
	 */
	public static Response httpDelete(String host, int port, String resource) {
		return singleton.httpDelete(getHttpUrl(host, port, resource));
	}
	public static Response httpDelete(String host, int port, String resource, MultivaluedMap<String, Object> headers) {
		return singleton.httpDelete(getHttpUrl(host, port, resource), headers);
	}

	/**
	 * 通过Jersey Client发送HTTP POST请求，请求数据以Form格式发送
	 * 
	 * @param url
	 *            目标资源URL
	 * @param map
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static Response httpPostForm(String url, Map<String, String> map) {
		return singleton.httpPostForm(url, map);
	}
	public static Response httpPostForm(String url, Map<String, String> map, MultivaluedMap<String, Object> headers) {
		return singleton.httpPostForm(url, map, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP POST请求，请求数据以Form格式发送
	 * 
	 * 主机名或IP
	 * 
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * @param map
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static Response httpPostForm(String host, int port, String resource, Map<String, String> map) {
		return singleton.httpPostForm(getHttpUrl(host, port, resource), map);
	}
	public static Response httpPostForm(String host, int port, String resource, Map<String, String> map, MultivaluedMap<String, Object> headers) {
		return singleton.httpPostForm(getHttpUrl(host, port, resource), map, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP POST请求，请求数据以JSON格式发送
	 * 
	 * @param url
	 *            目标资源URL
	 * @param entity
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static <T> Response httpPostJson(String url, T entity) {
		return singleton.httpPostJson(url, entity);
	}
	public static <T> Response httpPostJson(String url, T entity, MultivaluedMap<String, Object> headers) {
		return singleton.httpPostJson(url, entity, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP POST请求，请求数据以JSON格式发送
	 * 
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * @param entity
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static <T> Response httpPostJson(String host, int port, String resource, T entity) {
		return singleton.httpPostJson(getHttpUrl(host, port, resource), entity);
	}
	public static <T> Response httpPostJson(String host, int port, String resource, T entity, MultivaluedMap<String, Object> headers) {
		return singleton.httpPostJson(getHttpUrl(host, port, resource), entity, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP PUT请求，请求数据以Form格式发送
	 * 
	 * @param url
	 *            目标资源URL
	 * @param map
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static Response httpPutForm(String url, Map<String, String> map) {
		return singleton.httpPutForm(url, map);
	}
	public static Response httpPutForm(String url, Map<String, String> map, MultivaluedMap<String, Object> headers) {
		return singleton.httpPutForm(url, map, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP PUT请求，请求数据以Form格式发送
	 * 
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * @param map
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static Response httpPutForm(String host, int port, String resource, Map<String, String> map) {
		return singleton.httpPutForm(getHttpUrl(host, port, resource), map);
	}
	public static Response httpPutForm(String host, int port, String resource, Map<String, String> map, MultivaluedMap<String, Object> headers) {
		return singleton.httpPutForm(getHttpUrl(host, port, resource), map, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP PUT请求，请求数据以JSON格式发送
	 * 
	 * @param url
	 *            目标资源URL
	 * @param entity
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static <T> Response httpPutJson(String url, T entity) {
		return singleton.httpPutJson(url, entity);
	}
	public static <T> Response httpPutJson(String url, T entity, MultivaluedMap<String, Object> headers) {
		return singleton.httpPutJson(url, entity, headers);
	}

	/**
	 * 通过Jersey Client发送HTTP PUT请求，请求数据以JSON格式发送
	 * 
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * @param entity
	 *            待发送的数据
	 * 
	 * @return Response对象
	 */
	public static <T> Response httpPutJson(String host, int port, String resource, T entity) {
		return singleton.httpPutJson(getHttpUrl(host, port, resource), entity);
	}
	public static <T> Response httpPutJson(String host, int port, String resource, T entity, MultivaluedMap<String, Object> headers) {
		return singleton.httpPutJson(getHttpUrl(host, port, resource), entity, headers);
	}

	/**
	 * 根据主机名、端口和资源路径来产生HTTP URL
	 * 
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * 
	 * @return 资源的HTTP URL
	 */
	private static String getHttpUrl(String host, int port, String resource) {
		return getUrl("http", host, port, resource);
	}

	/**
	 * 根据主机名、端口和资源路径来产生URL (HTTP/HTTPS)
	 * 
	 * @param schema
	 *            http或https
	 * @param host
	 *            主机名或IP
	 * @param port
	 *            端口
	 * @param resource
	 *            资源路径
	 * 
	 * @return 资源的URL
	 */
	private static String getUrl(String schema, String host, int port, String resource) {
		String url = schema + "://" + host + ":" + port;

		if (null == resource) {
			return url;
		}

		String resourcePath = resource.trim();
		if (resourcePath.length() == 0) {
			return url;
		}

		if (resourcePath.startsWith("/")) {
			return url + resourcePath;
		}

		return url + "/" + resourcePath;
	}

	private static class MyJerseyClient {

		/**
		 * 通过Jersey Client发送HTTP GET请求
		 * 
		 * @param url
		 *            目标资源URL
		 * 
		 * @return Response对象
		 */
		Response httpGet(String url) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).get();
			response.bufferEntity();
			return response;
		}
		Response httpGet(String url, MultivaluedMap<String, Object> headers) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON)
					.headers(headers).get();
			response.bufferEntity();
			return response;
		}

		/**
		 * 通过Jersey Client发送HTTP DELETE请求
		 * 
		 * @param url
		 *            目标资源URL
		 * 
		 * @return Response对象
		 */
		Response httpDelete(String url) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).delete();
			response.bufferEntity();
			return response;
		}
		Response httpDelete(String url, MultivaluedMap<String, Object> headers) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).headers(headers).delete();
			response.bufferEntity();
			return response;
		}

		/**
		 * 通过Jersey Client发送HTTP POST请求，请求数据以Form格式发送
		 * 
		 * @param url
		 *            目标资源URL
		 * @param map
		 *            待发送的数据
		 * 
		 * @return Response对象
		 */
		Response httpPostForm(String url, Map<String, String> map) {
			if (null == map || map.size() == 0) {
				return ResponseUtil.getBizErrorResponse(ErrorCode.BIZ_ERROR_PARAMETER_NULL.value(), "POST请求未包含任何数据！");
			}

			Form form = new Form();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				form.param(entry.getKey(), entry.getValue());
			}

			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).post(Entity.form(form));
			response.bufferEntity();
			return response;
		}
		Response httpPostForm(String url, Map<String, String> map, MultivaluedMap<String, Object> headers) {
			if (null == map || map.size() == 0) {
				return ResponseUtil.getBizErrorResponse(ErrorCode.BIZ_ERROR_PARAMETER_NULL.value(), "POST请求未包含任何数据！");
			}

			Form form = new Form();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				form.param(entry.getKey(), entry.getValue());
			}

			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).headers(headers).post(Entity.form(form));
			response.bufferEntity();
			return response;
		}

		/**
		 * 通过Jersey Client发送HTTP POST请求，请求数据以JSON格式发送
		 * 
		 * @param url
		 *            目标资源URL
		 * @param entity
		 *            待发送的数据
		 * 
		 * @return Response对象
		 */
		<T> Response httpPostJson(String url, T entity) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON)
					.post(Entity.json(entity));
			response.bufferEntity();
			return response;
		}
		<T> Response httpPostJson(String url, T entity, MultivaluedMap<String, Object> headers) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON)
					.headers(headers).post(Entity.json(entity));
			response.bufferEntity();
			return response;
		}

		/**
		 * 通过Jersey Client发送HTTP PUT请求，请求数据以Form格式发送
		 * 
		 * @param url
		 *            目标资源URL
		 * 
		 * @param map
		 *            待发送的数据
		 * 
		 * @return Response对象
		 */
		Response httpPutForm(String url, Map<String, String> map) {
			if (null == map || map.size() == 0) {
				return ResponseUtil.getBizErrorResponse(ErrorCode.BIZ_ERROR_PARAMETER_NULL.value(), "POST请求未包含任何数据！");
			}

			Form form = new Form();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				form.param(entry.getKey(), entry.getValue());
			}

			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).put(Entity.form(form));
			response.bufferEntity();
			return response;
		}
		Response httpPutForm(String url, Map<String, String> map, MultivaluedMap<String, Object> headers) {
			if (null == map || map.size() == 0) {
				return ResponseUtil.getBizErrorResponse(ErrorCode.BIZ_ERROR_PARAMETER_NULL.value(), "POST请求未包含任何数据！");
			}

			Form form = new Form();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				form.param(entry.getKey(), entry.getValue());
			}

			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).headers(headers).put(Entity.form(form));
			response.bufferEntity();
			return response;
		}

		/**
		 * 通过Jersey Client发送HTTP PUT请求，请求数据以JSON格式发送
		 * 
		 * @param url
		 *            目标资源URL
		 * @param entity
		 *            待发送的数据
		 * 
		 * @return Response对象
		 */
		<T> Response httpPutJson(String url, T entity) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).put(Entity.json(entity));
			response.bufferEntity();
			return response;
		}
		<T> Response httpPutJson(String url, T entity, MultivaluedMap<String, Object> headers) {
			Client client = ClientBuilder.newClient(new ClientConfig());
			Response response = client.target(url).request(MediaType.APPLICATION_JSON).headers(headers).put(Entity.json(entity));
			response.bufferEntity();
			return response;
		}
	}

}
