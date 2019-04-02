package com.neverbug.common.utils.gid;



import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xie.wenbo
 *  生成指定长度的序号
 */
public class SN {
	/**
	 * 长度与最大取值的映射
	 * 长度的取值范围: [1, 18]
	 */
	private final Map<Integer, Long> map = new HashMap<>();

	private long sequence = 0L;

	private final int max_len = 18;

	private SN() {
		long maxVal = 1L;
		for (int len = 1; len <= max_len; ++len) {
			maxVal *= 10;
			map.put(len, maxVal);
		}
	}

	private static class Singleton {
		private static final SN INSTANCE = new SN();
	}

	/**
	 * 获取新的依次递增的SN
	 * 
	 * @param len
	 *            SN长度，长度区间为: [1,18]
	 * @return
	 */
	public static String nextSn(final int len) {
		return Singleton.INSTANCE.getNextSn(len);
	}

	/**
	 * 获取新的随机SN
	 * 
	 * @param len
	 *            RSN长度，长度区间为: [1,18]
	 * @return
	 */
	public static String nextRsn(final int len) {
		return Singleton.INSTANCE.getNextRndSn(len);
	}

	/**
	 * 获取新的随机码
	 * 
	 * @param len
	 *            随机码长度，必须>0
	 * @return
	 */
	public static String nextRc(final int len) {
		return Singleton.INSTANCE.getNextRndCode(len);
	}

	/**
	 * 根据要求的长度，产生依次递增的SN
	 * @param len 长度
	 * @return
	 */
	private synchronized String getNextSn(int len) {
		// 不产生序号0
		if (++sequence >= map.get(len)) {
			sequence = 1L;
		}

		return String.format("%0" + len + "d", sequence);
	}

	/**
	 * 根据要求的长度，产生随机的SN
	 * @param len 长度
	 * @return
	 */
	private synchronized String getNextRndSn(int len) {
		return String.format("%0" + len + "d", RandomUtils.nextLong(1, map.get(len)));
	}

	/**
	 * 根据要求的长度，生成随机码
	 * @param len 长度
	 * @return
	 */
	private synchronized String getNextRndCode(int len) {
		return RandomStringUtils.randomAlphanumeric(len).replaceAll("O", "0").replaceAll("l", "L");
	}

}
