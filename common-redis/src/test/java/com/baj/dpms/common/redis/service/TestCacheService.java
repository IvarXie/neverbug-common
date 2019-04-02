package com.baj.dpms.common.redis.service;

import com.baj.dpms.common.redis.domain.CacheDomain;

/**
 * Title: 缓存Test
 * Description: </p>
 * Email is Wenbo.Xie@b-and-qchina.com<
 * Company: http://www.bthome.com
 *
 * @author xie.wenbo
 * @date 2019-04-02 11:09
 */
public interface TestCacheService {

    CacheDomain insert(CacheDomain cacheDomain);

    void delete(Long id);

    CacheDomain update(CacheDomain cacheDomain);

    CacheDomain query(Long id);

}
