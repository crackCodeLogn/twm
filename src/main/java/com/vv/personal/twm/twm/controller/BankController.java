package com.vv.personal.twm.twm.controller;

import com.vv.personal.twm.artifactory.FixedDepositKeyUtil;
import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.twm.constants.BankFields;
import com.vv.personal.twm.twm.constants.FdFields;
import com.vv.personal.twm.twm.feign.BankServiceFeign;
import com.vv.personal.twm.twm.feign.RenderServiceFeign;
import com.vv.personal.twm.twm.util.DateUtil;
import com.vv.personal.twm.twm.util.TimeUtil;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.LinkedList;

import static com.vv.personal.twm.twm.constants.Constants.EMPTY_STR;
import static com.vv.personal.twm.twm.constants.Constants.FAILED;
import static com.vv.personal.twm.twm.util.GenericUtil.generateInsertionTime;

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

    @GetMapping("/banks/addBank")
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
        BankProto.BankList banksQueryResponse;
        try {
            banksQueryResponse = bankField != BankFields.ALL
                    ? bankServiceFeign.getBanks(bankField.name(), searchValue)
                    : getAllBanks();
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

    @GetMapping("/fd/addFd")
    @ApiOperation(value = "add new FD entry")
    private String addFixedDeposit(@RequestParam(defaultValue = "V2") String user,
                                   @RequestParam(defaultValue = "ABCD0123456") String bankIfsc,
                                   @RequestParam(defaultValue = "0.0") double depositAmount,
                                   @RequestParam(defaultValue = "0.0") double rateOfInterest,
                                   @RequestParam(defaultValue = "20201128") String startDate,
                                   @RequestParam(defaultValue = "25") int months,
                                   @RequestParam(required = false, defaultValue = "1") int days,
                                   FixedDepositProto.InterestType interestType,
                                   @RequestParam String nominee,
                                   @RequestParam(defaultValue = "20201128-17:22") String insertionTime) {
        final InspectResult inputVerificationResult = inspectInput(bankIfsc, depositAmount, rateOfInterest, startDate, months, days, insertionTime);
        if (!inputVerificationResult.isPass()) {
            LOGGER.warn("New FD input failed due to incorrect entries => {}. Retry with correct details", inputVerificationResult.getPassResult());
            return inputVerificationResult.getPassResult();
        }

        long insertionTimeMillis = generateInsertionTime(insertionTime);
        FixedDepositProto.FixedDeposit.Builder fixedDepositBuilder = FixedDepositProto.FixedDeposit.newBuilder()
                .setUser(user)
                .setBankIFSC(bankIfsc)
                .setDepositAmount(depositAmount)
                .setRateOfInterest(rateOfInterest)
                .setStartDate(startDate)
                .setMonths(months)
                .setDays(days)
                .setInterestType(interestType)
                .setNominee(nominee)
                .setInsertionTime(insertionTimeMillis);
        String fdKey = FixedDepositKeyUtil.generateFdKey(fixedDepositBuilder);
        FixedDepositProto.FixedDeposit fixedDeposit = fixedDepositBuilder.setKey(fdKey).build();

        LOGGER.info("Took input a new FD => '{}'", fdKey);
        try {
            String bankCallResult = bankServiceFeign.addFd(fixedDeposit);
            LOGGER.info("Result from bank service call => {}", bankCallResult);
            return bankCallResult;
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's addFd end-point. ", e);
        }
        return FAILED;
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
        FixedDepositProto.FixedDepositList fdQueryResponse;
        try {
            fdQueryResponse = fdFields != FdFields.ALL
                    ? bankServiceFeign.getFds(fdFields.name(), searchValue)
                    : getAllFixedDeposits();
            LOGGER.info("FD query resp: {}", fdQueryResponse);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's getFds end-point. ", e);
            return FAILED;
        }

        try {
            String rendOutput = renderServiceFeign.rendFds(fdQueryResponse);
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call / process rendering service's rendBanks end-point. ", e);
        }
        return FAILED;
    }

    public BankProto.BankList getAllBanks() {
        return bankServiceFeign.getBanks(BankFields.ALL.name(), EMPTY_STR);
    }

    public FixedDepositProto.FixedDepositList getAllFixedDeposits() {
        return bankServiceFeign.getFds(FdFields.ALL.name(), EMPTY_STR);
    }

    public InspectResult inspectInput(String bankIfsc, double depositAmount, double rateOfInterest, String startDate, int months, int days, String insertionTime) {
        final InspectResult result = new InspectResult().setPass(false);
        if (!bankIfsc.matches("[A-Z]{4}[0-9]{7}")) return result.setPassResult("IFSC in incorrect format. Correct => [A-Z]{4}[0-9]{7}");
        if (depositAmount <= 0.0) return result.setPassResult("Deposit amt has to be positive");
        if (rateOfInterest <= 0.0) return result.setPassResult("Rate of interest has to be positive");
        if (DateUtil.transmuteToLocalDate(startDate) == null) return result.setPassResult("Start date in incorrect format. Correct => YYYYMMDD");
        if (months <= 0) return result.setPassResult("Deposit months has to be positive");
        if (days <= 0) return result.setPassResult("Deposit days has to be positive");
        String[] insertionTimeSplit = insertionTime.split("-");
        if (DateUtil.transmuteToLocalDate(insertionTimeSplit[0]) == null || TimeUtil.transmuteToLocalTime(insertionTimeSplit[1]) == null)
            return result.setPassResult("Insertion time in incorrect format. Correct => YYYYMMDD-HH:mm");
        return result.setPass(true);
    }

    private static class InspectResult {
        private boolean pass = true;
        private String passResult = "OK";

        public InspectResult() {
        }

        public InspectResult(boolean pass, String passResult) {
            this.pass = pass;
            this.passResult = passResult;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("InspectResult{");
            sb.append("pass=").append(pass);
            sb.append(", passResult='").append(passResult).append('\'');
            sb.append('}');
            return sb.toString();
        }

        public boolean isPass() {
            return pass;
        }

        public InspectResult setPass(boolean pass) {
            this.pass = pass;
            return this;
        }

        public String getPassResult() {
            return passResult;
        }

        public InspectResult setPassResult(String passResult) {
            this.passResult = passResult;
            return this;
        }
    }
}
