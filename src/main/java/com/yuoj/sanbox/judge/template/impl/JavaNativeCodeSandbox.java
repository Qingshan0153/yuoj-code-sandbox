package com.yuoj.sanbox.judge.template.impl;

import com.yuoj.sanbox.judge.model.ExecuteCodeRequest;
import com.yuoj.sanbox.judge.model.ExecuteCodeResponse;
import com.yuoj.sanbox.judge.template.AbstractJavaCodeSandboxTemplate;
import org.springframework.stereotype.Service;

/**
 * java 原生代码沙箱实现（直接复用模板方法）
 *
 * @author 李京霖
 * @version 2024/3/4 21:30 1.0
 */
@Service
public class JavaNativeCodeSandbox extends AbstractJavaCodeSandboxTemplate {
    /**
     * 执行代码
     *
     * @param executeCodeRequest 执行请求
     * @return ExecuteCodeResponse
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
