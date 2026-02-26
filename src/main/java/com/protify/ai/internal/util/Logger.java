/*
 * Copyright(c) 2026 Protify Consulting LLC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.protify.ai.internal.util;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.logging.LogRecord;

public final class Logger {

    private final System.Logger delegate;
    private final java.util.logging.Logger julDelegate;
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    Logger(System.Logger delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        // We grab the underlying JUL logger to manually submit LogRecords
        this.julDelegate = java.util.logging.Logger.getLogger(delegate.getName());
    }

    public void info(String msg) {
        log(Level.INFO, msg, null, null);
    }

    public void info(String format, Object... params) {
        if (isInfoEnabled()) {
            log(Level.INFO, format, params, null);
        }
    }

    public void debug(String msg) {
        log(Level.DEBUG, msg, null, null);
    }

    public void debug(String format, Object... params) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, format, params, null);
        }
    }

    public void warn(String msg) {
        log(Level.WARNING, msg, null, null);
    }

    public void warn(String format, Object... params) {
        if (isWarnEnabled()) {
            log(Level.WARNING, format, params, null);
        }
    }

    public void warn(String msg, Throwable thrown) {
        log(Level.WARNING, msg, null, thrown);
    }

    public void error(String msg) {
        log(Level.ERROR, msg, null, null);
    }

    public void error(String format, Object... params) {
        if (isErrorEnabled()) {
            log(Level.ERROR, format, params, null);
        }
    }

    public void error(String msg, Throwable thrown) {
        log(Level.ERROR, msg, null, thrown);
    }

    public void trace(String msg) {
        log(Level.TRACE, msg, null, null);
    }

    public void trace(String format, Object... params) {
        if (isTraceEnabled()) {
            log(Level.TRACE, format, params, null);
        }
    }

    private void log(Level level, String pattern, Object[] params, Throwable thrown) {
        String message = formatMessage(pattern, params);
        java.util.logging.Level julLevel = mapLevel(level);

        LogRecord logRecord = new LogRecord(julLevel, message);
        logRecord.setLoggerName(julDelegate.getName());
        logRecord.setThrown(thrown);

        // Manually find the caller so the log shows the real class/method
        WALKER.walk(frames -> frames
                .filter(f -> !f.getClassName().equals(Logger.class.getName()))
                .findFirst()
        ).ifPresent(caller -> {
            logRecord.setSourceClassName(caller.getClassName());
            logRecord.setSourceMethodName(caller.getMethodName());
        });

        julDelegate.log(logRecord);
    }

    private String formatMessage(String pattern, Object[] params) {
        if (pattern == null) return null;
        if (params == null || params.length == 0 || !pattern.contains("{}")) return pattern;

        StringBuilder sb = new StringBuilder(pattern.length() + 50);
        int lastIndex = 0;
        int paramIndex = 0;
        int placeholderIndex;
        while (paramIndex < params.length && (placeholderIndex = pattern.indexOf("{}", lastIndex)) != -1) {
            sb.append(pattern, lastIndex, placeholderIndex);
            Object param = params[paramIndex++];
            sb.append(param);
            lastIndex = placeholderIndex + 2;
        }
        sb.append(pattern.substring(lastIndex));
        return sb.toString();
    }

    private java.util.logging.Level mapLevel(Level level) {
        switch (level) {
            case TRACE: return java.util.logging.Level.FINEST;
            case DEBUG: return java.util.logging.Level.FINE;
            case INFO:  return java.util.logging.Level.INFO;
            case WARNING: return java.util.logging.Level.WARNING;
            case ERROR: return java.util.logging.Level.SEVERE;
            default: return java.util.logging.Level.INFO;
        }
    }

    public boolean isDebugEnabled() { return delegate.isLoggable(Level.DEBUG); }
    public boolean isTraceEnabled() { return delegate.isLoggable(Level.TRACE); }
    public boolean isInfoEnabled() { return delegate.isLoggable(Level.INFO); }
    public boolean isWarnEnabled() { return delegate.isLoggable(Level.WARNING); }
    public boolean isErrorEnabled() { return delegate.isLoggable(Level.ERROR); }
}
