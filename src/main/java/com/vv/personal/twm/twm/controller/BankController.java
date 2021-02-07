package com.vv.personal.twm.twm.controller;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.ping.processor.Pinger;
import com.vv.personal.twm.twm.constants.BankFields;
import com.vv.personal.twm.twm.feign.BankServiceFeign;
import com.vv.personal.twm.twm.feign.RenderServiceFeign;
import com.vv.personal.twm.twm.util.DateUtil;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    private Pinger pinger;

    @GetMapping("/banks/addBank")
    @ApiOperation(value = "add new bank entry")
    private String addBank(String bank, String bankIfsc, String contactNumber, BankProto.BankType bankType) {
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
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

    @PostMapping("/banks/deleteBank")
    @ApiOperation(value = "delete bank on IFSC code")
    public String deleteBank(String ifscToDelete) {
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
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
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
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
                                   @RequestParam(defaultValue = "V2") String originalUser,
                                   @RequestParam(defaultValue = "12345678901") String fdNumber, //the Key/FD
                                   @RequestParam(defaultValue = "12345678901") String customerId,
                                   @RequestParam(defaultValue = "ABCD0123456") String bankIfsc,
                                   @RequestParam(defaultValue = "0.0") double depositAmount,
                                   @RequestParam(defaultValue = "0.0") double rateOfInterest,
                                   @RequestParam(defaultValue = "20201128") String startDate,
                                   @RequestParam(defaultValue = "25") int months,
                                   @RequestParam(required = false, defaultValue = "0") int days,
                                   FixedDepositProto.InterestType interestType,
                                   @RequestParam String nominee) {
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        final InspectResult inputVerificationResult = inspectInput(fdNumber, customerId, bankIfsc, depositAmount, rateOfInterest, startDate, months, days);
        if (!inputVerificationResult.isPass()) {
            LOGGER.warn("New FD input failed due to incorrect entries => {}. Retry with correct details", inputVerificationResult.getPassResult());
            return inputVerificationResult.getPassResult();
        }

        FixedDepositProto.FixedDeposit.Builder fixedDepositBuilder = FixedDepositProto.FixedDeposit.newBuilder()
                .setUser(user)
                .setOriginalUser(originalUser)
                .setFdNumber(fdNumber)
                .setCustomerId(customerId)
                .setBankIFSC(bankIfsc)
                .setDepositAmount(depositAmount)
                .setRateOfInterest(rateOfInterest)
                .setStartDate(startDate)
                .setMonths(months)
                .setDays(days)
                .setInterestType(interestType)
                .setNominee(nominee);
        //String fdKey = FixedDepositKeyUtil.generateFdKey(fixedDepositBuilder);
        FixedDepositProto.FixedDeposit fixedDeposit = fixedDepositBuilder
                //.setKey(fdKey)
                .build();

        LOGGER.info("Took input a new FD => '{}'", fixedDeposit.getFdNumber());
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
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return bankServiceFeign.deleteFd(fdKey);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's deleteFd end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/manual/fd/compute-computables")
    @ApiOperation(value = "compute FD details on all FD and update DB")
    public void computeComputables() {
        LOGGER.info("Initiating computing of computables in all FD");
        FixedDepositProto.FixedDepositList fixedDepositList = getFixedDeposits(FixedDepositProto.FilterBy.ALL, EMPTY_STR);
        fixedDepositList.getFixedDepositList().stream()
                .filter(fixedDeposit -> !fixedDeposit.getFdNumber().isEmpty())
                .forEach(fixedDeposit -> bankServiceFeign.updateFd(fixedDeposit.getFdNumber()));
        LOGGER.info("Completed computing of computables in all FD");
    }


    @GetMapping("/fd/getFixedDeposits")
    @ApiOperation(value = "get FD(s) on fields", hidden = true)
    public FixedDepositProto.FixedDepositList getFixedDeposits(FixedDepositProto.FilterBy fdField,
                                                               String searchValue) {
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return FixedDepositProto.FixedDepositList.newBuilder().build();
        }
        FixedDepositProto.FixedDepositList fdQueryResponse;
        try {
            fdQueryResponse = fdField != FixedDepositProto.FilterBy.ALL
                    ? bankServiceFeign.getFds(fdField.name(), searchValue)
                    : getAllFixedDeposits();
            LOGGER.info("FD query resp: {}", fdQueryResponse);
            return fdQueryResponse;
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's getFds end-point. ", e);
        }
        return FixedDepositProto.FixedDepositList.newBuilder().build();
    }

    @GetMapping("/manual/fd/getFixedDeposits")
    @ApiOperation(value = "get FD(s) on fields")
    public String getFixedDepositsManually(FixedDepositProto.FilterBy fdField,
                                           String searchValue,
                                           @RequestParam(defaultValue = "startDate") FixedDepositProto.OrderFDsBy orderBy) {
        if (!pinger.allEndPointsActive(renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        FixedDepositProto.FixedDepositList fdQueryResponse = getFixedDeposits(fdField, searchValue);
        List<FixedDepositProto.FixedDeposit> fixedDepositList = new ArrayList<>(fdQueryResponse.getFixedDepositList());
        Optional<FixedDepositProto.FixedDeposit> aggFd = fixedDepositList.stream().filter(fixedDeposit -> fixedDeposit.getFdNumber().isEmpty()).findFirst();
        aggFd.ifPresent(fixedDepositList::remove);

        Comparator<FixedDepositProto.FixedDeposit> orderingComparator = null;
        switch (orderBy) {
            case START_DATE:
                orderingComparator = Comparator.comparing(FixedDepositProto.FixedDeposit::getStartDate);
                break;
            case END_DATE:
                orderingComparator = Comparator.comparing(FixedDepositProto.FixedDeposit::getEndDate);
                break;
            case DEPOSIT_AMOUNT:
                orderingComparator = Comparator.comparing(FixedDepositProto.FixedDeposit::getDepositAmount);
                break;
            case RATE_OF_INTEREST:
                orderingComparator = Comparator.comparing(FixedDepositProto.FixedDeposit::getRateOfInterest);
                break;
            case MONTHS:
                orderingComparator = Comparator.comparing(FixedDepositProto.FixedDeposit::getMonths);
                break;
            case UNRECOGNIZED:
                break;
        }
        fixedDepositList.sort(orderingComparator);
        fdQueryResponse = FixedDepositProto.FixedDepositList.newBuilder()
                .addAllFixedDeposit(fixedDepositList)
                .addFixedDeposit(aggFd.orElseGet(() -> FixedDepositProto.FixedDeposit.newBuilder().build()))
                .build();
        try {
            String rendOutput = renderServiceFeign.rendFds(fdQueryResponse);
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call / process rendering service's rendBanks end-point. ", e);
        }
        return FAILED;
    }

    @GetMapping("/fd/getFixedDepositsAnnualBreakdown")
    @ApiOperation(value = "get FD(s) on fields")
    public String getFixedDepositsAnnualBreakdown(FixedDepositProto.FilterBy fdField,
                                                  String searchValue) {
        if (!pinger.allEndPointsActive(bankServiceFeign, renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        FixedDepositProto.FixedDepositList fdQueryResponse = bankServiceFeign.generateAnnualBreakdownForExistingFds(fdField.name(), searchValue);

        return renderServiceFeign.rendFdsWithAnnualBreakdown(fdQueryResponse);
    }

    public BankProto.BankList getAllBanks() {
        return bankServiceFeign.getBanks(BankFields.ALL.name(), EMPTY_STR);
    }

    public FixedDepositProto.FixedDepositList getAllFixedDeposits() {
        return bankServiceFeign.getFds(FixedDepositProto.FilterBy.ALL.name(), EMPTY_STR);
    }

    public InspectResult inspectInput(String fdNumber, String customerId, String bankIfsc, double depositAmount, double rateOfInterest, String startDate, int months, int days) {
        final InspectResult result = new InspectResult().setPass(false);
        if (!fdNumber.matches("[0-9]+")) return result.setPassResult("FD-number in incorrect format. Correct => [0-9]+");
        if (!customerId.matches("[0-9]+")) return result.setPassResult("CustomerId in incorrect format. Correct => [0-9]+");
        if (!bankIfsc.matches("[A-Z]{4}[0-9]{7}")) return result.setPassResult("IFSC in incorrect format. Correct => [A-Z]{4}[0-9]{7}");
        if (depositAmount <= 0.0) return result.setPassResult("Deposit amt has to be positive");
        if (rateOfInterest <= 0.0) return result.setPassResult("Rate of interest has to be positive");
        if (DateUtil.transmuteToLocalDate(startDate) == null) return result.setPassResult("Start date in incorrect format. Correct => YYYYMMDD");
        //if (DateUtil.transmuteToLocalDate(endDate) == null) return result.setPassResult("End date in incorrect format. Correct => YYYYMMDD");
        if (months <= 0) return result.setPassResult("Deposit months has to be positive");
        if (days < 0) return result.setPassResult("Deposit days has to be positive");
        /*String[] insertionTimeSplit = insertionTime.split("-");
        if (DateUtil.transmuteToLocalDate(insertionTimeSplit[0]) == null || TimeUtil.transmuteToLocalTime(insertionTimeSplit[1]) == null)
            return result.setPassResult("Insertion time in incorrect format. Correct => YYYYMMDD-HH:mm");*/
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
