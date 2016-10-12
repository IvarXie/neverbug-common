package com.jyall.util;

import org.springframework.context.ApplicationContext;

public class SpringUtil {
	private static ApplicationContext ctx;

	private SpringUtil() {}

	public static void setApplicationContext(ApplicationContext applicationContext) {
		// TODO Auto-generated method stub
		ctx = applicationContext;
	}

	/**
	 * 根据名称获取Bean
	 * 
	 * @param name
	 *            Bean的名称
	 * 
	 * @return 获取到的Bean对象，类型为Object
	 */
	public static Object getBean(String name) {
		if (null == ctx) {
			throw new NullPointerException("ApplicationContext is null");
		}

		return ctx.getBean(name);
	}

	/**
	 * 根据类型获取Bean
	 * 
	 * @param clazz
	 *            Bean类型
	 * 
	 * @return 获取到的Bean对象
	 */
	public static <T> T getBean(Class<T> clazz) {
		if (null == ctx) {
			throw new NullPointerException("ApplicationContext is null");
		}
		return ctx.getBean(clazz);
	}

}
