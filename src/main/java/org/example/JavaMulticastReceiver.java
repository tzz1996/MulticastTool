package org.example;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class JavaMulticastReceiver {
    private static final String MULTICAST_GROUP = "239.255.27.1";
    private static final int PORT = 8888;

    public static void main(String[] args) {
        try {
            // 创建组播套接字并绑定到指定端口
            MulticastSocket socket = new MulticastSocket(PORT);
            
            // 获取组播地址
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            
            // 获取所有网络接口并加入组播组
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                try {
                    if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                        socket.joinGroup(new java.net.InetSocketAddress(group, PORT), networkInterface);
                        System.out.println("Joined multicast group on interface: " + networkInterface.getDisplayName());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to join multicast group on interface " + 
                            networkInterface.getDisplayName() + ": " + e.getMessage());
                }
            }
            
            System.out.println("Multicast receiver started. Waiting for messages...");
            
            // 创建接收缓冲区
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            // 持续接收消息
            while (true) {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("Received: " + message);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 