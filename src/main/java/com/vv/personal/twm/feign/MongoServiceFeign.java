package com.vv.personal.twm.feign;


import com.vv.personal.twm.ping.remote.feign.PingFeign;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author Vivek
 * @since 16/11/20
 */
@FeignClient("twm-mongo-service")
public interface MongoServiceFeign extends PingFeign {

}
