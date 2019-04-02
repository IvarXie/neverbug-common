package com.neverbug.common.redis.utils;

import com.neverbug.common.redis.BootTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Title: RedisUtilTest
 * Description: </p>
 * Email is Wenbo.Xie@b-and-qchina.com<
 * Company: http://www.bthome.com
 *
 * @author xie.wenbo
 * @date 2019-04-01 18:44
 */
public class RedisUtilTest extends BootTest {
    private final String redis_key = "TEST";
    private final String set_get_key = redis_key+"-set-get";
    @Autowired
    private RedisUtils redisUtils;

    @Test
    public void test_set_get(){
        int value = new Random().nextInt();
        redisUtils.set(set_get_key,value);
        Object result = redisUtils.get(set_get_key);
        Assert.assertEquals(value,result);
        redisUtils.delete(set_get_key);
    }

    @Test
    public void test_hashKey(){
        String key = redisUtils.randomKey();
        Assert.assertTrue(redisUtils.hasKey(key));
        redisUtils.delete(key);
    }

    @Test
    public void test_set_get_expire(){
        int value = new Random().nextInt();
        int second = 2;
        String key = set_get_key+"-expire";
        redisUtils.set(key,value,second);
        try {
            Thread.sleep(second*1000+100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertFalse(redisUtils.hasKey(key));
    }
    @Test
    public void test_expire(){
        long millisecond = 2000;
        String key = redis_key+"-expire";
        Assert.assertFalse(redisUtils.expire(key,millisecond, TimeUnit.MILLISECONDS));
        try {
            Thread.sleep(millisecond+100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertFalse(redisUtils.hasKey(key));
    }

}
