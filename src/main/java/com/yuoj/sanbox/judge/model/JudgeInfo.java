package com.yuoj.sanbox.judge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 判题信息(json数组)
 *
 * @author 李京霖
 * @version 2024/2/29 11:01 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    private String message;


    /**
     * 运行时间
     */
    private Long time;

    /**
     * 消耗内存
     */
    private Long memory;

}
