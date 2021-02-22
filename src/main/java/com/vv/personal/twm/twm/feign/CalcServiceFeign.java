package com.vv.personal.twm.twm.feign;

import com.vv.personal.twm.ping.feign.HealthFeign;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author Vivek
 * @since 22/02/21
 */
@FeignClient("twm-calc-service")
public interface CalcServiceFeign extends HealthFeign {

}
