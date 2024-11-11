package com.vv.personal.twm.feign;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.remote.feign.PingFeign;
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
public interface BankServiceFeign extends PingFeign {

    @PostMapping("/banking/banks/{db}/addBank")
    String addBank(@PathVariable("db") String db, @RequestBody BankProto.Bank newBank);

    @PostMapping("/banking/banks/{db}/deleteBank")
    String deleteBank(@PathVariable("db") String db, @RequestBody String ifscToDelete);

    @GetMapping("/banking/banks/{db}/getBanks?field={field}&value={value}")
    BankProto.BankList getBanks(@PathVariable("db") String db,
                                @PathVariable("field") String field,
                                @PathVariable("value") String value);

    @PostMapping("/banking/fd/{db}/addFd")
    String addFd(@PathVariable("db") String db, @RequestBody FixedDepositProto.FixedDeposit newFixedDeposit);

    @PostMapping("/banking/fd/{db}/deleteFd")
    String deleteFd(@PathVariable("db") String db, @RequestBody String fdKey);

    @GetMapping("/banking/fd/{db}/update?fdKey={fdKey}")
    String updateFd(@PathVariable("db") String db, @PathVariable("fdKey") String fdKey);

    @GetMapping("/banking/fd/{db}/update/active?fdKey={fdKey}&isActive={isActive}")
    String updateFdActiveStatus(@PathVariable("db") String db,
                                @PathVariable("fdKey") String fdKey,
                                @PathVariable("isActive") Boolean isActive);

    @GetMapping("/banking/fd/{db}/getFds?field={field}&value={value}&considerActiveFdOnly={considerActiveFdOnly}")
    FixedDepositProto.FixedDepositList getFds(@PathVariable("db") String db,
                                              @PathVariable("field") String field,
                                              @PathVariable("value") String value,
                                              @PathVariable("considerActiveFdOnly") boolean considerActiveFdOnly);

    @GetMapping("/banking/fd/{db}/annual-breakdown?field={field}&value={value}&excludeOnBankIfsc={excludeOnBankIfsc}&considerActiveFdOnly={considerActiveFdOnly}")
    FixedDepositProto.FixedDepositList generateAnnualBreakdownForExistingFds(@PathVariable("db") String db,
                                                                             @PathVariable("field") String field,
                                                                             @PathVariable("value") String value,
                                                                             @PathVariable("excludeOnBankIfsc") String excludeOnBankIfsc,
                                                                             @PathVariable("considerActiveFdOnly") boolean considerActiveFdOnly);

    @GetMapping("/banking/fd/{db}/freeze/totalAmount?fdKey={fdKey}&totalAmount={totalAmount}")
    String freezeTotalAmount(@PathVariable("db") String db,
                             @PathVariable("fdKey") String fdKey,
                             @PathVariable("totalAmount") Double totalAmount);

    @GetMapping("/banking/fd/{db}/expire/nr?fdKey={fdKey}")
    String expireNrFd(@PathVariable("db") String db,
                      @PathVariable("fdKey") String fdKey);
}
