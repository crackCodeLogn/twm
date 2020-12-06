package com.vv.personal.twm.twm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

/**
 * @author Vivek
 * @since 16/11/20
 */
@EnableFeignClients
@EnableEurekaClient
@SpringBootApplication
public class TwmServer {
    public static void main(String[] args) {
        SpringApplication.run(TwmServer.class, args);
    }

    @Bean
    ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }
}
