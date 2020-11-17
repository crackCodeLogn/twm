package com.vv.personal.twm.twm.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Vivek
 * @since 17/11/20
 */
@FeignClient("twm-rendering-service")
public interface RenderServiceFeign {

    @PostMapping("/render/rendBanks")
    String rendBanks(@RequestBody String banksJson);
}
