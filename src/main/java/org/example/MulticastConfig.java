package org.example;

import java.util.ArrayList;
import java.util.List;

public class MulticastConfig {
    public static class MulticastGroup {
        private final String group;
        private final int port;

        public MulticastGroup(String group, int port) {
            this.group = group;
            this.port = port;
        }

        public String getGroup() {
            return group;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return group + ":" + port;
        }
    }

    // 定义要监听的组播组列表
    public static final List<MulticastGroup> MULTICAST_GROUPS = new ArrayList<>();
    
    static {
        // 添加要监听的组播组和端口
        MULTICAST_GROUPS.add(new MulticastGroup("239.255.27.1", 8888));
        MULTICAST_GROUPS.add(new MulticastGroup("239.255.27.2", 8889));
        MULTICAST_GROUPS.add(new MulticastGroup("239.255.27.3", 8890));
        // 可以继续添加更多组播组
    }
} 