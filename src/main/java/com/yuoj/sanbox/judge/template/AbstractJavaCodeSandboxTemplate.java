package com.yuoj.sanbox.judge.template;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yuoj.sanbox.judge.CodeSandbox;
import com.yuoj.sanbox.judge.model.ExecuteCodeRequest;
import com.yuoj.sanbox.judge.model.ExecuteCodeResponse;
import com.yuoj.sanbox.judge.model.ExecuteMessage;
import com.yuoj.sanbox.judge.model.JudgeInfo;
import com.yuoj.sanbox.utils.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Java 代码沙箱模板类
 *
 * @author 李京霖
 * @version 2024/3/4 16:54 1.0
 */


@Slf4j
public abstract class AbstractJavaCodeSandboxTemplate implements CodeSandbox {


    /**
     * 全局代码目录
     */
    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    /**
     * 全局Java类名
     */
    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";


    private static final long TIME_OUT = 5000L;

    private static final String USER_CODE_PARENT_PATH;

    static {
        USER_CODE_PARENT_PATH = initPath();
    }


    public static String initPath() {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在,不存在新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 用户代码隔离存放
        return globalCodePathName + File.separator + UUID.randomUUID();
    }


    /**
     * 1. 保存用户代码为文件
     *
     * @param code 代码
     * @return File
     */
    public File saveCodeToFile(String code) {
        String userCodePath = USER_CODE_PARENT_PATH + File.separator + GLOBAL_JAVA_CLASS_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2. 编译代码，得到class文件
     *
     * @param userCodeFile 代码文件
     */
    public void compileFile(File userCodeFile) {
        // 编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * // 3. 执行代码，得到输出结果
     *
     * @return List<ExecuteMessage>
     */
    public List<ExecuteMessage> runCode(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(runProcess, "运行");
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }


    /**
     * 收集整理输出结果
     *
     * @param executeMessageList 提交信息列表
     * @return ExecuteCodeResponse
     */
    public ExecuteCodeResponse getOutput(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 清理文件
     *
     * @param userCodeFile 代码文件
     * @return boolean
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            return FileUtil.del(USER_CODE_PARENT_PATH);
        }
        return true;
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest 执行请求
     * @return ExecuteCodeResponse
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        // 1. 保存用户代码为文件
        File userCodeFile = this.saveCodeToFile(code);
        // 2. 编译代码，得到class文件
        this.compileFile(userCodeFile);
        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = this.runCode(userCodeFile, inputList);
        // 4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = this.getOutput(executeMessageList);

        // 5. 文件清理
        boolean isDel = this.deleteFile(userCodeFile);
        if (!isDel) {
            log.error("文件：{},删除失败!", USER_CODE_PARENT_PATH);
        }
        return executeCodeResponse;

    }

}
