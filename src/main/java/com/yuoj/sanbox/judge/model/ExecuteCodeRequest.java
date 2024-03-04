package com.yuoj.sanbox.judge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 李京霖
 * @version 2024/3/3 2:18 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecuteCodeRequest {

    /**
     * 输入用例
     */
    private List<String> inputList;

    /**
     * 执行代码
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;
}
