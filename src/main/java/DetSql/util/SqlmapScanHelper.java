package DetSql.util;

import DetSql.config.SqlmapConfig;
import DetSql.core.SqlmapStarter;
import DetSql.logging.DetSqlLogger;
import DetSql.ui.Messages;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;

import javax.swing.*;
import java.util.*;

/**
 * SqlMap 扫描辅助类
 * 负责将指定参数的值替换为 * (SqlMap 注入点标记) 并启动扫描
 */
public class SqlmapScanHelper {

    private static final String INJECTION_MARKER = "*";

    /**
     * 扫描指定参数
     * 
     * @param logger 日志记录器
     * @param request 原始 HTTP 请求
     * @param paramNames 要扫描的参数名集合
     */
    public static void scanParameters(DetSqlLogger logger, HttpRequest request, Set<String> paramNames) {
        if (request == null || paramNames == null || paramNames.isEmpty()) {
            JOptionPane.showMessageDialog(null, Messages.getString("sqlmap.request_or_params_empty"),
                    "SqlMap", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 将参数值替换为 *
            HttpRequest modifiedRequest = replaceParameterValues(request, paramNames);

            // 获取修改后的请求字节
            byte[] requestBytes = modifiedRequest.toByteArray().getBytes();

            // 启动 SqlMap
            logger.info("[SqlMap] 开始扫描参数: " + String.join(", ", paramNames));
            SqlmapStarter.start(logger, requestBytes);

        } catch (Exception e) {
            logger.error("[SqlMap] 扫描失败: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    Messages.getString("sqlmap.scan_failed") + ": " + e.getMessage(),
                    Messages.getString("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 将指定参数的值替换为 * (SqlMap 注入点标记)
     * 
     * @param request 原始请求
     * @param paramNames 要替换的参数名集合
     * @return 修改后的请求
     */
    public static HttpRequest replaceParameterValues(HttpRequest request, Set<String> paramNames) {
        HttpRequest modifiedRequest = request;
        
        // 处理 URL 参数
        modifiedRequest = replaceUrlParameters(modifiedRequest, paramNames);
        
        // 处理 BODY 参数
        modifiedRequest = replaceBodyParameters(modifiedRequest, paramNames);
        
        // 处理 JSON 参数
        modifiedRequest = replaceJsonParameters(modifiedRequest, paramNames);
        
        // 处理 XML 参数
        modifiedRequest = replaceXmlParameters(modifiedRequest, paramNames);
        
        // 处理 COOKIE 参数
        modifiedRequest = replaceCookieParameters(modifiedRequest, paramNames);
        
        return modifiedRequest;
    }

    private static HttpRequest replaceUrlParameters(HttpRequest request, Set<String> paramNames) {
        List<ParsedHttpParameter> params = request.parameters(HttpParameterType.URL);
        if (params.isEmpty()) return request;
        
        List<HttpParameter> newParams = new ArrayList<>();
        boolean modified = false;
        
        for (ParsedHttpParameter param : params) {
            if (paramNames.contains(param.name())) {
                newParams.add(HttpParameter.urlParameter(param.name(), INJECTION_MARKER));
                modified = true;
            } else {
                newParams.add(HttpParameter.urlParameter(param.name(), param.value()));
            }
        }
        
        return modified ? request.withUpdatedParameters(newParams) : request;
    }

    private static HttpRequest replaceBodyParameters(HttpRequest request, Set<String> paramNames) {
        List<ParsedHttpParameter> params = request.parameters(HttpParameterType.BODY);
        if (params.isEmpty()) return request;
        
        List<HttpParameter> newParams = new ArrayList<>();
        boolean modified = false;
        
        for (ParsedHttpParameter param : params) {
            if (paramNames.contains(param.name())) {
                newParams.add(HttpParameter.bodyParameter(param.name(), INJECTION_MARKER));
                modified = true;
            } else {
                newParams.add(HttpParameter.bodyParameter(param.name(), param.value()));
            }
        }
        
        return modified ? request.withUpdatedParameters(newParams) : request;
    }

    private static HttpRequest replaceJsonParameters(HttpRequest request, Set<String> paramNames) {
        List<ParsedHttpParameter> params = request.parameters(HttpParameterType.JSON);
        if (params.isEmpty()) return request;
        
        String body = request.body().toString();
        int bodyOffset = request.bodyOffset();
        
        // 按偏移量倒序排列，避免替换时偏移量变化
        List<ParsedHttpParameter> sortedParams = new ArrayList<>(params);
        sortedParams.sort((a, b) -> Integer.compare(
                b.valueOffsets().startIndexInclusive(), 
                a.valueOffsets().startIndexInclusive()));
        
        for (ParsedHttpParameter param : sortedParams) {
            if (paramNames.contains(param.name())) {
                int relativeStart = param.valueOffsets().startIndexInclusive() - bodyOffset;
                int relativeEnd = param.valueOffsets().endIndexExclusive() - bodyOffset;
                
                if (relativeStart >= 0 && relativeEnd <= body.length()) {
                    body = body.substring(0, relativeStart) + INJECTION_MARKER + body.substring(relativeEnd);
                }
            }
        }
        
        return request.withBody(body);
    }

    private static HttpRequest replaceXmlParameters(HttpRequest request, Set<String> paramNames) {
        List<ParsedHttpParameter> params = request.parameters(HttpParameterType.XML);
        if (params.isEmpty()) return request;
        
        String body = request.body().toString();
        int bodyOffset = request.bodyOffset();
        
        List<ParsedHttpParameter> sortedParams = new ArrayList<>(params);
        sortedParams.sort((a, b) -> Integer.compare(
                b.valueOffsets().startIndexInclusive(), 
                a.valueOffsets().startIndexInclusive()));
        
        for (ParsedHttpParameter param : sortedParams) {
            if (paramNames.contains(param.name())) {
                int relativeStart = param.valueOffsets().startIndexInclusive() - bodyOffset;
                int relativeEnd = param.valueOffsets().endIndexExclusive() - bodyOffset;
                
                if (relativeStart >= 0 && relativeEnd <= body.length()) {
                    body = body.substring(0, relativeStart) + INJECTION_MARKER + body.substring(relativeEnd);
                }
            }
        }
        
        return request.withBody(body);
    }

    private static HttpRequest replaceCookieParameters(HttpRequest request, Set<String> paramNames) {
        List<ParsedHttpParameter> params = request.parameters(HttpParameterType.COOKIE);
        if (params.isEmpty()) return request;
        
        List<HttpParameter> newParams = new ArrayList<>();
        boolean modified = false;
        
        for (ParsedHttpParameter param : params) {
            if (paramNames.contains(param.name())) {
                newParams.add(HttpParameter.cookieParameter(param.name(), INJECTION_MARKER));
                modified = true;
            } else {
                newParams.add(HttpParameter.cookieParameter(param.name(), param.value()));
            }
        }
        
        return modified ? request.withUpdatedParameters(newParams) : request;
    }
}

