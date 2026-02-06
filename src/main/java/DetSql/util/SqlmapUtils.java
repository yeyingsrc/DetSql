package DetSql.util;

import DetSql.config.SqlmapConfig;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * sqlmap 工具类
 * 提供跨平台的 sqlmap 调用支持
 */
public class SqlmapUtils {
    public static final int OS_WIN = 1;
    public static final int OS_MAC = 2;
    public static final int OS_LINUX = 3;
    public static final int OS_UNKNOWN = 4;

    /**
     * 获取操作系统名称
     */
    public static String getOSName() {
        return System.getProperties().getProperty("os.name").toUpperCase();
    }

    /**
     * 获取操作系统类型
     */
    public static int getOSType() {
        String osName = getOSName();
        if (osName.contains("WINDOW")) {
            return OS_WIN;
        } else if (osName.contains("MAC")) {
            return OS_MAC;
        } else if (osName.contains("LINUX")) {
            return OS_LINUX;
        } else {
            return OS_UNKNOWN;
        }
    }

    /**
     * 写入文件
     */
    public static void writeFile(byte[] bytes, String filepath) {
        try (FileOutputStream fos = new FileOutputStream(filepath)) {
            fos.write(bytes);
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 获取临时请求文件路径
     */
    public static String getTempRequestFilePath(String filename) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + filename;
        SqlmapConfig.setRequestFilePath(filePath);
        return filePath;
    }

    /**
     * 创建 Windows BAT 脚本文件
     */
    public static String makeBatFile(String filename, String content) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String batFile = tempDir + File.separator + filename;
        String sysEncoding = System.getProperty("file.encoding");
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(batFile), sysEncoding);
             BufferedWriter writer = new BufferedWriter(write)) {
            writer.write("@echo off\n");
            writer.write("chcp 65001 >nul\n");
            writer.write(content);
            writer.write("\npause");
            return batFile;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建 Linux Shell 脚本文件
     */
    public static String makeShellScript(String filename, String content) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String shellFile = tempDir + File.separator + filename;
        try (OutputStreamWriter write = new OutputStreamWriter(
                new FileOutputStream(shellFile), StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(write)) {
            writer.write("#!/bin/bash\n");
            writer.write(content);
            writer.write("\nread -p 'Press Enter to continue...'");
            // 设置可执行权限
            new File(shellFile).setExecutable(true);
            return shellFile;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建 macOS AppleScript 命令
     */
    public static String makeAppleScriptCommand(String command) {
        // 转义双引号
        String escapedCommand = command.replace("\"", "\\\"");
        return String.format(
                "tell application \"Terminal\"\n" +
                "    activate\n" +
                "    do script \"%s\"\n" +
                "end tell", escapedCommand);
    }

    /**
     * 使用 Java ProcessBuilder 直接运行命令（用于 OS_UNKNOWN 情况）
     */
    public static Process runWithJavaProcess(String command, Consumer<String> outputHandler,
                                              Consumer<String> errorHandler) throws IOException {
        List<String> cmds = new ArrayList<>();
        int osType = getOSType();

        if (osType == OS_WIN) {
            cmds.add("cmd.exe");
            cmds.add("/c");
            cmds.add(command);
        } else {
            // Unix-like systems
            cmds.add("/bin/bash");
            cmds.add("-c");
            cmds.add(command);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(cmds);
        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();

        // 异步读取输出
        if (outputHandler != null) {
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputHandler.accept(line);
                    }
                } catch (IOException e) {
                    // 静默处理
                }
            }).start();
        }

        // 异步读取错误输出
        if (errorHandler != null) {
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorHandler.accept(line);
                    }
                } catch (IOException e) {
                    // 静默处理
                }
            }).start();
        }

        return process;
    }

    /**
     * 构建平台特定的命令列表
     */
    public static List<String> buildPlatformCommands(String command) {
        List<String> cmds = new ArrayList<>();
        int osType = getOSType();

        switch (osType) {
            case OS_WIN:
                String batFilePath = makeBatFile("sqlmap_scan.bat", command);
                if (batFilePath != null) {
                    cmds.add("cmd.exe");
                    cmds.add("/c");
                    cmds.add("start");
                    cmds.add(batFilePath);
                }
                break;

            case OS_MAC:
                cmds.add("osascript");
                cmds.add("-e");
                cmds.add(makeAppleScriptCommand(command));
                break;

            case OS_LINUX:
                String shellFilePath = makeShellScript("sqlmap_scan.sh", command);
                if (shellFilePath != null) {
                    // 尝试使用常见的终端模拟器
                    cmds.add("/bin/bash");
                    cmds.add("-c");
                    cmds.add("x-terminal-emulator -e " + shellFilePath +
                            " || gnome-terminal -- " + shellFilePath +
                            " || konsole -e " + shellFilePath +
                            " || xterm -e " + shellFilePath);
                }
                break;

            default:
                // OS_UNKNOWN: 直接使用 bash
                cmds.add("/bin/bash");
                cmds.add("-c");
                cmds.add(command);
                break;
        }

        return cmds;
    }

    /**
     * 设置系统剪贴板文本
     */
    public static void setSysClipboardText(String str) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(str);
        clip.setContents(tText, null);
    }

    /**
     * 保存 HTTP 请求到临时文件
     */
    public static String saveRequestToTempFile(byte[] requestBytes, String filename) {
        String filePath = getTempRequestFilePath(filename);
        writeFile(requestBytes, filePath);
        return filePath;
    }
}