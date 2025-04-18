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
            MulticastMessageQueue.offerMessageQueue("Custom handler for " + group + " received: " + message);
        } catch (Exception e) {
            MulticastMessageQueue.offerMessageQueue("Error processing message: " + e.getMessage());
        }
    }
}
