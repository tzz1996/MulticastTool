package org.example.simpleUdp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class JavaMulticastSender {
    private static final String MULTICAST_GROUP = "239.255.27.1";
    private static final int PORT = 8888;

    public static void main(String[] args) {
        try {
            // 创建组播套接字
            MulticastSocket socket = new MulticastSocket();
            
            // 设置TTL（Time To Live）
            socket.setTimeToLive(1); // 限制在本地网络
            
            // 获取组播地址
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            
            // 发送10条消息
            for (int i = 0; i < 10; i++) {
                String message = "Java Native Multicast! Message " + i;
                byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
                
                // 创建数据包
                DatagramPacket packet = new DatagramPacket(
                    buffer, 
                    buffer.length, 
                    group, 
                    PORT
                );
                
                // 发送数据包
                socket.send(packet);
                System.out.println("Sent: " + message);
                
                // 等待1秒
                Thread.sleep(1000);
            }
            
            // 关闭套接字
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 