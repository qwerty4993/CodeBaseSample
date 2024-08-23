package com.example.logging;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.util.Strings;

@Plugin(name = "CustomThrowablePatternConverter", category = "Converter")
public class CustomThrowablePatternConverter extends ThrowablePatternConverter {

    protected CustomThrowablePatternConverter(String[] options) {
        super("CustomThrowable", "throwable", options);
    }

    @PluginFactory
    public static CustomThrowablePatternConverter newInstance(String[] options) {
        return new CustomThrowablePatternConverter(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        Throwable throwable = event.getThrown();
        if (throwable != null) {
            List<StackTraceElement> filteredStackTrace = new ArrayList<>();
            for (StackTraceElement element : throwable.getStackTrace()) {
                // Add your custom filtering logic here
                if (!element.getClassName().startsWith("java.") &&
                    !element.getClassName().startsWith("javax.") &&
                    !element.getClassName().startsWith("sun.") &&
                    !element.getClassName().startsWith("com.sun.") &&
                    !element.getClassName().startsWith("org.springframework.")) {
                    filteredStackTrace.add(element);
                }
            }
            toAppendTo.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append(Strings.LINE_SEPARATOR);
            for (StackTraceElement element : filteredStackTrace) {
                toAppendTo.append("\tat ").append(element.toString()).append(Strings.LINE_SEPARATOR);
            }
        }
    }
}

/*
<Configuration status="WARN" packages="com.example.logging">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{ISO8601} [%t] %-5p %c{1} - %m %xEx{full}%n" />
    </Console>
  </Appenders>

  <Loggers>
    <Root level="error">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
 */