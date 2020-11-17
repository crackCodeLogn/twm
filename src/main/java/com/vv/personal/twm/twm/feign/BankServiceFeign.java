package com.vv.personal.twm.twm.feign;

import com.vv.personal.twm.artifactory.bank.Bank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Vivek
 * @since 17/11/20
 */
@FeignClient("twm-bank-service")
public interface BankServiceFeign {

    @PostMapping("/banking/banks/addBank")
    String addBank(@RequestBody Bank newBank);

    @PostMapping("/banking/banks/deleteBank")
    String deleteBank(@RequestBody String ifscToDelete);

    @GetMapping("/banking/banks/getBanks?field={field}&value={value}")
    String getBanks(@PathVariable("field") String field,
                    @PathVariable("value") String value);
}
