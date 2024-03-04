package com.yuoj.sanbox.judge;

import com.yuoj.sanbox.judge.model.ExecuteCodeRequest;
import com.yuoj.sanbox.judge.model.ExecuteCodeResponse;

/**
 * 代码沙箱请求接口
 * 扩展：查看代码沙箱状态接口
 *
 * @author 李京霖
 * @version 2024/3/3 2:18 1.0
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest 执行请求
     * @return ExecuteCodeResponse
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
