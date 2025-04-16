package org.example.nettyUdp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MulticastSender {
    private static final EventLoopGroup group = new NioEventLoopGroup();
    private static Channel channel;
    private static final Map<MulticastConfig.MulticastGroup, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    static {
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

            channel = bootstrap.bind(0).sync().channel();
        } catch (Exception e) {
            e.printStackTrace();
            group.shutdownGracefully();
        }
    }

    /**
     * 发送单条UDP组播消息
     * @param multicastGroup 组播组信息（包含地址和端口）
     * @param message 要发送的消息内容
     * @throws Exception 发送失败时抛出异常
     */
    private static void sendMessage(MulticastConfig.MulticastGroup multicastGroup, String message) throws Exception {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Channel is not initialized or not active");
        }

        // 创建消息缓冲区
        ByteBuf buf = channel.alloc().buffer();
        buf.writeBytes(message.getBytes(CharsetUtil.UTF_8));
        
        // 创建目标地址
        InetSocketAddress address = new InetSocketAddress(
            multicastGroup.getGroup(), 
            multicastGroup.getPort()
        );
        
        // 创建数据包并发送
        DatagramPacket packet = new DatagramPacket(buf, address);
        channel.writeAndFlush(packet).sync();
        
        System.out.println("Sent to " + multicastGroup + ": " + message);
    }

    /**
     * 开始定时发送消息
     * @param multicastGroup 组播组信息
     * @param message 要发送的消息
     * @param initialDelay 首次发送延迟时间（毫秒）
     * @param period 发送间隔时间（毫秒）
     * @throws Exception 如果发送失败
     */
    public static void startScheduledSend(MulticastConfig.MulticastGroup multicastGroup, String message, 
            long initialDelay, long period) throws Exception {
        // 如果已经存在该组播组的定时任务，先停止它
        stopScheduledSend(multicastGroup);

        ScheduledFuture<?> future = group.scheduleAtFixedRate(() -> {
            try {
                sendMessage(multicastGroup, message);
            } catch (Exception e) {
                e.printStackTrace();
                stopScheduledSend(multicastGroup); // 发生错误时停止定时发送
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);

        scheduledTasks.put(multicastGroup, future);
    }

    /**
     * 停止指定组播组的定时发送
     * @param multicastGroup 要停止的组播组
     */
    public static void stopScheduledSend(MulticastConfig.MulticastGroup multicastGroup) {
        ScheduledFuture<?> future = scheduledTasks.remove(multicastGroup);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 停止所有组播组的定时发送
     */
    public static void stopAllScheduledSend() {
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(false);
        }
        scheduledTasks.clear();
    }

    /**
     * 关闭发送器，释放资源
     */
    public static void shutdown() {
        stopAllScheduledSend(); // 确保停止所有定时发送
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }

    /**
     * 测试方法：同时向多个组播组发送消息
     * 与MulticastReceiver的main方法配合测试
     */
    public static void testMultipleScheduledSend() throws Exception {
        try {
            // 创建多个组播组，与MulticastReceiver中的配置对应
            MulticastConfig.MulticastGroup group1 = new MulticastConfig.MulticastGroup("239.255.27.1", 8888);
            MulticastConfig.MulticastGroup group2 = new MulticastConfig.MulticastGroup("239.255.27.2", 8889);
            MulticastConfig.MulticastGroup group3 = new MulticastConfig.MulticastGroup("239.255.27.3", 8890);

            // 为每个组播组启动定时发送，使用不同的发送间隔
            MulticastSender.startScheduledSend(group1, "Message to group 1", 0, 1000); // 每秒发送一次
            MulticastSender.startScheduledSend(group2, "Message to group 2", 0, 2000); // 每2秒发送一次
            MulticastSender.startScheduledSend(group3, "Message to group 3", 0, 3000); // 每3秒发送一次

            // 让定时发送运行一段时间
            Thread.sleep(30000); // 运行30秒

            // 停止所有组播组的发送
            MulticastSender.stopAllScheduledSend();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 确保关闭资源
            MulticastSender.shutdown();
        }
    }

    /**
     * 简单的循环测试方法，用于基本功能测试
     * 向每个组播组发送固定数量的消息
     */
    public static void testSimpleLoopSend() throws Exception {
        try {
            // 向每个组播组发送消息
            for (MulticastConfig.MulticastGroup multicastGroup : MulticastConfig.MULTICAST_GROUPS) {
                // 发送10条消息到每个组播组
                for (int i = 0; i < 10; i++) {
                    String message = "Hello Multicast " + multicastGroup + "! Message " + i;
                    sendMessage(multicastGroup, message);
                    Thread.sleep(1000); // 每秒发送一条消息
                }
            }
        } finally {
            shutdown();
        }
    }

    /**
     * 测试方法，用于与MulticastReceiver配合测试
     */
    public static void main(String[] args) throws Exception {
       testMultipleScheduledSend();
        // testSimpleLoopSend();
    }
} 