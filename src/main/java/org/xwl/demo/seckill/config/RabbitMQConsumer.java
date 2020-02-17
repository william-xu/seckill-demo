package org.xwl.demo.seckill.config;


import java.io.IOException;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwl.demo.seckill.constant.MQConstant;
import org.xwl.demo.seckill.constant.SeckillStateEnum;
import org.xwl.demo.seckill.dto.SeckillMsgBody;
import org.xwl.demo.seckill.exception.SeckillException;
import org.xwl.demo.seckill.service.SeckillService;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@Component
public class RabbitMQConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @Resource
    private SeckillService seckillService;
    @Resource
    private RabbitMQChannelManager mqChannelManager;

    public void receive() {
        Channel channel = mqChannelManager.getReceiveChannel();
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body)
                    throws IOException {

                String msg = new String(body, "UTF-8");
                logger.info("[mqReceive]  '" + msg + "'");
                SeckillMsgBody msgBody = JSON.parseObject(msg, SeckillMsgBody.class);
                try {
                    seckillService.doUpdateStock(msgBody.getSeckillId(), msgBody.getUserPhone());
                    logger.info("---->ACK");
                    channel.basicAck(envelope.getDeliveryTag(), false);

                } catch (SeckillException seckillE) {
                    if (seckillE.getSeckillStateEnum() == SeckillStateEnum.REPEAT_KILL
                            || seckillE.getSeckillStateEnum() == SeckillStateEnum.END) {
                        // SeckillStateEnum.REPEAT_KILL说明数据库中已经保存了记录，这样的多余消息，应该消费掉并应答队列
                        // 秒杀活动时间已经结束的，也不需要更新数据库
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    } else {
                        logger.error(seckillE.getMessage(), seckillE);
                        logger.info("---->NACK--error_requeue!!!");
                        channel.basicNack(envelope.getDeliveryTag(), false, true);
                    }
                }
            }
        };

        try {
            channel.basicConsume(MQConstant.QUEUE_NAME_SECKILL, false, consumer);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
