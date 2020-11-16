package com.vv.personal.twm.twm.controller;

import com.vv.personal.twm.artifactory.bank.Bank;
import com.vv.personal.twm.artifactory.bank.BankType;
import com.vv.personal.twm.twm.feign.MongoServiceFeign;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vivek
 * @since 16/11/20
 */
@RestController("CentralController")
@RequestMapping("/central")
public class CentralController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CentralController.class);

    @Autowired
    private MongoServiceFeign mongoServiceFeign;

    @PostMapping("/addBank")
    @ApiOperation(value = "add new bank entry")
    private String addBank(String bank, String bankIfsc, Long contactNumber, BankType bankType) {
        Bank newBank = new Bank()
                .setName(bank)
                .setIFSC(bankIfsc)
                .setContactNumber(contactNumber)
                .setType(bankType);
        LOGGER.info("Took input a new bank => '{}'", newBank);
        try {
            return mongoServiceFeign.addBank(newBank);
        } catch (Exception e) {
            LOGGER.error("Failed to call mongo service's addBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @PostMapping("/dummyAddBank")
    @ApiOperation(value = "add dummy new bank entry")
    public String dummyTester() {
        return addBank("JPY", "JPY020323", 123345676L, BankType.PRIVATE);
    }
}
