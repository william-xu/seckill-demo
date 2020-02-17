package org.xwl.demo.seckill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties("zookeeper")
public class ZookeeperConfig {
	
	private String connectStr;
	
	private String lockRoot;

    private int sessionTimeout;

    private int connectTimeout;

    private int lockAcquireTimeout;

}
