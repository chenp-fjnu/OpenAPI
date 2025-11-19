package com.group.gateway.logging.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志索引实体类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "indexName")
public class LogIndex {
    
    /**
     * 索引名称
     */
    private String indexName;
    
    /**
     * 索引描述
     */
    private String description;
    
    /**
     * 索引状态
     */
    private IndexStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
    
    /**
     * 文档总数
     */
    private long documentCount;
    
    /**
     * 索引大小（字节）
     */
    private long sizeInBytes;
    
    /**
     * 字段映射
     */
    private Map<String, FieldMapping> fieldMappings;
    
    /**
     * 索引设置
     */
    private IndexSettings settings;
    
    /**
     * 分片信息
     */
    private ShardInfo shards;
    
    /**
     * 索引状态枚举
     */
    public enum IndexStatus {
        CREATING, ACTIVE, CLOSED, ERROR
    }
    
    /**
     * 字段映射
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldMapping {
        
        /**
         * 字段名称
         */
        private String fieldName;
        
        /**
         * 字段类型
         */
        private FieldType type;
        
        /**
         * 是否索引
         */
        private boolean indexed;
        
        /**
         * 是否存储
         */
        private boolean stored;
        
        /**
         * 是否分析
         */
        private boolean analyzed;
        
        /**
         * 分词器
         */
        private String analyzer;
        
        /**
         * 搜索分词器
         */
        private String searchAnalyzer;
        
        /**
         * 字段格式
         */
        private String format;
        
        /**
         * 字段属性
         */
        private Map<String, Object> properties;
    }
    
    /**
     * 字段类型枚举
     */
    public enum FieldType {
        TEXT, KEYWORD, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN, DATE, 
        NESTED, OBJECT, BINARY, IP, GEO_POINT, GEO_SHAPE
    }
    
    /**
     * 索引设置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexSettings {
        
        /**
         * 分片数量
         */
        private int numberOfShards = 1;
        
        /**
         * 副本数量
         */
        private int numberOfReplicas = 1;
        
        /**
         * 刷新间隔
         */
        private String refreshInterval = "1s";
        
        /**
         * 最大结果窗口
         */
        private int maxResultWindow = 10000;
        
        /**
         * 分析器
         */
        private String analyzer;
        
        /**
         * 索引生命周期管理
         */
        private LifecyclePolicy lifecyclePolicy;
        
        /**
         * 索引生命周期管理策略
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LifecyclePolicy {
            
            /**
             * 策略名称
             */
            private String policyName;
            
            /**
             * 保留期限
             */
            private String retentionPeriod;
            
            /**
             * 压缩设置
             */
            private CompressionSettings compression;
            
            /**
             * 压缩设置
             */
            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class CompressionSettings {
                
                /**
                 * 是否启用压缩
                 */
                private boolean enabled = true;
                
                /**
                 * 压缩级别
                 */
                private int level = 3;
            }
        }
    }
    
    /**
     * 分片信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShardInfo {
        
        /**
         * 主分片
         */
        private int primary;
        
        /**
         * 副本分片
         */
        private int replica;
        
        /**
         * 活跃分片
         */
        private int active;
        
        /**
         * 初始化分片
         */
        private int initializing;
        
        /**
         * 未分配分片
         */
        private int unassigned;
    }
    
    /**
     * 静态工厂方法
     */
    public static LogIndex createDailyIndex(String prefix) {
        String indexName = prefix + "-" + LocalDateTime.now().toLocalDate().toString().replace("-", ".");
        return builder()
                .indexName(indexName)
                .description("Daily log index for " + LocalDateTime.now().toLocalDate().toString())
                .status(IndexStatus.CREATING)
                .createTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .documentCount(0)
                .sizeInBytes(0)
                .build();
    }
    
    public static LogIndex createIndex(String indexName, String description) {
        return builder()
                .indexName(indexName)
                .description(description)
                .status(IndexStatus.CREATING)
                .createTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .documentCount(0)
                .sizeInBytes(0)
                .build();
    }
}