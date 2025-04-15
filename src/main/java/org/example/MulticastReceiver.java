package org.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MulticastReceiver {
    private static final String MULTICAST_GROUP = "239.255.27.1";
    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channelFactory(() -> new NioDatagramChannel(InternetProtocolFamily.IPv4))
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
                                    ByteBuf content = msg.content();
                                    String received = content.toString(CharsetUtil.UTF_8);
                                    System.out.println("Received: " + received);
                                }
                            });
                        }
                    });

            Channel channel = bootstrap.bind(PORT).sync().channel();
            
            // 获取所有网络接口并加入组播组
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                try {
                    if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                        InetSocketAddress groupAddress = new InetSocketAddress(MULTICAST_GROUP, PORT);
                        ((NioDatagramChannel) channel).joinGroup(groupAddress, networkInterface).sync();
                        System.out.println("Joined multicast group on interface: " + networkInterface.getDisplayName());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to join multicast group on interface " + 
                            networkInterface.getDisplayName() + ": " + e.getMessage());
                }
            }
            
            System.out.println("Multicast receiver started. Waiting for messages...");
            
            channel.closeFuture().await();
        } finally {
            group.shutdownGracefully();
        }
    }
} 