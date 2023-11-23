package com.vv.personal.twm.twm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.util.TimeZone;

import static com.vv.personal.twm.twm.constants.Constants.*;

/**
 * @author Vivek
 * @since 16/11/20
 */
@EnableFeignClients
@EnableDiscoveryClient
@ComponentScan({"com.vv.personal.twm.twm", "com.vv.personal.twm.ping"})
@SpringBootApplication
public class TwmServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwmServer.class);

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("EST", ZoneId.SHORT_IDS))); //force setting
        SpringApplication.run(TwmServer.class, args);
    }

    @Autowired
    private Environment environment;

    @Bean
    ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void firedUpAllCylinders() {
        String host = LOCALHOST;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.error("Failed to obtain ip address. ", e);
        }
        String port = environment.getProperty(LOCAL_SPRING_PORT);
        LOGGER.info("'{}' activation is complete! Exact url: {}", environment.getProperty("spring.application.name").toUpperCase(),
                String.format(SWAGGER_UI_URL, host, port));
    }
}
