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
import java.util.List;
import java.util.ArrayList;

public class MulticastReceiver {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 为每个组播组创建单独的channel
            List<Channel> channels = new ArrayList<>();
            
            // 遍历所有组播组配置
            for (MulticastConfig.MulticastGroup multicastGroup : MulticastConfig.MULTICAST_GROUPS) {
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
                                        System.out.println("Received from " + multicastGroup + ": " + received);
                                    }
                                });
                            }
                        });

                Channel channel = bootstrap.bind(multicastGroup.getPort()).sync().channel();
                channels.add(channel);
                
                // 获取所有网络接口并加入组播组
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    try {
                        if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                            InetSocketAddress groupAddress = new InetSocketAddress(
                                multicastGroup.getGroup(), 
                                multicastGroup.getPort()
                            );
                            ((NioDatagramChannel) channel).joinGroup(groupAddress, networkInterface).sync();
                            System.out.println("Joined multicast group " + multicastGroup + 
                                    " on interface: " + networkInterface.getDisplayName());
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to join multicast group " + multicastGroup + 
                                " on interface " + networkInterface.getDisplayName() + 
                                ": " + e.getMessage());
                    }
                }
            }
            
            System.out.println("Multicast receiver started. Waiting for messages...");
            
            // 等待所有channel关闭
            for (Channel channel : channels) {
                channel.closeFuture().await();
            }
        } finally {
            group.shutdownGracefully();
        }
    }
} 