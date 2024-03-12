#1.定义父镜像(定义当前工程依赖的环境)：
FROM openjdk:8-alpine
#2.定义作者信息（可以省略不写）：
MAINTAINER  mendax
# 指定工作目录
WORKDIR /app
#3.将jar包添加到容器（将jar包存入镜像中）：
ADD ./target/yuoj-code-sandbox-1.0-SNAPSHOT.jar yuoj-code-sandbox-docker.jar
#4.指定这个容器对外暴露的端口号
EXPOSE 18888
#5.定义容器启动执行的命令： 当通过此镜像启动容器的时候，执行的命令
ENTRYPOINT ["java","-jar","yuoj-code-sandbox-docker.jar","--spring.profiles.active=prod"]