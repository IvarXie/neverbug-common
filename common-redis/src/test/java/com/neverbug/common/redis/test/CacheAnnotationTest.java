package com.neverbug.common.redis.test;

import com.neverbug.common.redis.BootTest;
import com.neverbug.common.redis.domain.CacheDomain;
import com.neverbug.common.redis.service.TestCacheService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Title: CacheAnnotationTest
 * Description: </p>
 * Email is Wenbo.Xie@b-and-qchina.com<
 * Company: http://www.bthome.com
 *
 * @author xie.wenbo
 * @date 2019-04-02 14:35
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CacheAnnotationTest extends BootTest {

    @Autowired
    private TestCacheService testCacheService;

    @Test
    public void test_1_insert(){
        CacheDomain cacheDomain = new CacheDomain();
        cacheDomain.setId(1L);
        cacheDomain.setName("张三");
        cacheDomain.setAge(18);
        testCacheService.insert(cacheDomain);
    }

    @Test
    public void test_2_update(){
        CacheDomain cacheDomain = new CacheDomain();
        cacheDomain.setId(1L);
        cacheDomain.setName("李四");
        cacheDomain.setAge(19);
        testCacheService.update(cacheDomain);
    }

    @Test
    public void test_3_query(){
        System.out.println(testCacheService.query(1L));
    }

    @Test
    public void test_4_delete(){
        testCacheService.delete(1L);
    }

}
