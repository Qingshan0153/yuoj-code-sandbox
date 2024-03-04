package com.yuoj.sanbox.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author 李京霖
 * @version 2024/3/4 11:32 1.0
 */

@Component
public class DockerUtil {
    private final static String HOST = "tcp://101.42.24.207:2375";
    private final static String API_VERSION = "1.41";

    /**
     * 连接docker服务器
     *
     * @return DockerClient
     */
    public static DockerClient connect() {
        // 配置docker CLI的一些选项
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(HOST)
                // 与docker版本对应，参考https://docs.docker.com/engine/api/#api-version-matrix
                // 或者通过docker version指令查看api version
                .withApiVersion(API_VERSION)
                .build();

        // 创建DockerHttpClient
        DockerHttpClient httpClient = new ApacheDockerHttpClient
                .Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

}
