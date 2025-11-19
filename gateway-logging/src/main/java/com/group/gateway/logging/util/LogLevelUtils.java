package com.group.gateway.logging.util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 日志级别工具类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Component
public class LogLevelUtils {
    
    // 标准日志级别（从低到高）
    public static final String[] STANDARD_LEVELS = {
        "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"
    };
    
    // 日志级别优先级映射
    private static final Map<String, Integer> LEVEL_PRIORITY = new HashMap<>();
    
    static {
        LEVEL_PRIORITY.put("TRACE", 1);
        LEVEL_PRIORITY.put("DEBUG", 2);
        LEVEL_PRIORITY.put("INFO", 3);
        LEVEL_PRIORITY.put("WARN", 4);
        LEVEL_PRIORITY.put("WARNING", 4); // WARN的别名
        LEVEL_PRIORITY.put("ERROR", 5);
        LEVEL_PRIORITY.put("FATAL", 6);
    }
    
    // 常用日志级别集合
    private static final Set<String> VALID_LEVELS = LEVEL_PRIORITY.keySet();
    
    /**
     * 验证日志级别是否有效
     */
    public boolean isValidLevel(String level) {
        return level != null && VALID_LEVELS.contains(level.toUpperCase());
    }
    
    /**
     * 标准化日志级别
     * 将输入的级别转换为标准格式
     */
    public String normalizeLevel(String level) {
        if (level == null) {
            return "INFO"; // 默认级别
        }
        
        String upperLevel = level.toUpperCase().trim();
        
        // 处理别名
        switch (upperLevel) {
            case "WARNING":
                return "WARN";
            default:
                return upperLevel;
        }
    }
    
    /**
     * 获取日志级别优先级
     */
    public int getLevelPriority(String level) {
        String normalizedLevel = normalizeLevel(level);
        return LEVEL_PRIORITY.getOrDefault(normalizedLevel, 3); // 默认为INFO级别
    }
    
    /**
     * 比较两个日志级别的优先级
     * 
     * @param level1 级别1
     * @param level2 级别2
     * @return 
     *   -1 如果 level1 < level2
     *    0 如果 level1 == level2
     *    1 如果 level1 > level2
     */
    public int compareLevels(String level1, String level2) {
        int priority1 = getLevelPriority(level1);
        int priority2 = getLevelPriority(level2);
        return Integer.compare(priority1, priority2);
    }
    
    /**
     * 检查第一个级别是否包含第二个级别
     * 
     * @param currentLevel 当前级别
     * @param checkLevel 要检查的级别
     * @return true 如果当前级别包含要检查的级别
     */
    public boolean shouldLog(String currentLevel, String checkLevel) {
        int currentPriority = getLevelPriority(currentLevel);
        int checkPriority = getLevelPriority(checkLevel);
        return currentPriority >= checkPriority;
    }
    
    /**
     * 从配置字符串解析日志级别
     * 
     * @param levelConfig 配置字符串，支持逗号分隔的多个级别
     * @return 级别集合
     */
    public Set<String> parseLevels(String levelConfig) {
        if (levelConfig == null || levelConfig.trim().isEmpty()) {
            return new HashSet<>(Arrays.asList("INFO"));
        }
        
        return Arrays.stream(levelConfig.split(","))
                .map(String::trim)
                .filter(this::isValidLevel)
                .map(this::normalizeLevel)
                .collect(Collectors.toSet());
    }
    
    /**
     * 将级别集合转换为配置字符串
     */
    public String toConfigString(Set<String> levels) {
        if (levels == null || levels.isEmpty()) {
            return "INFO";
        }
        
        return levels.stream()
                .sorted(this::compareLevels)
                .collect(Collectors.joining(","));
    }
    
    /**
     * 获取所有有效的日志级别
     */
    public List<String> getAllValidLevels() {
        return new ArrayList<>(VALID_LEVELS);
    }
    
    /**
     * 获取所有标准日志级别
     */
    public List<String> getStandardLevels() {
        return Arrays.asList(STANDARD_LEVELS);
    }
    
    /**
     * 根据优先级排序级别列表
     */
    public List<String> sortLevelsByPriority(List<String> levels) {
        return levels.stream()
                .sorted(this::compareLevels)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取低于指定级别的所有级别
     */
    public List<String> getLevelsBelow(String level) {
        int targetPriority = getLevelPriority(level);
        
        return STANDARD_LEVELS.stream()
                .filter(l -> getLevelPriority(l) < targetPriority)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取高于指定级别的所有级别
     */
    public List<String> getLevelsAbove(String level) {
        int targetPriority = getLevelPriority(level);
        
        return STANDARD_LEVELS.stream()
                .filter(l -> getLevelPriority(l) > targetPriority)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取包含指定级别的级别范围
     */
    public List<String> getLevelsFrom(String fromLevel) {
        int fromPriority = getLevelPriority(fromLevel);
        
        return STANDARD_LEVELS.stream()
                .filter(l -> getLevelPriority(l) >= fromPriority)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取两个级别之间的所有级别
     */
    public List<String> getLevelsBetween(String fromLevel, String toLevel) {
        int fromPriority = getLevelPriority(fromLevel);
        int toPriority = getLevelPriority(toLevel);
        
        // 确保from优先级 <= to优先级
        if (fromPriority > toPriority) {
            return new ArrayList<>();
        }
        
        return STANDARD_LEVELS.stream()
                .filter(l -> {
                    int levelPriority = getLevelPriority(l);
                    return levelPriority >= fromPriority && levelPriority <= toPriority;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 转换日志级别为颜色代码
     */
    public String getLevelColor(String level) {
        switch (normalizeLevel(level)) {
            case "ERROR":
            case "FATAL":
                return "\u001B[31m"; // 红色
            case "WARN":
                return "\u001B[33m"; // 黄色
            case "INFO":
                return "\u001B[32m"; // 绿色
            case "DEBUG":
                return "\u001B[34m"; // 蓝色
            case "TRACE":
                return "\u001B[36m"; // 青色
            default:
                return "\u001B[37m"; // 白色
        }
    }
    
    /**
     * 重置颜色
     */
    public static String getResetColor() {
        return "\u001B[0m";
    }
    
    /**
     * 格式化带颜色的级别字符串
     */
    public String formatLevelWithColor(String level) {
        return getLevelColor(level) + level + getResetColor();
    }
    
    /**
     * 验证级别配置字符串
     */
    public boolean isValidLevelConfig(String config) {
        if (config == null || config.trim().isEmpty()) {
            return true; // 空配置表示使用默认级别
        }
        
        try {
            Set<String> levels = parseLevels(config);
            return !levels.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取级别统计信息
     */
    public Map<String, Object> getLevelStatistics(List<String> levels) {
        Map<String, Object> stats = new HashMap<>();
        
        if (levels == null || levels.isEmpty()) {
            stats.put("total", 0);
            stats.put("distribution", new HashMap<>());
            return stats;
        }
        
        // 统计分布
        Map<String, Long> distribution = levels.stream()
                .map(this::normalizeLevel)
                .collect(Collectors.groupingBy(level -> level, Collectors.counting()));
        
        stats.put("total", (long) levels.size());
        stats.put("distribution", distribution);
        
        // 计算百分比
        Map<String, Double> percentages = distribution.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / levels.size() * 100
                ));
        stats.put("percentages", percentages);
        
        // 最常用级别
        String mostCommon = distribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("INFO");
        stats.put("mostCommon", mostCommon);
        
        return stats;
    }
}