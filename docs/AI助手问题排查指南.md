# AI助手问题排查指南

## 问题描述

在使用AI助手功能时遇到以下错误：

```
com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot construct instance of `org.springframework.ai.chat.messages.UserMessage` (although at least one Creator exists): cannot deserialize from Object value (no delegate- or property-based Creator)
```

## 问题原因

这个错误是由于Spring AI的消息对象（如`UserMessage`、`AssistantMessage`）在Redis中进行序列化和反序列化时出现的问题。Spring AI的消息类没有默认的无参构造函数，导致Jackson无法正确反序列化这些对象。

## 解决方案

### 1. 修改Redis聊天内存仓库

将原来直接存储Spring AI消息对象的方式改为存储简化的消息对象，然后在读取时重新构造Spring AI消息对象。

**主要修改点：**

1. **使用字符串模板存储**：将`RedisTemplate<String, Object>`改为`RedisTemplate<String, String>`
2. **创建简化消息类**：定义`SimpleMessage`类用于序列化
3. **添加转换方法**：在存储和读取时进行消息对象转换
4. **异常处理**：在反序列化失败时返回空列表而不是抛出异常
5. **解决Bean冲突**：使用`@Qualifier`注解避免与Spring Boot自动配置的bean冲突

### 2. 修改AI配置类

更新Redis配置以支持新的存储方式：

1. **更新RedisTemplate配置**：使用字符串序列化器
2. **更新依赖注入**：传入ObjectMapper用于JSON序列化
3. **避免Bean名称冲突**：使用自定义的bean名称

## 修改后的代码

### RedisChatMemoryRepository.java

```java
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisChatMemoryRepository(@Qualifier("aiStringRedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    // 使用简化的消息对象进行存储
    public static class SimpleMessage {
        private String content;
        private String messageType;
        // getter和setter方法
    }
    
    // 转换方法
    private SimpleMessage convertToSimpleMessage(Message message) {
        SimpleMessage simple = new SimpleMessage();
        simple.setContent(message.getText()); // 注意：使用getText()而不是getContent()
        simple.setMessageType(message.getMessageType().name());
        return simple;
    }
    
    private Message convertToMessage(SimpleMessage simple) {
        return switch (simple.getMessageType()) {
            case "USER" -> new UserMessage(simple.getContent());
            case "ASSISTANT" -> new AssistantMessage(simple.getContent());
            case "SYSTEM" -> new SystemMessage(simple.getContent());
            default -> new UserMessage(simple.getContent());
        };
    }
}
```

### AiConfiguration.java

```java
@Configuration
public class AiConfiguration {
    
    @Bean
    public RedisTemplate<String, String> aiStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用字符串序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public ChatMemoryRepository chatMemoryRepository(
            @Qualifier("aiStringRedisTemplate") RedisTemplate<String, String> redisTemplate, 
            ObjectMapper objectMapper) {
        return new RedisChatMemoryRepository(redisTemplate, objectMapper);
    }
}
```

## 关键解决步骤

### 步骤1：Bean名称冲突解决
**问题**：Spring Boot自动配置已经提供了`stringRedisTemplate` bean
**解决**：使用自定义名称`aiStringRedisTemplate`

### 步骤2：依赖注入歧义解决
**问题**：存在多个`RedisTemplate<String, String>`类型的bean
**解决**：在构造函数和配置方法中使用`@Qualifier("aiStringRedisTemplate")`

### 步骤3：消息对象API修正
**问题**：`Message`接口没有`getContent()`方法
**解决**：使用`getText()`方法获取消息内容

## 技术细节

### 序列化策略

1. **原问题**：Spring AI的消息类设计为不可变对象，没有默认构造函数
2. **解决方案**：使用简化的POJO类进行序列化，在运行时重新构造Spring AI对象
3. **优势**：避免了复杂的Jackson配置，提高了序列化的可靠性

### 错误处理

```java
@Override
public List<Message> findByConversationId(String conversationId) {
    try {
        // 反序列化逻辑
    } catch (Exception e) {
        // 如果反序列化失败，返回空列表而不是抛出异常
        return List.of();
    }
}
```

这样可以确保即使Redis中有损坏的数据，系统也能正常运行。

## 验证方法

1. **启动应用**：确保没有序列化相关的错误
   ```
   2025-12-22T21:01:57.802+08:00  INFO 16936 --- [           main] c.e.springboot.SpringbootApplication     : Started SpringbootApplication in 3.262 seconds
   ```

2. **创建会话**：测试新会话的创建
3. **发送消息**：验证消息的存储和检索
4. **重启应用**：确保聊天历史能够正确恢复

## 预防措施

1. **定期清理Redis**：设置合理的过期时间（当前为24小时）
2. **监控日志**：关注序列化相关的警告和错误
3. **版本兼容性**：升级Spring AI版本时注意消息类的变化
4. **Bean命名规范**：避免与Spring Boot自动配置的bean名称冲突

## 相关文件

- `springboot/src/main/java/com/example/springboot/config/RedisChatMemoryRepository.java`
- `springboot/src/main/java/com/example/springboot/config/AiConfiguration.java`

## 其他可能的解决方案

### 方案1：自定义Jackson配置
为Spring AI消息类配置自定义的序列化器和反序列化器。

### 方案2：使用内存存储
如果不需要持久化聊天历史，可以使用Spring AI提供的内存存储。

### 方案3：数据库存储
将聊天历史存储在数据库中，避免序列化问题。

## 总结

通过以下关键步骤解决了序列化问题：

1. **避免Bean冲突**：使用自定义的RedisTemplate bean名称
2. **正确的依赖注入**：使用@Qualifier注解指定具体的bean
3. **简化序列化对象**：将复杂的Spring AI消息对象转换为简单的POJO
4. **API方法修正**：使用正确的消息内容获取方法
5. **异常处理**：确保系统在数据损坏时仍能正常运行

这种方案具有良好的可维护性和扩展性，成功解决了Spring AI消息序列化的问题。