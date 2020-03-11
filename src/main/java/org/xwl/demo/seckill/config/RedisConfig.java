package org.xwl.demo.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

	@Bean
	public RedissonClient redisson(RedisProperties redisProperties) {
		Config config = new Config();
		config.useSingleServer().setAddress(redisProperties.getUrl()).setDatabase(redisProperties.getDatabase());
		RedissonClient redissonClient = Redisson.create(config);
		return redissonClient;
	}

}
