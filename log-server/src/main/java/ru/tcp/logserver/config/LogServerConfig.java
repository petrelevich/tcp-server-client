package ru.tcp.logserver.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tcp.Server;
import ru.tcp.logserver.parser.MsgParser;
import ru.tcp.logserver.parser.MsgParserSm;

@Configuration
public class LogServerConfig {

    @Bean
    Server server(@Value("${log-server.port}") int port) {
        var server = new Server(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        return server;
    }

    @Bean(name = "executorForProcessing", destroyMethod = "close")
    ExecutorService executorForProcessing() {
        var factory = Thread.ofPlatform().name("processor-", 0).factory();
        return Executors.newSingleThreadExecutor(factory);
    }

    @Bean
    MsgParser msgParser() {
        return new MsgParserSm();
    }
}
