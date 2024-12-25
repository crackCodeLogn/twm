package com.vv.personal.twm.feign;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.remote.feign.PingFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/banking/bank-account/{db}/bank-account")
    String addBankAccount(@PathVariable("db") String db, @RequestBody BankProto.BankAccount newBankAccount);

    @PostMapping("/banking/bank-account/{db}/bank-accounts")
    String addBankAccounts(@PathVariable("db") String db, @RequestBody BankProto.BankAccounts newBankAccounts);

    @GetMapping("/banking/bank-account/{db}/bank-accounts")
    BankProto.BankAccounts getBankAccounts(@PathVariable("db") String db,
                                           @RequestParam("field") String field,
                                           @RequestParam("value") String value);

    @GetMapping("/banking/bank-account/{db}/bank-account/{id}")
    BankProto.BankAccount getBankAccount(@PathVariable("db") String db,
                                         @PathVariable("id") String id);

    @GetMapping("/banking/bank-account/{db}/bank-account/{id}/balance")
    BankProto.BankAccount getBankAccountBalance(@PathVariable("db") String db,
                                                @PathVariable("id") String id);

    // Should be PATCH, but apparently its non-standard for feign, thus falling back to POST
    @PostMapping("/banking/bank-account/{db}/bank-account/{id}/balance")
    boolean updateBankAccountBalance(@PathVariable("db") String db,
                                     @PathVariable("id") String id,
                                     @RequestBody BankProto.BankAccount bankAccount);

    @DeleteMapping("/banking/bank-account/{db}/bank-account/{id}")
    String deleteBankAccount(@PathVariable("db") String db, @PathVariable("id") String id);
}
