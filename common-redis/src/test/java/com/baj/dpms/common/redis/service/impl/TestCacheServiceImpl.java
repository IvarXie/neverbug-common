package com.baj.dpms.common.redis.service.impl;

import com.baj.dpms.common.redis.domain.CacheDomain;
import com.baj.dpms.common.redis.service.TestCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


/**
 * Title: TestCacheServiceImpl
 * Description: </p>
 * Email is Wenbo.Xie@b-and-qchina.com<
 * Company: http://www.bthome.com
 *
 * @author xie.wenbo
 * @date 2019-04-02 14:02
 */
@Service
public class TestCacheServiceImpl implements TestCacheService {

    @Override
    @CachePut(cacheNames = "cache_test", key="#cacheDomain.id")
    public CacheDomain insert(CacheDomain cacheDomain) {
        System.out.println("cache_test insert :"+cacheDomain);
        return cacheDomain;
    }

    @Override
    @CachePut(cacheNames = "cache_test", key="#cacheDomain.id")
    public CacheDomain update(CacheDomain cacheDomain) {
        System.out.println("cache_test update :"+cacheDomain);
        return cacheDomain;
    }

    @Override
    @Cacheable(cacheNames = "cache_test", key="#id")
    public CacheDomain query(Long id){
        System.out.println("cache_test query :"+id);
        return null;
    }

    @Override
    @CacheEvict(cacheNames="cache_test", key="#id")
    public void delete(Long id) {
        System.out.println(" cache_test delete :"+id);
    }
}
