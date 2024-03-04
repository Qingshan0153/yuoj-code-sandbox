package com.yuoj.sanbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import com.yuoj.sanbox.utils.DockerUtil;

/**
 * @author 李京霖
 * @version 2024/3/4 11:21 1.0
 */
public class DockerDemo {


    public static void main(String[] args) throws InterruptedException {
        DockerClient dockerClient = DockerUtil.connect();
        String image = "nginx:latest";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像："+item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
//        System.out.println("镜像拉取完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse containerResponse = containerCmd
                .withCmd("echo", "Hello Docker")
                .exec();
        System.out.println("创建容器id:"+containerResponse.getId());
        String containerId = containerResponse.getId();
    }


}
