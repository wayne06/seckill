package com.zs.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.zs.seckill.dao.ItemStockDOMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class MqConsumer {

    private DefaultMQPushConsumer consumer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer("stock_consumer_group");
        consumer.setNamesrvAddr(nameAddr);
        consumer.subscribe(topicName, "*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messageExts,
                                                            ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                // 在数据库中扣减库存的逻辑
                Message message = messageExts.get(0);
                String msgJson = new String(message.getBody());
                Map<String, Object> map = JSON.parseObject(msgJson, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                itemStockDOMapper.decreaseStock(itemId, amount);

                // 返回CONSUME_SUCCESS说明这条消息已经被消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }

}
