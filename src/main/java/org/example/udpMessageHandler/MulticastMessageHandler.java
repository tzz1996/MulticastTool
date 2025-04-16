package org.example.udpMessageHandler;

import io.netty.channel.socket.DatagramPacket;
import org.example.nettyUdp.MulticastConfig;

import java.util.List;

/**
 * 组播消息处理器接口
 */
public interface MulticastMessageHandler {
    /**
     * 处理接收到的组播消息
     * @param group       组播组信息
     * @param classNames  组播中对应的结构体名列表
     * @param packet      接收到的数据包
     */
    void handleMessage(MulticastConfig.MulticastGroup group, List<String> classNames, DatagramPacket packet);
} 