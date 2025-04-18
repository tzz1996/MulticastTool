package org.example.nettyUdp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MulticastMessageQueue {
    // 消息队列，用于在主线程中打印消息
    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>(1000); // 设置队列容量
    // 控制消息打印线程的标志
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Object lock = new Object();
    private static Consumer<String> messageConsumer;
    private static Thread printerThread;

    /**
     * 设置消息消费者，用于在主线程中处理消息
     * @param consumer 消息消费者
     */
    public static void setMessageConsumer(Consumer<String> consumer) {
        messageConsumer = consumer;
    }

    /**
     * 将消息添加到队列中
     * @param message 要添加的消息
     */
    public static void offerMessageQueue(String message) {
        try {
            synchronized (lock) {
                // 使用 put 而不是 offer，确保消息不会丢失
                messageQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 启动消息处理
     */
    public static void startMessagePrinter() {
        // 设置消息消费者，确保在主线程中处理消息
        setMessageConsumer(message -> {
            System.out.println(message);
        });

        // 启动消息处理线程
        printerThread = new Thread(() -> {
            while (running.get()) {
                try {
                    String message = messageQueue.take();
                    if (messageConsumer != null) {
                        messageConsumer.accept(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        printerThread.setDaemon(true);
        printerThread.start();
    }

    /**
     * 停止消息处理
     */
    public static void stopMessagePrinter() {
        running.set(false);
        if (printerThread != null) {
            printerThread.interrupt();
        }
    }

    /**
     * 获取队列中的消息数量
     * @return 队列中的消息数量
     */
    public static int getQueueSize() {
        synchronized (lock) {
            return messageQueue.size();
        }
    }
}
