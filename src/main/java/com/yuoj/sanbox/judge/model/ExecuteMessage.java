package com.yuoj.sanbox.judge.model;

import lombok.Data;

/**
 * @author 李京霖
 * @version 2024/3/4 14:34 1.0
 */

@Data
public class ExecuteMessage {
    /**
     * 程序退出码
     */
    private Integer exitValue;

    /**
     * 消息
     */
    private String message;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 运行时间
     */
    private Long time;

    /**
     * 内存
     */
    private Long memory;

}
