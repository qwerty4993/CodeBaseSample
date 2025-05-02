package com.cisco.ui.api.config;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class XSSFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(XSSFilter.class.getName());
    private Map<Pattern, String> xssPatterns = new HashMap<>();
    private Map<String, String> xssData = new HashMap<>();


    @Override
    public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain) {
        HttpServletResponse response = (jakarta.servlet.http.HttpServletResponse) resp;
        try {
            /*if (!"true".equalsIgnoreCase(XSS_FILTER_ENABLED)) {
                chain.doFilter(request, response);
                return;
            }*/

            String json = "{\"checkParameters\":\"true\",\"checkHeaders\":\"true\",\"checkCookies\":\"true\"}";
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> validationFlags = objectMapper.readValue(json, Map.class);

            jakarta.servlet.http.HttpServletRequest httpRequest = ((jakarta.servlet.http.HttpServletRequest) request);

            boolean isXSSDetected = false;

            if ("true".equalsIgnoreCase(validationFlags.get("checkParameters"))) {
                isXSSDetected = checkParameters(httpRequest);
            }

            if (!isXSSDetected && "true".equalsIgnoreCase(validationFlags.get("checkHeaders"))) {
                isXSSDetected = checkHeaders(httpRequest);
            }

            if (!isXSSDetected && "true".equalsIgnoreCase(validationFlags.get("checkCookies"))) {
                isXSSDetected = checkCookies(httpRequest);
            }

            if (!isXSSDetected && checkRequestBody(httpRequest)) {
                isXSSDetected = true;
            }

            if (isXSSDetected) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String errorResponse = "{\"error\":\"Access forbidden due to security policy violation.\"}";
                PrintWriter out = response.getWriter();
                try {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.write(errorResponse);
                } finally {
                    out.flush();
                    out.close();
                }
                return;
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            LOGGER.severe("Error occurred during XSS filtering: " + e.getMessage());
        }
    }

    private void loadXSSPatternsFromDB() {
        // Placeholder for fetching patterns from a database or configuration file
         Map<String, String> XSS_PATTERNS = new HashMap<>() {{
            put("(?i)<script.*?>.*?</script.*?>", "Script Tag");
            put("(?i)<.*?javascript:.*?>", "Javascript Protocol");
            put("(?i)<.*?on.*?=.*?>", "Event Handler Attribute");
            put("(?i)<.*?eval\\(.*?\\).*?>", "Eval Function");
            put("(?i)<.*?alert\\(.*?\\).*?>", "Alert Function");
            put("(?i)<.*?expression\\(.*?\\).*?>", "CSS Expression");
            put("(?i)<.*?img.*?src=.*?javascript:.*?>", "Image with Javascript");
            put("(?i)<.*?(iframe|object|embed).*?>", "Iframe, Object, or Embed Tag");
            put("(?i)['\";]document\\.location", "Document Location Manipulation");
            put("(?i)['\";]window\\.location", "Window Location Manipulation");
        }};

        for (Map.Entry<String, String> row : XSS_PATTERNS.entrySet()) {
            String patternStr = row.getKey();
            String description =row.getValue();
            xssPatterns.put(Pattern.compile(patternStr, Pattern.DOTALL), description);
        }

        LOGGER.info("Loaded " + xssPatterns.size() + " XSS patterns.");
    }

    private Map<String, Object> createPatternRow(String pattern, String description) {
        Map<String, Object> row = new HashMap<>();
        row.put("pattern", pattern);
        row.put("description", description);
        return row;
    }

    private boolean checkParameters(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            if (isXSSDetected(paramValue)) {
                logBlockedRequest(request, paramName, paramValue, "Request Parameter");
                return true;
            }
        }
        return false;
    }

    private boolean checkHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (isXSSDetected(headerValue)) {
                logBlockedRequest(request, headerName, headerValue, "Request Header");
                return true;
            }
        }
        return false;
    }

    private boolean checkCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                String cookieValue = cookie.getValue();
                if (isXSSDetected(cookieValue)) {
                    logBlockedRequest(request, cookie.getName(), cookieValue, "Cookie");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkRequestBody(HttpServletRequest request) {
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod())) {
            String requestBody = extractRequestBody(request);
            if (isXSSDetected(requestBody)) {
                logBlockedRequest(request, "Request Body", requestBody, "Request Body");
                return true;
            }
        }
        return false;
    }

    private boolean isXSSDetected(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (Map.Entry<Pattern, String> entry : xssPatterns.entrySet()) {
            if (entry.getKey().matcher(value).find()) {
                xssData.put("description", entry.getValue());
                xssData.put("pattern", entry.getKey().pattern());
                xssData.put("matchedData", value);
                LOGGER.warning("XSS pattern detected: " +xssData.toString());
                return true;
            }
        }
        return false;
    }

    private void logBlockedRequest(HttpServletRequest request, String name, String value, String type) {
        LOGGER.warning("XSS attack detected in " + type + ": " + name);
        LOGGER.warning("Blocked Value: " + maskSensitiveData(value));
        LOGGER.warning("URL: " + request.getRequestURL());
    }

    private String maskSensitiveData(String value) {
        return value.replaceAll(".", "*");
    }


    private String extractRequestBody(HttpServletRequest request) {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to read request body: " + e.getMessage());
            return ""; // Return an empty body instead of throwing an exception
        }
        return body.toString();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        loadXSSPatternsFromDB();
    }

    @Override
    public void destroy() {
        // Cleanup logic if required
        xssPatterns.clear();
        xssData.clear();
        LOGGER.info("XSSFilter destroyed and patterns cleared.");
    }
}
