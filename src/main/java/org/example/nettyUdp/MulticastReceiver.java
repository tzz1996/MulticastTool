package org.example.nettyUdp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import org.example.udpMessageHandler.MulticastMessageHandler;
import org.example.udpMessageHandler.TestHandler;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastReceiver {
    // 存储组播组和对应的消息处理器
    private static final Map<MulticastConfig.MulticastGroup, MulticastMessageHandler> handlers = new ConcurrentHashMap<>();
    // 存储组播组和对应的线程池
    private static final Map<MulticastConfig.MulticastGroup, ExecutorService> groupThreadPools = new ConcurrentHashMap<>();
    // 一个组播端口对应一个channel通信
    private static final List<Channel> channels = new ArrayList<>();
    
    // 添加管理 Channel 的方法
    private static void addChannel(Channel channel) {
        channels.add(channel);
    }
    
    private static void removeChannel(Channel channel) {
        channels.remove(channel);
        channel.close();
    }
    
    /**
     * 注册组播消息处理器
     * @param group 组播组
     * @param handler 消息处理器
     */
    public static void registerHandler(MulticastConfig.MulticastGroup group, MulticastMessageHandler handler) {
        handlers.put(group, handler);
        // 为每个组播组创建一个独立的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(5); // 可以根据需要调整线程池大小
        groupThreadPools.put(group, executorService);
    }
    
    /**
     * 移除组播消息处理器
     * @param group 组播组
     */
    public static void removeHandler(MulticastConfig.MulticastGroup group) {
        handlers.remove(group);
        // 关闭并移除对应的线程池
        ExecutorService executorService = groupThreadPools.remove(group);
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * 添加组播监听
     * @param groupToClassNames 所有组播端口-实体名称列表信息
     */
    private static void joinMulticastGroups(Map<MulticastConfig.MulticastGroup, List<String>> groupToClassNames) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 为每个组播组创建单独的channel
            for (MulticastConfig.MulticastGroup multicastGroup : groupToClassNames.keySet()) {
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
                                        msg.retain();
                                        // 获取对应的消息处理器和线程池
                                        MulticastMessageHandler handler = handlers.get(multicastGroup);
                                        ExecutorService executorService = groupThreadPools.get(multicastGroup);
                                        
                                        if (handler != null && executorService != null) {
                                            // 在消息被释放前复制内容
                                            ByteBuf content = msg.content();
                                            
                                            // 使用对应的线程池处理消息
                                            executorService.submit(() -> {
                                                try {
                                                    // 创建新的 DatagramPacket 副本
//                                                    ByteBuf newContent = ctx.alloc().buffer();
//                                                    newContent.writeBytes(content);
                                                    
                                                    handler.handleMessage(multicastGroup, groupToClassNames.get(multicastGroup), content);
                                                } catch (Exception e) {
                                                    MulticastMessageQueue.offerMessageQueue("Error in handler: " + e.getMessage());
                                                }
                                            });
                                        } else {
                                            // 如果没有注册处理器，使用默认处理方式
                                            ByteBuf content = msg.content();
                                            String received = content.toString(CharsetUtil.UTF_8);
                                            MulticastMessageQueue.offerMessageQueue("Received from " + multicastGroup + ": " + received);
                                            msg.release();
                                        }
                                    }
                                });
                            }
                        });

                Channel channel = bootstrap.bind(multicastGroup.getPort()).sync().channel();
                addChannel(channel);
                
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
            // 关闭所有线程池
            for (ExecutorService executorService : groupThreadPools.values()) {
                executorService.shutdown();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // 解析XML配置文件
        String configPath = new File("multicast-config.xml").getAbsolutePath();
        Map<MulticastConfig.MulticastGroup, List<String>> groupToClassNames = MulticastConfig.parseXmlConfig(configPath);
        
        // 打印配置信息
        System.out.println("Loaded multicast configuration:");
        for (Map.Entry<MulticastConfig.MulticastGroup, List<String>> entry : groupToClassNames.entrySet()) {
            System.out.println("Group: " + entry.getKey());
            System.out.println("ClassNames: " + entry.getValue());
        }

        // 示例：注册自定义消息处理器
        registerHandler(new MulticastConfig.MulticastGroup("239.255.27.1", 8888), new TestHandler());

        // 启动消息处理
        MulticastMessageQueue.startMessagePrinter();

        joinMulticastGroups(groupToClassNames);
    }
} 