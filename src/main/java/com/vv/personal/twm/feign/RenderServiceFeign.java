package com.vv.personal.twm.feign;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.remote.feign.PingFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Vivek
 * @since 17/11/20
 */
@FeignClient("twm-rendering-service")
public interface RenderServiceFeign extends PingFeign {

    @PostMapping("/render/rendBanks")
    String rendBanks(@RequestBody BankProto.BankList bankList);

    @PostMapping("/render/rendFds")
    String rendFds(@RequestBody FixedDepositProto.FixedDepositList fixedDepositList);

    @PostMapping("/render/rendFdsWithAnnualBreakdown")
    String rendFdsWithAnnualBreakdown(@RequestBody FixedDepositProto.FixedDepositList fixedDepositList);

    @PostMapping("/render/rendBankAccounts")
    String rendBankAccounts(BankProto.BankAccounts bankAccounts);
}
