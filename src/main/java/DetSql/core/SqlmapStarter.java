package DetSql.core;

import DetSql.config.SqlmapConfig;
import DetSql.logging.DetSqlLogger;
import DetSql.util.SqlmapUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SQLMap 启动器
 * 负责在新终端窗口中启动 SQLMap 扫描
 */
public class SqlmapStarter implements Runnable {

    private final DetSqlLogger logger;
    private final byte[] requestBytes;

    /**
     * 构造函数
     * @param logger 日志记录器
     * @param requestBytes HTTP 请求字节数组
     */
    public SqlmapStarter(DetSqlLogger logger, byte[] requestBytes) {
        this.logger = logger;
        this.requestBytes = requestBytes;
    }

    @Override
    public void run() {
        try {
            // 保存请求到临时文件
            String tempFilePath = SqlmapUtils.saveRequestToTempFile(requestBytes, "sqlmap_request.txt");
            logger.info("[SQLMap] 请求已保存到: " + tempFilePath);

            // 构建完整命令
            String command = SqlmapConfig.buildFullCommand();
            logger.info("[SQLMap] 执行命令: " + command);

            // 获取平台特定的命令列表
            List<String> cmds = SqlmapUtils.buildPlatformCommands(command);

            if (cmds.isEmpty()) {
                String errorMsg = "无法构建 SQLMap 命令";
                logger.error("[SQLMap] " + errorMsg);
                JOptionPane.showMessageDialog(null, errorMsg, "SQLMap 错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int osType = SqlmapUtils.getOSType();

            // 对于 Linux，复制命令到剪贴板作为备用
            if (osType == SqlmapUtils.OS_LINUX) {
                SqlmapUtils.setSysClipboardText(command);
                logger.info("[SQLMap] 命令已复制到剪贴板");
                JOptionPane.showMessageDialog(null,
                        "命令已复制到剪贴板，如果终端未自动打开，请手动粘贴执行",
                        "SQLMap", JOptionPane.INFORMATION_MESSAGE);
            }

            // 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(cmds);
            Process process = processBuilder.start();

            // 异步读取输出
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[SQLMap] " + line);
                    }
                } catch (IOException e) {
                    // 静默处理
                }
            }).start();

            // 异步读取错误输出
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.error("[SQLMap] " + line);
                    }
                } catch (IOException e) {
                    // 静默处理
                }
            }).start();

            logger.info("[SQLMap] 已启动 SQLMap 扫描");

        } catch (IOException e) {
            logger.error("[SQLMap] 启动失败: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "SQLMap 启动失败: " + e.getMessage(),
                    "SQLMap 错误", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            logger.error("[SQLMap] 未知错误: " + e.getMessage());
        }
    }

    /**
     * 启动 SQLMap 扫描（便捷方法）
     * @param logger 日志记录器
     * @param requestBytes HTTP 请求字节数组
     */
    public static void start(DetSqlLogger logger, byte[] requestBytes) {
        new Thread(new SqlmapStarter(logger, requestBytes)).start();
    }
}