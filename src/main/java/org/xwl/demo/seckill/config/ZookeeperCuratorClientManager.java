package org.xwl.demo.seckill.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ZookeeperCuratorClientManager {

    @Autowired
    private ZookeeperConfig zkConfigBean;

    private final ThreadLocal<CuratorFramework> localLock = new ThreadLocal<CuratorFramework>() {
        @Override
        protected CuratorFramework initialValue() {
            return newClient();
        }
    };

    private CuratorFramework newClient(){
        RetryPolicy retry = new ExponentialBackoffRetry(1000, 3);
        return CuratorFrameworkFactory.builder().connectString(zkConfigBean.getConnectStr())
                .retryPolicy(retry)
                .sessionTimeoutMs(zkConfigBean.getSessionTimeout())
                .connectionTimeoutMs(zkConfigBean.getConnectTimeout())
                .build();
    }

    public CuratorFramework getClient() {
        CuratorFramework client = localLock.get();
        if (client == null) {
            client = newClient();
            localLock.set(client);
        }
        return client;
    }

    public void setClient(CuratorFramework client) {
        localLock.set(client);
    }
}
