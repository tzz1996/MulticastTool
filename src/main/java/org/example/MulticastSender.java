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

public class MulticastSender {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channelFactory(() -> new NioDatagramChannel(InternetProtocolFamily.IPv4))
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) {
                            // 不需要添加任何处理器，因为我们只是发送消息
                        }
                    });

            Channel channel = bootstrap.bind(0).sync().channel();
            
            // 向每个组播组发送消息
            for (MulticastConfig.MulticastGroup multicastGroup : MulticastConfig.MULTICAST_GROUPS) {
                // 发送10条消息到每个组播组
                for (int i = 0; i < 10; i++) {
                    String message = "Hello Multicast " + multicastGroup + "! Message " + i;
                    ByteBuf buf = channel.alloc().buffer();
                    buf.writeBytes(message.getBytes(CharsetUtil.UTF_8));
                    
                    InetSocketAddress address = new InetSocketAddress(
                        multicastGroup.getGroup(), 
                        multicastGroup.getPort()
                    );
                    channel.writeAndFlush(new DatagramPacket(buf, address));
                    System.out.println("Sent to " + multicastGroup + ": " + message);
                    
                    Thread.sleep(1000); // 每秒发送一条消息
                }
            }
            
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
} 