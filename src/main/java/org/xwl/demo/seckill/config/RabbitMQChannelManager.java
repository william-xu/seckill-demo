package org.xwl.demo.seckill.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xwl.demo.seckill.constant.MQConstant;

import com.rabbitmq.client.Channel;

/**
 * 管理当前线程使用的Rabbitmq通道.
 * 使用了ThreadLocal
 */
@Component
public class RabbitMQChannelManager {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQChannelManager.class);
    @Autowired
    private ConnectionFactory connFactory;

    private ThreadLocal<Channel> localSendChannel = new ThreadLocal<Channel>() {
        public Channel initialValue() {
            try {
                Channel channelInst = connFactory.createConnection().createChannel(false);
                channelInst.queueDeclare(MQConstant.QUEUE_NAME_SECKILL, true, false, false, null);
                return channelInst;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

    };
    private ThreadLocal<Channel> localReceiveChannel = new ThreadLocal<Channel>() {
        public Channel initialValue() {
            try {
                Channel channelInst = connFactory.createConnection().createChannel(false);
                channelInst.queueDeclare(MQConstant.QUEUE_NAME_SECKILL, true, false, false, null);
                channelInst.basicQos(0, 1, false);
                return channelInst;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

    };

    /**
     * 获取当前线程使用的Rabbitmq通道
     * @return
     */
    public Channel getSendChannel() {
        logger.info("Send_CurThread.id={}--->", Thread.currentThread().getId());
        Channel channel = localSendChannel.get();
        if (channel==null){
            //申明队列
            try {
                channel = connFactory.createConnection().createChannel(false);
                channel.queueDeclare(MQConstant.QUEUE_NAME_SECKILL, true, false, false, null);
                localSendChannel.set(channel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return channel;
    }

    public Channel getReceiveChannel() {
        logger.info("Receive_CurThread.id={}--->", Thread.currentThread().getId());
        Channel channel = localReceiveChannel.get();
        if (channel==null){
            //申明队列
            try {
                channel = connFactory.createConnection().createChannel(false);
                channel.queueDeclare(MQConstant.QUEUE_NAME_SECKILL, true, false, false, null);
                localReceiveChannel.set(channel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return channel;
    }
}
