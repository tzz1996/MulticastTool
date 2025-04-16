package org.example.udpMessageHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.example.nettyUdp.MulticastConfig;

import java.util.List;

public class TestHandler implements MulticastMessageHandler {
    @Override
    public void handleMessage(MulticastConfig.MulticastGroup group, List<String> classNames, DatagramPacket packet) {
        ByteBuf content = packet.content();
        String message = content.toString(CharsetUtil.UTF_8);
        System.out.println("Custom handler for " + group + " received: " + message);
    }
}
