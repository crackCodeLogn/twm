package com.vv.personal.twm.twm.controller;

import com.vv.personal.twm.artifactory.bank.Bank;
import com.vv.personal.twm.artifactory.bank.BankType;
import com.vv.personal.twm.twm.feign.BankServiceFeign;
import com.vv.personal.twm.twm.feign.RenderServiceFeign;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vivek
 * @since 16/11/20
 */
@RestController("BankController")
@RequestMapping("/central/bank")
public class BankController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankController.class);

    @Autowired
    private BankServiceFeign bankServiceFeign;

    @Autowired
    private RenderServiceFeign renderServiceFeign;

    @PostMapping("/addBank")
    @ApiOperation(value = "add new bank entry")
    private String addBank(String bank, String bankIfsc, String contactNumber, BankType bankType) {
        Bank newBank = new Bank()
                .setName(bank)
                .setIFSC(bankIfsc)
                .setContactNumber(contactNumber)
                .setType(bankType);
        LOGGER.info("Took input a new bank => '{}'", newBank);
        try {
            return bankServiceFeign.addBank(newBank);
        } catch (Exception e) {
            LOGGER.error("Failed to call mongo service's addBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @PostMapping("/dummyAddBank")
    @ApiOperation(value = "add dummy new bank entry")
    public String dummyTester() {
        return addBank("JPY", "JPY020323", "123345676", BankType.PRIVATE);
    }

    @PostMapping("/deleteBank")
    @ApiOperation(value = "delete bank on IFSC code")
    public String deleteBank(String ifscToDelete) {
        try {
            return bankServiceFeign.deleteBank(ifscToDelete);
        } catch (Exception e) {
            LOGGER.error("Failed to call mongo service's deleteBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/getBanks")
    @ApiOperation(value = "get bank(s) on fields")
    public String getBanks(BankFields bankField,
                           String searchValue) {
        try {
            String banksQueryResponse = bankServiceFeign.getBanks(bankField.name(), searchValue);
            LOGGER.info("Banks query resp: {}", banksQueryResponse);
            String rendOutput = renderServiceFeign.rendBanks(banksQueryResponse);
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call mongo service's deleteBank end-point. ", e);
        }
        return "FAILED!!";
    }

    private enum BankFields {
        ALL, NAME, TYPE, IFSC
    }
}
