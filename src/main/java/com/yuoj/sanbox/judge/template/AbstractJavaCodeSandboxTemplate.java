package com.yuoj.sanbox.judge.template;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.yuoj.sanbox.judge.CodeSandbox;
import com.yuoj.sanbox.judge.model.ExecuteCodeRequest;
import com.yuoj.sanbox.judge.model.ExecuteCodeResponse;
import com.yuoj.sanbox.judge.model.ExecuteMessage;
import com.yuoj.sanbox.judge.model.JudgeInfo;
import com.yuoj.sanbox.judge.model.enums.JudgeInfoMessageEnum;
import com.yuoj.sanbox.judge.model.enums.QuestionSubmitStatusEnum;
import com.yuoj.sanbox.utils.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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


    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_CODE_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 10000L;

    /**
     * Java安全管理器类存放路径
     */
    private static final String SECURITY_MANAGER_PATH;

    /**
     * Java安全管理器类名
     */
    private static final String SECURITY_MANAGER_CLASS_NAME = "CustomSecurityManager";

    /**
     * Java代码黑名单
     * 黑名单检测通常用于辅助安全策略，而不是作为唯一的安全手段
     */
    private static final List<String> blackList = Arrays.asList(
            // 文件操作相关
            "Files", "File", "FileInputStream", "FileOutputStream", "RandomAccessFile", "FileReader", "FileWriter", "FileChannel", "FileLock", "Path", "Paths", "File.createTempFile", "File.createTempDirectory", "ZipInputStream", "ZipOutputStream",

            // 网络相关
            "Socket", "ServerSocket", "DatagramSocket", "InetAddress", "URL", "URLConnection", "HttpURLConnection", "SocketChannel", "ServerSocketChannel", "DatagramChannel", "SocketPermission", "ServerSocketPermission",

            // 系统命令执行相关
            "exec", "Runtime.getRuntime().exec", "ProcessBuilder", "SecurityManager", "System.exit", "Runtime.getRuntime().halt", "SecurityManager.checkExec",

            // 反射相关
            "Class.forName", "Method.invoke", "sun.reflect.", "java.lang.reflect.", "Unsafe", "sun.misc.Unsafe", "sun.reflect.Unsafe", "Proxy",

            // 数据库相关
            "Statement", "PreparedStatement", "CallableStatement", "DataSource", "Connection", "ResultSet", "Hibernate", "JPA", // 防止使用 ORM 框架执行不安全的数据库操作
            "createStatement", "prepareStatement", "prepareCall",

            // 不安全的操作
            "Unsafe", "sun.misc.Unsafe", "sun.reflect.Unsafe",

            // 加密解密相关
            "Cipher", "MessageDigest", "KeyGenerator", "KeyPairGenerator", "SecretKeyFactory", "KeyStore", "SecureRandom", "java.security.",

            // 序列化相关
            "ObjectInputStream", "ObjectOutputStream", "Serializable", "Externalizable", "readObject", "writeObject",

            // 线程相关
            "Thread", "Runnable", "Executor", "ExecutorService", "ThreadPoolExecutor", "ThreadGroup", "ThreadLocal", "Thread.sleep", "Thread.yield", "Thread.stop", "Thread.suspend", "Thread.resume", "java.util.concurrent.",

            // 安全管理器相关
            "SecurityManager",

            // 其他可能导致安全问题的操作
            "System.load", "System.loadLibrary", // 防止加载本地库
            "JNI", "Java Native Interface", // 防止使用 JNI 调用本地代码
            "Unsafe.allocateMemory", "Unsafe.freeMemory", // 直接内存操作
            "System.getProperties", "System.setProperty", // 系统属性操作
            "System.getenv", // 获取环境变量
            "System.console", // 控制台访问
            "Runtime.addShutdownHook", // 添加关闭钩子
            "Runtime.load", "Runtime.loadLibrary" // 加载本地库
    );
    /**
     * 代码黑名单字典树
     */
    private static final WordTree WORD_TREE;

    static {
        // 初始化黑名单字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
        // 初始安全配置文件路径
        SECURITY_MANAGER_PATH = System.getProperty("user.dir");
    }

    /**
     * 完整的流程
     *
     * @param executeCodeRequest 代码请求
     * @return ExecuteCodeResponse
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        // 0. 安全控制：限制敏感代码：黑名单检测
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null) {
            System.out.println("包含禁止词：" + foundWord.getFoundWord());
            // 返回错误信息
            return new ExecuteCodeResponse(null, "包含禁止词：" + foundWord.getFoundWord(), QuestionSubmitStatusEnum.FAILED.getValue(), new JudgeInfo(JudgeInfoMessageEnum.DANGEROUS_OPERATION.getValue(), null, null));
        }

        // 1、将用户提交的代码保存为文件。
        File userCodeFile = saveCodeToFile(code);

        // 2、编译代码，得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        log.info("编译后信息：{}", compileFileExecuteMessage);
        if (compileFileExecuteMessage.getErrorMessage() != null) {
            // 返回编译错误信息
            return new ExecuteCodeResponse(null, compileFileExecuteMessage.getMessage(), QuestionSubmitStatusEnum.FAILED.getValue(), new JudgeInfo(compileFileExecuteMessage.getErrorMessage(), null, null));
        }
        // 3、执行程序
        List<ExecuteMessage> executeMessageList = runCode(userCodeFile, inputList);

        // 4、整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5、文件清理
        boolean del = clearFile(userCodeFile);
        if (!del) {
            log.error("deleteFile Field ,userCodeFilePath = {}", userCodeFile.getAbsoluteFile());
        }

        return outputResponse;
    }

    /**
     * 1、将用户提交的代码保存为文件。
     *
     * @param code 代码
     * @return File
     */
    public File saveCodeToFile(String code) {
        // 获取用户工作文件路径
        String userDir = System.getProperty("user.dir");
        //  File.separator 区分不太系统的分隔符：\\ or /
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局目录路径是否存在
        if (!FileUtil.exist(globalCodePathName)) {
            // 不存在，则创建文件目录
            FileUtil.mkdir(globalCodePathName);
        }
        // 存在，则保存用户提交代码
        // 把用户的代码隔离存放，
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        // 实际存放文件的目录
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_CODE_CLASS_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2、编译代码，得到class文件
     *
     * @param userCodeFile 代码文件
     * @return ExecuteMessage
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误！");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3、执行程序文件，获得执行结果列表
     *
     * @param userCodeFile 代码文件
     * @param inputList    输入
     * @return List<ExecuteMessage>
     */
    public List<ExecuteMessage> runCode(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 安全控制：限制资源分配：最大队资源大小：128MB
            // 安全控制：配置安全管理器：java.lang.SecurityManager
//            String runCmd = String.format("java -Xmx128m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
//            String osName = System.getProperty("os.name").toLowerCase();
            // 如果是Windows系统，支持安全管理器security-manager的创建，反之是Linux则不支持（可能也支持，但作者暂时因时间原因未找出对策，故出此下策）
//            if (osName.contains("nix") || osName.contains("nux")) {
            String runCmd = String.format("java -Xmx128m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
//            }
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        log.warn("程序运行超时，已经中断");
                        runProcess.destroy();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(runProcess, "运行");
                log.info("代码程序执行信息：" + executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("程序执行错误" + e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4、获取响应输出结果
     *
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        // 执行错误
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交程序执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long executeTime = executeMessage.getTime();
            if (executeTime != null) {
                maxTime = Math.max(maxTime, executeTime);
            }
        }
        // 正常执行完成
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
     * 5、清理文件
     *
     * @param userCodeFile
     * @return
     */
    public boolean clearFile(File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            log.info("删除" + (del ? "成功！" : "失败！"));
            return del;
        }
        return true;
    }

}
