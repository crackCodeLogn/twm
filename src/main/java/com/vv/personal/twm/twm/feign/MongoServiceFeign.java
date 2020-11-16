package com.vv.personal.twm.twm.feign;

import com.vv.personal.twm.artifactory.bank.Bank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Vivek
 * @since 16/11/20
 */
@FeignClient("twm-mongo-service")
public interface MongoServiceFeign {

    @PostMapping("/mongo/bank/addBank")
    String addBank(@RequestBody Bank newBank);

    @PostMapping("/mongo/bank/deleteBank")
    String deleteBank(@RequestBody String ifscToDelete);

    @GetMapping("/mongo/bank/getBanks?field={field}&value={value}")
    String getBanks(@PathVariable("field") String field,
                    @PathVariable("value") String value);
}
