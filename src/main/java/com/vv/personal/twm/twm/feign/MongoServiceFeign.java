package com.vv.personal.twm.twm.feign;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author Vivek
 * @since 16/11/20
 */
@FeignClient("twm-mongo-service")
public interface MongoServiceFeign {

}
