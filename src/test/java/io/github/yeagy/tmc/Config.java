package io.github.yeagy.tmc;

import lombok.Data;

@Data
public class Config {
    private final String app;
    private final HttpConfig lb;
    private final HttpConfig direct;
    private final LoggingConfig logging;

    @Data
    public static class HttpConfig {
        private final String host;
        private final int port;
        private final String timeout;
        private final boolean https;
    }

    @Data
    public static class LoggingConfig {
        private final String level;
        private final NeedlessConfig submodule;
    }

    @Data
    public static class NeedlessConfig {
        private final String foo;
    }
}
