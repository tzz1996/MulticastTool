package org.example.udpMessageHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.example.nettyUdp.MulticastConfig;
import org.example.nettyUdp.MulticastMessageQueue;

import java.util.List;

public class TestHandler implements MulticastMessageHandler {
    @Override
    public void handleMessage(MulticastConfig.MulticastGroup group, List<String> classNames, ByteBuf content) {
        try {
            String message = content.toString(CharsetUtil.UTF_8);
            // 确保消息完整
            String fullMessage = String.format("Custom handler for %s received: %s", group, message);
            MulticastMessageQueue.offerMessageQueue(fullMessage);
        } catch (Exception e) {
            MulticastMessageQueue.offerMessageQueue("Error processing message: " + e.getMessage());
        } finally {
            // 确保释放引用
            content.release();
        }
    }
}
