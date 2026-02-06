package DetSql.core;

import DetSql.config.SqlmapConfig;
import DetSql.logging.DetSqlLogger;
import DetSql.ui.Messages;
import DetSql.util.SqlmapUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * sqlmap 启动器
 * 负责在新终端窗口中启动 sqlmap 扫描
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
            logger.info("[sqlmap] 请求已保存到: " + tempFilePath);

            // 构建完整命令
            String command = SqlmapConfig.buildFullCommand();
            logger.info("[sqlmap] 执行命令: " + command);

            // 获取平台特定的命令列表
            List<String> cmds = SqlmapUtils.buildPlatformCommands(command);

            if (cmds.isEmpty()) {
                String errorMsg = Messages.getString("sqlmap.cannot_build_command");
                logger.error("[sqlmap] " + errorMsg);
                JOptionPane.showMessageDialog(null, errorMsg, Messages.getString("dialog.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            int osType = SqlmapUtils.getOSType();

            // 对于 Linux，复制命令到剪贴板作为备用
            if (osType == SqlmapUtils.OS_LINUX) {
                SqlmapUtils.setSysClipboardText(command);
                logger.info("[sqlmap] 命令已复制到剪贴板");
                JOptionPane.showMessageDialog(null,
                        Messages.getString("sqlmap.linux_clipboard"),
                        "sqlmap", JOptionPane.INFORMATION_MESSAGE);
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
                        logger.info("[sqlmap] " + line);
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
                        logger.error("[sqlmap] " + line);
                    }
                } catch (IOException e) {
                    // 静默处理
                }
            }).start();

            logger.info("[sqlmap] 已启动 sqlmap 扫描");

        } catch (IOException e) {
            logger.error("[sqlmap] 启动失败: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    Messages.getString("sqlmap.start_failed") + ": " + e.getMessage(),
                    Messages.getString("dialog.error"), JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            logger.error("[sqlmap] 未知错误: " + e.getMessage());
        }
    }

    /**
     * 启动 sqlmap 扫描（便捷方法）
     * @param logger 日志记录器
     * @param requestBytes HTTP 请求字节数组
     */
    public static void start(DetSqlLogger logger, byte[] requestBytes) {
        new Thread(new SqlmapStarter(logger, requestBytes)).start();
    }
}