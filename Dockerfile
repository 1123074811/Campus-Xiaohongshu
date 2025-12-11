FROM library/openjdk:21-ea-21-jdk-oracle

# 设置工作目录
WORKDIR /app

# 暴露端口（与应用配置保持一致）
EXPOSE 9090

# 复制 Maven 打包后的 JAR 到容器中并命名为 app.jar
# 注意：这里的路径是容器构建上下文中的路径，一般为 target 目录下的最终 JAR
COPY target/springboot-0.0.1-SNAPSHOT.jar app.jar

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
