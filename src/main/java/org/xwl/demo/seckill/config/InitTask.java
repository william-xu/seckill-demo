package org.xwl.demo.seckill.config;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.xwl.demo.seckill.constant.RedisKeyPrefix;
import org.xwl.demo.seckill.dao.SeckillDAO;
import org.xwl.demo.seckill.entity.Seckill;

@Component
public class InitTask implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitTask.class);

    @Autowired
    private RedisConnectionFactory redisConnFactory;
    
    @Resource
    private SeckillDAO seckillDAO;
    @Resource
    private RabbitMQConsumer mqConsumer;

    @Override
    public void run(String... args) throws Exception {
        logger.info("StartToConsumeMsg--->");
        mqConsumer.receive();
        initRedis();
    }

    /**
     * 预热秒杀数据到Redis
     */
    private void initRedis() {
        //清空Redis缓存
    	redisConnFactory.getConnection().flushDb();
        List<Seckill> seckillList = seckillDAO.queryAll(0, 10);
        for (Seckill seckill : seckillList) {
            String nameKey = RedisKeyPrefix.SECKILL_NAME + seckill.getSeckillId();
            String inventoryKey = RedisKeyPrefix.SECKILL_INVENTORY + seckill.getSeckillId();
            redisConnFactory.getConnection().set(nameKey.getBytes(), seckill.getName().getBytes());
            redisConnFactory.getConnection().set(inventoryKey.getBytes(), String.valueOf(seckill.getInventory()).getBytes());
        }
        logger.info("Redis缓存数据初始化完毕！");
    }
}
