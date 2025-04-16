package org.example;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MulticastGroup that = (MulticastGroup) o;
            return port == that.port && group.equals(that.group);
        }

        @Override
        public int hashCode() {
            return 31 * group.hashCode() + port;
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

    // XML配置类
    @JacksonXmlRootElement(localName = "multicastConfig")
    public static class MulticastXmlConfig {
        @JacksonXmlProperty(localName = "group")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<GroupConfig> groups;

        public List<GroupConfig> getGroups() {
            return groups;
        }

        public void setGroups(List<GroupConfig> groups) {
            this.groups = groups;
        }
    }

    public static class GroupConfig {
        @JacksonXmlProperty(isAttribute = true)
        private String address;

        @JacksonXmlProperty(isAttribute = true)
        private int port;

        @JacksonXmlProperty(localName = "message")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<String> messages;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }
    }

    /**
     * 解析XML配置文件，返回组播组和消息的映射
     * @param xmlFilePath XML文件路径
     * @return 组播组和消息列表的映射
     * @throws IOException 如果解析失败
     */
    public static Map<MulticastGroup, List<String>> parseXmlConfig(String xmlFilePath) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        MulticastXmlConfig config = xmlMapper.readValue(new File(xmlFilePath), MulticastXmlConfig.class);
        
        Map<MulticastGroup, List<String>> result = new HashMap<>();
        for (GroupConfig groupConfig : config.getGroups()) {
            MulticastGroup group = new MulticastGroup(groupConfig.getAddress(), groupConfig.getPort());
            result.put(group, groupConfig.getMessages());
        }
        return result;
    }
} 