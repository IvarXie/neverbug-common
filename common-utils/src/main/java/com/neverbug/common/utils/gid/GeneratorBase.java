package com.neverbug.common.utils.gid;

/**
 * @author guo.guanfei
 *
 *         序列号生成器的抽象基类
 */
abstract class GeneratorBase {

	/* 数据中心ID和节点ID共占10位，共支持部署1024个节点 */
	/**
	 * 数据中心ID位数
	 */
	final long dcIdBits = 1L;
	/**
	 * 工作节点ID位数
	 */
	final long workerIdBits = 10L - dcIdBits;

}
