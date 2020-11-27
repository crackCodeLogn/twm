package com.vv.personal.twm.twm.controller;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.twm.constants.BankFields;
import com.vv.personal.twm.twm.constants.FdFields;
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

import java.util.Collection;
import java.util.LinkedList;

import static com.vv.personal.twm.twm.constants.Constants.EMPTY_STR;
import static com.vv.personal.twm.twm.constants.Constants.FAILED;

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

    private final Collection<BankProto.Bank> bankCollection = new LinkedList<>();

    /*@PostConstruct
    public void postHaste() {
        String banksQueryResponse = getAllBanks();
        Type collectionType = new TypeToken<Collection<BankProto.Bank>>() {
        }.getType();
        bankCollection = GSON.fromJson(banksQueryResponse, collectionType);
    }*/

    @PostMapping("/banks/addBank")
    @ApiOperation(value = "add new bank entry")
    private String addBank(String bank, String bankIfsc, String contactNumber, BankProto.BankType bankType) {
        BankProto.Bank newBank = BankProto.Bank.newBuilder()
                .setName(bank)
                .setIFSC(bankIfsc)
                .setContactNumber(contactNumber)
                .setBankType(bankType)
                .build();
        LOGGER.info("Took input a new bank => '{}'", newBank);
        try {
            return bankServiceFeign.addBank(newBank);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's addBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @PostMapping("/banks/dummyAddBank")
    @ApiOperation(value = "add dummy new bank entry")
    public String dummyTester() {
        return addBank("JPY", "JPY020323", "123345676", BankProto.BankType.PRIVATE);
    }

    @PostMapping("/banks/deleteBank")
    @ApiOperation(value = "delete bank on IFSC code")
    public String deleteBank(String ifscToDelete) {
        try {
            return bankServiceFeign.deleteBank(ifscToDelete);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's deleteBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/banks/getBanks")
    @ApiOperation(value = "get bank(s) on fields")
    public String getBanks(BankFields bankField,
                           String searchValue) {
        String banksQueryResponse;
        try {
            banksQueryResponse = bankField != BankFields.ALL ? bankServiceFeign.getBanks(bankField.name(), searchValue) : getAllBanks();
            LOGGER.info("Banks query resp: {}", banksQueryResponse);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's getBanks end-point. ", e);
            return FAILED;
        }

        try {
            String rendOutput = renderServiceFeign.rendBanks(banksQueryResponse);
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call / process rendering service's rendBanks end-point. ", e);
        }
        return FAILED;
    }

    @PostMapping("/fd/addFd")
    @ApiOperation(value = "add new FD entry")
    private String addFixedDeposit(String bank, String bankIfsc, String contactNumber, BankProto.BankType bankType) {
        BankProto.Bank newBank = BankProto.Bank.newBuilder()
                .setName(bank)
                .setIFSC(bankIfsc)
                .setContactNumber(contactNumber)
                .setBankType(bankType)
                .build();
        LOGGER.info("Took input a new bank => '{}'", newBank);
        try {
            return bankServiceFeign.addBank(newBank);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's addFd end-point. ", e);
        }
        return "FAILED!!";
    }

    @PostMapping("/fd/dummyAddFd")
    @ApiOperation(value = "add dummy new FD entry")
    public String dummyFdTester() {
        return addFixedDeposit("JPY", "JPY020323", "123345676", BankProto.BankType.PRIVATE);
    }

    @PostMapping("/fd/deleteFixedDeposit")
    @ApiOperation(value = "delete FD on supplied key")
    public String deleteFixedDeposit(String fdKey) {
        try {
            return bankServiceFeign.deleteFd(fdKey);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's deleteFd end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/fd/getFixedDeposits")
    @ApiOperation(value = "get FD(s) on fields")
    public String getFixedDeposits(FdFields fdFields,
                                   String searchValue) {
        try {
            String fdQueryResponse = bankServiceFeign.getFds(fdFields.name(), searchValue);
            LOGGER.info("FD query resp: {}", fdQueryResponse);
            /*String rendOutput = renderServiceFeign.rendBanks(fdQueryResponse);
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;*/
            return fdQueryResponse;
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's getFds end-point. ", e);
        }
        return "FAILED!!";
    }


    public String getAllBanks() {
        return bankServiceFeign.getBanks(BankFields.ALL.name(), EMPTY_STR);
    }
}
