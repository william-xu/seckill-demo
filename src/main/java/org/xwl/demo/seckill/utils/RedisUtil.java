package org.xwl.demo.seckill.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Repository;
import org.xwl.demo.seckill.entity.Seckill;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;

@Repository
public class RedisUtil {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    @Resource(name = "initJedisPool")
//    private JedisPool jedisPool;

    @Autowired
    private RedisConnectionFactory redisConnFactory;
    
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public Seckill getSeckill(long seckillId) {
        //redis操作逻辑
        try {
//            Jedis jedis = factory.getConnection().get
            try {
                String key = "seckill:" + seckillId;
                //并没有实现内部序列化操作
                // get-> byte[] -> 反序列化 ->Object(Seckill)
                // 采用自定义序列化
                //protostuff : pojo.
                byte[] bytes = redisConnFactory.getConnection().get(key.getBytes());
                //缓存中获取到bytes
                if (bytes != null) {
                    //空对象
                    Seckill seckill = schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                    //seckill 被反序列化
                    return seckill;
                }
            } finally {
//                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public String putSeckill(Seckill seckill) {
        // set Object(Seckill) -> 序列化 -> byte[]
        try {
//            Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:" + seckill.getSeckillId();
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                //超时缓存
                int timeout = 60 * 60;//1小时
                boolean result = redisConnFactory.getConnection().setEx(key.getBytes(), timeout, bytes);

                return String.valueOf(result);
            } finally {
//                conntion.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
