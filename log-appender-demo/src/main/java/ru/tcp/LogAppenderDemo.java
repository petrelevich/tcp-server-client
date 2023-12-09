package ru.tcp;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("java:S106")
public class LogAppenderDemo {
    private static final Logger log = LoggerFactory.getLogger(LogAppenderDemo.class);

    public static void main(String[] args) throws InterruptedException {

        var counter = 0;
        while (!Thread.currentThread().isInterrupted()) {
            log.info("test msg Ok");
            log.info("test msg Ok-2");
            log.info("test msg Ok-3:{}", counter++);

            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        }

        System.out.println("events");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
    }
}
