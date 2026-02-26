package DetSql.config;

/**
 * sqlmap 配置类
 * 管理 sqlmap 的路径、命令参数等配置
 */
public class SqlmapConfig {
    // 默认 Python 解释器名称
    private static String PYTHON_NAME = "python";

    // 默认 sqlmap 路径
    private static String SQLMAP_PATH = "sqlmap";

    // 请求文件路径（临时文件）
    private static String REQUEST_FILE_PATH = "";

    // 默认 sqlmap 命令参数
    private static final String DEFAULT_SQLMAP_OPTIONS =
            "-o --random-agent --time-sec=3 --risk=3 --level=5 --current-db --tamper=space2comment --batch";

    // 当前 sqlmap 命令参数
    private static String SQLMAP_OPTIONS_COMMAND = DEFAULT_SQLMAP_OPTIONS;

    // 操作系统类型
    private static String OS_TYPE;

    // 是否正在注入
    private static boolean IS_INJECT = false;

    /**
     * 获取 Python 解释器名称
     */
    public static String getPythonName() {
        return PYTHON_NAME;
    }

    /**
     * 设置 Python 解释器名称
     */
    public static void setPythonName(String pythonName) {
        PYTHON_NAME = pythonName;
    }

    /**
     * 获取 sqlmap 路径
     */
    public static String getSqlmapPath() {
        return SQLMAP_PATH;
    }

    /**
     * 设置 sqlmap 路径
     */
    public static void setSqlmapPath(String sqlmapPath) {
        SQLMAP_PATH = sqlmapPath;
    }

    /**
     * 获取请求文件路径
     */
    public static String getRequestFilePath() {
        return REQUEST_FILE_PATH;
    }

    /**
     * 设置请求文件路径
     */
    public static void setRequestFilePath(String requestFilePath) {
        REQUEST_FILE_PATH = requestFilePath;
    }

    /**
     * 获取 sqlmap 命令参数
     */
    public static String getSqlmapOptionsCommand() {
        return SQLMAP_OPTIONS_COMMAND;
    }

    /**
     * 设置 sqlmap 命令参数
     */
    public static void setSqlmapOptionsCommand(String sqlmapOptionsCommand) {
        SQLMAP_OPTIONS_COMMAND = sqlmapOptionsCommand;
    }

    /**
     * 获取默认 sqlmap 命令参数
     */
    public static String getDefaultSqlmapOptions() {
        return DEFAULT_SQLMAP_OPTIONS;
    }

    /**
     * 获取操作系统类型
     */
    public static String getOsType() {
        return OS_TYPE;
    }

    /**
     * 设置操作系统类型
     */
    public static void setOsType(String osType) {
        OS_TYPE = osType;
    }

    /**
     * 是否正在注入
     */
    public static boolean isInjecting() {
        return IS_INJECT;
    }

    /**
     * 设置注入状态
     */
    public static void setInjecting(boolean isInject) {
        IS_INJECT = isInject;
    }

    /**
     * 构建完整的 sqlmap 命令
     * @return 完整的 sqlmap 命令字符串
     */
    public static String buildFullCommand() {
        return String.format("%s \"%s\" -r \"%s\" %s",
                PYTHON_NAME, SQLMAP_PATH, REQUEST_FILE_PATH, SQLMAP_OPTIONS_COMMAND);
    }

    /**
     * 重置为默认配置
     */
    public static void resetToDefaults() {
        PYTHON_NAME = "python";
        SQLMAP_PATH = "sqlmap";
        SQLMAP_OPTIONS_COMMAND = DEFAULT_SQLMAP_OPTIONS;
        REQUEST_FILE_PATH = "";
        IS_INJECT = false;
    }
}