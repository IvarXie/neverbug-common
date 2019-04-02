package com.neverbug.common.redis.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;

/**
 * Title: CacheConfig
 * Description: </p>
 * Email is Wenbo.Xie@b-and-qchina.com<
 * Company: http://www.bthome.com
 *
 * @author xie.wenbo
 * @date 2019-04-02 11:36
 */
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {
//    @SuppressWarnings("rawtypes")
//    @Bean
//    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
//        //通过Spring提供的RedisCacheConfiguration类，构造一个自己的redis配置类，从该配置类中可以设置一些初始化的缓存命名空间
//        // 及对应的默认过期时间等属性，再利用RedisCacheManager中的builder.build()的方式生成cacheManager：
//        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();  // 生成一个默认配置，通过config对象即可对缓存进行自定义配置
//        config = config.entryTtl(Duration.ofMinutes(1))     // 设置缓存的默认过期时间，也是使用Duration设置
//                .disableCachingNullValues();     // 不缓存空值
//
//        // 设置一个初始化的缓存空间set集合
//        Set<String> cacheNames = new HashSet<>();
//        cacheNames.add("my-redis-cache1");
//        cacheNames.add("my-redis-cache2");
//
//        // 对每个缓存空间应用不同的配置
//        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
//        configMap.put("my-redis-cache1", config);
//        configMap.put("my-redis-cache2", config.entryTtl(Duration.ofSeconds(1200)));
//
//        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)     // 使用自定义的缓存配置初始化一个cacheManager
//                .initialCacheNames(cacheNames)  // 注意这两句的调用顺序，一定要先调用该方法设置初始化的缓存名，再初始化相关的配置
//                .withInitialCacheConfigurations(configMap)
//                .build();
//        return cacheManager;
//    }
}
