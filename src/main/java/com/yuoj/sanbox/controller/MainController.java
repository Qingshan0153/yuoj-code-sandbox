package com.yuoj.sanbox.controller;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import com.yuoj.sanbox.judge.model.ExecuteCodeRequest;
import com.yuoj.sanbox.judge.model.ExecuteCodeResponse;
import com.yuoj.sanbox.judge.template.impl.JavaDockerCodeSandbox;
import com.yuoj.sanbox.judge.template.impl.JavaNativeCodeSandbox;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * @author 李京霖
 * @version 2024/3/3 20:13 1.0
 */

@RestController
@RequestMapping("/sandbox")
public class MainController {

    /**
     * 定义鉴权请求头和密钥
     */
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "cB6nU2qD8gY1fE9";
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "Ok";
    }

    @PostMapping("/execute")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response) {
        String header = request.getHeader(AUTH_REQUEST_HEADER);
        String secretKey = DigestUtil.md5Hex(AUTH_REQUEST_SECRET, StandardCharsets.UTF_8);
        if (!header.equals(secretKey)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }
}
