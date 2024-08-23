package com.cisco.sampletest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogProcessor2 {

    private static final String LOG_FILE_PATH = "/Users/apps/datafiles_1/logs/text.txt";
    private static final String COMP_PATTERN = "~\\s*(.*?)\\s*~";
    private static final String EXCEPTION_PATTERN = "(\\w+(\\.\\w+)*Exception):";

    public static void main(String[] args) {
        Pattern compPattern = Pattern.compile(COMP_PATTERN);
        Pattern exceptionPattern = Pattern.compile(EXCEPTION_PATTERN);
        Map<String, Map<String, String>> compExecpMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher compMatcher = compPattern.matcher(line);
                Matcher exceptionMatcher = exceptionPattern.matcher(line);

                if (compMatcher.find() && exceptionMatcher.find()) {
                    String component = compMatcher.group(1);
                    String exception = exceptionMatcher.group(1);

                    Map<String, String> exceptionMap = compExecpMap.computeIfAbsent(component, k -> new HashMap<>());
                    exceptionMap.merge(exception, "1", (oldValue, newValue) -> String.valueOf(Integer.parseInt(oldValue) + 1));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print the map
        for (Map.Entry<String, Map<String, String>> entry : compExecpMap.entrySet()) {
            String component = entry.getKey();
            Map<String, String> exceptionMap = entry.getValue();
            for (Map.Entry<String, String> exceptionEntry : exceptionMap.entrySet()) {
                String exception = exceptionEntry.getKey();
                String count = exceptionEntry.getValue();
                System.out.println("Component: " + component + ", Exception: " + exception + ", Count: " + count);
            }
        }
    }
}