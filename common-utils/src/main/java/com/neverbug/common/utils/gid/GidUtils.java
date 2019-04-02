package com.neverbug.common.utils.gid;



import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Author xie.wenbo
 * @Description 生成全局id方法
 * @CreationDate: 2018-08-30 14:26
 */
public class GidUtils {

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 14:33
     * @Description SnowFlake算法获取一个全局id(19位)
     * @return java.lang.Long
     */
    public static Long gid() {
        return GID.next();
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 14:33
     * @Description 生成一个全局id(19位)集合
     * @return java.lang.Long
     */
    public static List<Long> gid(int num) throws Exception {
        if (num == 0) {
            throw new Exception("");
        }
        List<Long> gidList = new ArrayList<>(num);
        for (int i = 1; i <= num; i++) {
            gidList.add(GID.next());
        }
        return gidList;
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 14:39
     * @Description 生成一个全局唯一的流水号(24位)
     * @return java.lang.String
     */
    public static String gsn() {
        return GSN.next();
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 14:41
     * @Description 生成一个指定长度的随机序号
     * @param len 长度 允许范围: [1,18]
     * @return java.lang.String
     */
    public static String rsn(int len) throws RuntimeException {
        if (len < 1 || len > 18) {
            throw new RuntimeException("错误的RSN长度，允许范围: [1,18]");
        }
        return SN.nextRsn(len);
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 14:41
     * @Description 生成一个指定长度的依次递增的序号
     * @param len 长度 允许范围: [1,18]
     * @return java.lang.String
     */
    public static String sn(int len) throws RuntimeException {
        if (len < 1 || len > 18) {
            throw new RuntimeException("错误的SN长度，允许范围: [1,18]");
        }
        return SN.nextSn(len);
    }

    /**
     * @author xie.wenbo
     * @date Created on 2018/8/30 14:43
     * @Description 生成一个指定长度的随机码
     * @param len 长度 允许范围: [1,∞)
     * @return java.lang.String
     */
    public static String rc(int len) throws RuntimeException {
        if (len < 1) {
            throw new RuntimeException("错误的RC长度，允许范围: [1,∞)");
        }
        return SN.nextRc(len);
    }

    /**
     * <p>
     * 获取去掉"-" UUID
     * </p>
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
