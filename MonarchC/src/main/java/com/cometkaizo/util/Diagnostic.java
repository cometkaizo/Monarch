package com.cometkaizo.util;

public interface Diagnostic {
    String getString();

    record Error(Throwable e, String message) implements Diagnostic {
        public Error(String message) {
            this(null, message);
        }
        @Override
        public String getString() {
            String errorMsg = StringUtils.getAbbreviatedMessage(e);
            return message + (errorMsg.isBlank() ? "" : "\n\n" + errorMsg);
        }
    }

    record Warning(Throwable e, String message) implements Diagnostic {
        public Warning(String message) {
            this(null, message);
        }
        @Override
        public String getString() {
            String errorMsg = StringUtils.getAbbreviatedMessage(e);
            return message + (errorMsg.isBlank() ? "" : "\n\n" + errorMsg);
        }
    }
}
