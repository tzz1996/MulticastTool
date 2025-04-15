package org.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

public class MulticastSender {
    private static final String MULTICAST_GROUP = "239.255.27.1";
    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) {
                            // 不需要添加任何处理器，因为我们只是发送消息
                        }
                    });

            Channel channel = bootstrap.bind(0).sync().channel();
            
            // 发送10条组播消息
            for (int i = 0; i < 10; i++) {
                String message = "Hello Multicast! Message " + i;
                ByteBuf buf = channel.alloc().buffer();
                buf.writeBytes(message.getBytes(CharsetUtil.UTF_8));
                
                InetSocketAddress address = new InetSocketAddress(MULTICAST_GROUP, PORT);
                channel.writeAndFlush(new DatagramPacket(buf, address));
                System.out.println("Sent: " + message);
                
                Thread.sleep(1000); // 每秒发送一条消息
            }
            
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
} 