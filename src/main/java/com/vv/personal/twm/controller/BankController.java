package com.vv.personal.twm.controller;

import com.vv.personal.twm.artifactory.generated.bank.BankProto;
import com.vv.personal.twm.artifactory.generated.data.DataPacketProto;
import com.vv.personal.twm.artifactory.generated.deposit.FixedDepositProto;
import com.vv.personal.twm.constants.BankFields;
import com.vv.personal.twm.constants.Constants;
import com.vv.personal.twm.constants.DatabaseType;
import com.vv.personal.twm.feign.BankServiceFeign;
import com.vv.personal.twm.feign.RenderServiceFeign;
import com.vv.personal.twm.ping.processor.Pinger;
import com.vv.personal.twm.util.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.vv.personal.twm.constants.Constants.FAILED;

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
    @Operation(summary = "add new bank entry")
    private String addBank(String bank, String bankIfsc, String contactNumber, BankProto.BankType bankType,
                           Boolean isBankActive, String countryCode, DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Adding new bank: {} x {}", bank, bankIfsc);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        BankProto.Bank newBank = BankProto.Bank.newBuilder()
                .setName(bank)
                .setIFSC(bankIfsc)
                .setContactNumber(contactNumber)
                .setBankType(bankType)
                .setIsActive(isBankActive)
                .setCountryCode(countryCode)
                .build();
        LOGGER.info("Took input a new bank => '{}'", newBank);
        try {
            return bankServiceFeign.addBank(db, newBank);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's addBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @DeleteMapping("/banks/deleteBank")
    @Operation(summary = "delete bank on IFSC code")
    public String deleteBank(String ifscToDelete, DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Deleting bank IFSC: {}", ifscToDelete);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return bankServiceFeign.deleteBank(db, ifscToDelete);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's deleteBank end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/manual/banks/getBanks")
    @Operation(summary = "get bank(s) on fields")
    public String getBanks(BankFields bankField,
                           @RequestParam(required = false, defaultValue = "") String searchValue,
                           DatabaseType dbType) {
        Optional<BankProto.BankList> banksQueryResponse = getBanksOnConstraints(bankField, searchValue, dbType);
        if (banksQueryResponse.isEmpty()) return FAILED;

        if (!pinger.allEndPointsActive(renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return FAILED;
        }

        try {
            String rendOutput = renderServiceFeign.rendBanks(banksQueryResponse.get());
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call / process rendering service's rendBanks end-point. ", e);
        }
        return FAILED;
    }

    @GetMapping("/banks/getBanks")
    @Operation(summary = "get bank(s) on fields")
    public BankProto.BankList getBanks(
            @RequestParam("bankField") String bankFieldStr,
            @RequestParam("search") String searchValue,
            @RequestParam("dbType") String dbTypeStr) {
        BankProto.BankList bankList = BankProto.BankList.newBuilder().build();
        BankFields bankField;
        try {
            bankField = BankFields.valueOf(bankFieldStr);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid bank field: {}", bankFieldStr);
            return bankList;
        }
        DatabaseType dbType;
        try {
            dbType = DatabaseType.valueOf(dbTypeStr);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid database type: {}", dbTypeStr);
            return bankList;
        }
        return getBanksOnConstraints(bankField, searchValue, dbType).orElse(bankList);
    }

    private Optional<BankProto.BankList> getBanksOnConstraints(BankFields bankField, String searchValue,
                                                               DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Retrieving banks on {} x {}", bankField, searchValue);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return Optional.empty();
        }

        try {
            BankProto.BankList banksQueryResponse = bankField != BankFields.ALL
                    ? bankServiceFeign.getBanks(db, bankField.name(), searchValue)
                    : getAllBanks(db);
            LOGGER.info("Banks query resp: {}", banksQueryResponse);
            return Optional.of(banksQueryResponse);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's getBanks end-point. ", e);
        }
        return Optional.empty();
    }

    @GetMapping("/fd/addFd")
    @Operation(summary = "add new FD entry")
    private String addFixedDeposit(@RequestParam(defaultValue = "V2") String user,
                                   @RequestParam(defaultValue = "V2") String originalUser,
                                   @RequestParam(defaultValue = "12345678901") String fdNumber, //the Key/FD
                                   FixedDepositProto.AccountType accountType,
                                   @RequestParam(defaultValue = "12345678901") String customerId,
                                   @RequestParam(defaultValue = "ABCD0123456") String bankIfsc,
                                   @RequestParam(defaultValue = "0.0") double depositAmount,
                                   @RequestParam(defaultValue = "0.0") double rateOfInterest,
                                   @RequestParam(defaultValue = "20201128") String startDate,
                                   @RequestParam(defaultValue = "25") int months,
                                   @RequestParam(required = false, defaultValue = "0") int days,
                                   FixedDepositProto.InterestType interestType,
                                   @RequestParam String nominee,
                                   @RequestParam Boolean isFdActive,
                                   DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Adding new FD of {}", depositAmount);
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
                .setAccountType(accountType)
                .setCustomerId(customerId)
                .setBankIFSC(bankIfsc)
                .setDepositAmount(depositAmount)
                .setRateOfInterest(rateOfInterest)
                .setStartDate(startDate)
                .setMonths(months)
                .setDays(days)
                .setInterestType(interestType)
                .setIsFdActive(isFdActive)
                .setNominee(nominee);
        //String fdKey = FixedDepositKeyUtil.generateFdKey(fixedDepositBuilder);
        FixedDepositProto.FixedDeposit fixedDeposit = fixedDepositBuilder
                //.setKey(fdKey)
                .build();

        LOGGER.info("Took input a new FD => '{}'", fixedDeposit.getFdNumber());
        try {
            String bankCallResult = bankServiceFeign.addFd(db, fixedDeposit);
            LOGGER.info("Result from bank service call => {}", bankCallResult);
            return bankCallResult;
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's addFd end-point. ", e);
        }
        return FAILED;
    }

    @DeleteMapping("/fd/deleteFixedDeposit")
    @Operation(summary = "delete FD on supplied key")
    public String deleteFixedDeposit(String fdKey, DatabaseType db) {
        LOGGER.info("Deleting FD: {}", fdKey);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return bankServiceFeign.deleteFd(db.name(), fdKey);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's deleteFd end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/manual/fd/compute-computables")
    @Operation(summary = "compute FD details on all FD and update DB")
    public void computeComputables(DatabaseType dbType, boolean considerActiveFdOnly) {
        String db = dbType.name();
        LOGGER.info("Initiating computing of computables in all FD");
        FixedDepositProto.FixedDepositList fixedDepositList = getFixedDeposits(FixedDepositProto.FilterBy.ALL,
                Constants.EMPTY_STR, considerActiveFdOnly, dbType);
        fixedDepositList.getFixedDepositList().stream()
                .filter(fixedDeposit -> !fixedDeposit.getFdNumber().isEmpty())
                .forEach(fixedDeposit -> bankServiceFeign.updateFd(db, fixedDeposit.getFdNumber()));
        LOGGER.info("Completed computing of computables in all FD");
    }

    @GetMapping("/manual/fd/update-active-status")
    @Operation(summary = "update FD status")
    public String updateFdActiveStatus(DatabaseType db, String fdNumber, Boolean isActive) {
        LOGGER.info("Initiating update of FD {} to status {}", fdNumber, isActive);
        return bankServiceFeign.updateFdActiveStatus(db.name(), fdNumber, isActive);
    }

    @GetMapping("/manual/fd/freeze-total-amount")
    @Operation(summary = "freeze total amount")
    public String freezeTotalAmount(DatabaseType db, String fdNumber, Double totalAmount) {
        LOGGER.info("Initiating update of FD {} to freeze total amount to {}", fdNumber, totalAmount);
        return bankServiceFeign.freezeTotalAmount(db.name(), fdNumber, totalAmount);
    }

    @GetMapping("/manual/fd/expire/nr")
    @Operation(summary = "mark NR account as expired")
    public String expireNrFd(DatabaseType db, String fdNumber) {
        LOGGER.info("Initiating update of FD {} to expired", fdNumber);
        return bankServiceFeign.expireNrFd(db.name(), fdNumber);
    }

    @GetMapping("/fd/getFixedDeposits")
    @Operation(summary = "get FD(s) on fields", hidden = true)
    public FixedDepositProto.FixedDepositList getFixedDeposits(FixedDepositProto.FilterBy fdField,
                                                               @RequestParam(defaultValue = "") String searchValue,
                                                               boolean considerActiveFdOnly,
                                                               DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Retrieving all FDs on {} x {}", fdField, searchValue);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return FixedDepositProto.FixedDepositList.newBuilder().build();
        }
        FixedDepositProto.FixedDepositList fdQueryResponse;
        try {
            fdQueryResponse = fdField != FixedDepositProto.FilterBy.ALL
                    ? bankServiceFeign.getFds(db, fdField.name(), searchValue, considerActiveFdOnly)
                    : getAllFixedDeposits(db, considerActiveFdOnly);
            LOGGER.info("FD query resp: {}", fdQueryResponse);
            return fdQueryResponse;
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's getFds end-point. ", e);
        }
        return FixedDepositProto.FixedDepositList.newBuilder().build();
    }

    @GetMapping("/manual/fd/getFixedDeposits")
    @Operation(summary = "get FD(s) on fields")
    public String getFixedDepositsManually(FixedDepositProto.FilterBy fdField,
                                           String searchValue,
                                           @RequestParam(defaultValue = "startDate") FixedDepositProto.OrderFDsBy orderBy,
                                           boolean considerActiveFdOnly,
                                           DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Retrieving all FDs on {} x {}, sorted on {}", fdField, searchValue, orderBy);
        if (!pinger.allEndPointsActive(renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        FixedDepositProto.FixedDepositList fdQueryResponse = getFixedDeposits(fdField, searchValue,
                considerActiveFdOnly, dbType);
        List<FixedDepositProto.FixedDeposit> fixedDepositList = new ArrayList<>(fdQueryResponse.getFixedDepositList());

        //this aggFd is computed in the twm-bank-service
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
    @Operation(summary = "get FD(s) on fields")
    public String getFixedDepositsAnnualBreakdown(FixedDepositProto.FilterBy fdField,
                                                  String searchValue,
                                                  @RequestParam(defaultValue = "", required = false) String excludeOnBankIfsc,
                                                  boolean considerActiveFdOnly,
                                                  DatabaseType dbType) {
        String db = dbType.name();
        LOGGER.info("Retrieving FD annual breakdown calculation for {} x {}", fdField, searchValue);
        if (!pinger.allEndPointsActive(bankServiceFeign, renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        FixedDepositProto.FixedDepositList fdQueryResponse = bankServiceFeign.generateAnnualBreakdownForExistingFds(db, fdField.name(), searchValue, excludeOnBankIfsc, considerActiveFdOnly);
        return renderServiceFeign.rendFdsWithAnnualBreakdown(fdQueryResponse);
    }

    @PostMapping("/bank-account/bank-account")
    @Operation(summary = "add new bank account entry")
    public String addBankAccount(String bankIfsc, String accountName,
                                 String accountNumber,
                                 String transitNumber,
                                 String institutionNumber,
                                 Double balance,
                                 String pipeSeparatedBankAccountTypes,
                                 Double overdraftBalance,
                                 Double interestRate,
                                 Boolean isBankActive,
                                 BankProto.CurrencyCode currencyCode,
                                 @RequestParam(defaultValue = "") String note,
                                 DatabaseType db) {
        bankIfsc = bankIfsc.strip();
        accountName = accountName.strip();
        accountNumber = accountNumber.strip();
        transitNumber = transitNumber.strip();
        institutionNumber = institutionNumber.strip();
        note = note.strip();
        pipeSeparatedBankAccountTypes = pipeSeparatedBankAccountTypes.strip();
        List<BankProto.BankAccountType> bankAccountTypeList =
                Arrays.stream(pipeSeparatedBankAccountTypes.split("\\|")).map(type -> BankProto.BankAccountType.valueOf(type.strip())).toList();

        LOGGER.info("Adding new bank account: {} x {}", accountName, bankIfsc);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        BankProto.BankAccount newBankAccount = BankProto.BankAccount.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setBank(BankProto.Bank.newBuilder()
                        .setIFSC(bankIfsc))
                .setNumber(accountNumber)
                .setName(accountName)
                .setTransitNumber(transitNumber)
                .setInstitutionNumber(institutionNumber)
                .setBalance(balance)
                .addAllBankAccountTypes(bankAccountTypeList)
                .setOverdraftBalance(overdraftBalance)
                .setInterestRate(interestRate)
                .setIsActive(isBankActive)
                .setCcy(currencyCode)
                .setNote(note)
                .build();
        LOGGER.info("Took input a new bank account => '{}'", newBankAccount);
        try {
            bankServiceFeign.addBankAccount(db.name(), newBankAccount);
            return newBankAccount.getId();
        } catch (Exception e) {
            LOGGER.error("Failed to call bank service's addBankAccount end-point. ", e);
        }
        return "FAILED!!";
    }

    @GetMapping("/manual/bank-accounts/bank-accounts")
    @Operation(summary = "get bank account(s) on fields")
    public String getBankAccounts(BankFields bankField,
                                  @RequestParam(defaultValue = "") String searchValue,
                                  DatabaseType db) {
        if (bankField == BankFields.TYPE) {
            return "Not supported yet";
        }

        Optional<BankProto.BankAccounts> bankAccounts = sharedGetBankAccounts(bankField, searchValue, db);
        if (bankAccounts.isEmpty()) return FAILED;

        if (!pinger.allEndPointsActive(renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return FAILED;
        }
        try {
            String rendOutput = renderServiceFeign.rendBankAccounts(bankAccounts.orElse(null));
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call / process rendering service's rendBankAccounts end-point. ", e);
        }
        return FAILED;
    }

    @GetMapping("/bank-accounts/bank-accounts")
    @Operation(summary = "get bank account(s) on fields")
    public BankProto.BankAccounts getBankAccounts(@RequestParam("bankField") String bankFieldStr,
                                                  @RequestParam("search") String searchValue,
                                                  @RequestParam("dbType") String dbTypeStr) {
        BankProto.BankAccounts bankAccounts = BankProto.BankAccounts.newBuilder().build();
        BankFields bankField;
        try {
            bankField = BankFields.valueOf(bankFieldStr);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid bank accounts field: {}", bankFieldStr);
            return bankAccounts;
        }
        if (bankField == BankFields.TYPE) {
            LOGGER.warn("Bank account type {} - not supported yet", bankField);
            return bankAccounts;
        }
        DatabaseType dbType;
        try {
            dbType = DatabaseType.valueOf(dbTypeStr);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid database type: {}", dbTypeStr);
            return bankAccounts;
        }
        Optional<BankProto.BankAccounts> accounts =
                sharedGetBankAccounts(bankField, searchValue, dbType);
        return accounts.map(value -> BankProto.BankAccounts.newBuilder()
                .addAllAccounts(
                        value.getAccountsList().stream()
                                .map(bankAccount ->
                                        BankProto.BankAccount.newBuilder()
                                                .mergeFrom(bankAccount)
                                                .clearId()
                                                .build())
                                .collect(Collectors.toList())
                )
                .build()).orElse(bankAccounts);
    }

    private Optional<BankProto.BankAccounts> sharedGetBankAccounts(BankFields bankField, String searchValue,
                                                                   DatabaseType db) {

        LOGGER.info("Retrieving bank accounts on {} x {}", bankField, searchValue);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return Optional.empty();
        }

        try {
            BankProto.BankAccounts bankAccounts = bankServiceFeign.getBankAccounts(db.name(), bankField.name(),
                    searchValue);
            LOGGER.info("Bank accounts query resp: {}", bankAccounts);
            return Optional.of(bankAccounts);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank account service's getBankAccounts end-point. ", e);
        }
        return Optional.empty();
    }

    @GetMapping("/bank-accounts/bank-account")
    @Operation(summary = "get bank account")
    public String getBankAccount(String id, DatabaseType db) {
        id = id.strip();
        LOGGER.info("Retrieving bank account id: {}", id);
        if (!pinger.allEndPointsActive(bankServiceFeign, renderServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        BankProto.BankAccount bankAccount;
        try {
            bankAccount = bankServiceFeign.getBankAccount(db.name(), id);
            if (bankAccount == null) {
                return "May not exist: " + id;
            }
            LOGGER.info("Bank account query resp: {}", bankAccount);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank account service's getBankAccount end-point. ", e);
            return FAILED;
        }

        try {
            String rendOutput =
                    renderServiceFeign.rendBankAccounts(BankProto.BankAccounts.newBuilder().addAccounts(bankAccount).build());
            LOGGER.info("HTML table output:-\n{}", rendOutput);
            return rendOutput;
        } catch (Exception e) {
            LOGGER.error("Failed to call / process rendering service's rendBankAccounts end-point. ", e);
        }
        return FAILED;
    }

    @GetMapping("/bank-accounts/bank-account/balance")
    @Operation(summary = "get bank account balance")
    public String getBankAccountBalance(String id, DatabaseType db) {
        id = id.strip();
        LOGGER.info("Retrieving bank account balance of id: {}", id);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        BankProto.BankAccount bankAccount;
        try {
            bankAccount = bankServiceFeign.getBankAccountBalance(db.name(), id);
            LOGGER.info("Bank account bal query resp: {}", bankAccount);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank account service's getBankAccount end-point. ", e);
            return FAILED;
        }

        if (bankAccount.getId().equals(id)) {
            return String.valueOf(bankAccount.getBalance());
        }
        return FAILED;
    }

    @PostMapping("/manual/bank-accounts/bank-account/balance")
    @Operation(summary = "update bank account balance")
    public String updateBankAccountBalance(
            @RequestParam String id,
            @RequestParam Double amount,
            @RequestParam DatabaseType dbType) {
        return sharedUpdateBankAccountBalance(id.strip(), amount, dbType);
    }

    @PostMapping("/bank-accounts/bank-account/balance")
    @Operation(summary = "update bank account balance")
    public String updateBankAccountBalance(
            @RequestBody DataPacketProto.DataPacket dataPacket) {
        Map<String, String> stringStringMap = dataPacket.getStringStringMapMap();
        Map<String, Double> stringDoubleMap = dataPacket.getStringDoubleMapMap();

        if (!stringStringMap.containsKey("id") || !stringStringMap.containsKey("db") || !stringDoubleMap.containsKey(
                "amount")) {
            LOGGER.error("Failed to process request because of missing id / db / amount => {}", dataPacket);
            return FAILED;
        }

        String db = stringStringMap.get("db").strip();
        DatabaseType type;
        try {
            type = DatabaseType.valueOf(db);
        } catch (Exception e) {
            LOGGER.error("Unrecognized db type: {}", db, e);
            return FAILED;
        }

        String id = stringStringMap.get("id").strip();
        Double newBalance = stringDoubleMap.get("amount");
        return sharedUpdateBankAccountBalance(id, newBalance, type);
    }

    private String sharedUpdateBankAccountBalance(String id, Double amount, DatabaseType dbType) {
        LOGGER.info("Updating bank account balance of id: {}", id);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }

        try {
            boolean result = bankServiceFeign.updateBankAccountBalance(dbType.name(), id,
                    BankProto.BankAccount.newBuilder()
                            .setBalance(amount)
                            .setId(id)
                            .build());
            LOGGER.info("Bank account update bal resp: {}", result);
            if (result) return "Done";
        } catch (Exception e) {
            LOGGER.error("Failed to call bank account service's getBankAccount end-point. ", e);
        }
        return FAILED;
    }

    @DeleteMapping("/bank-account/bank-account/")
    @Operation(summary = "delete bank account on id")
    public String deleteBankAccount(String id, DatabaseType db) {
        id = id.strip();
        LOGGER.info("Deleting bank account: {}", id);
        if (!pinger.allEndPointsActive(bankServiceFeign)) {
            LOGGER.error("All end-points not active. Will not trigger op! Check log");
            return "END-POINTS NOT READY!";
        }
        try {
            return bankServiceFeign.deleteBankAccount(db.name(), id);
        } catch (Exception e) {
            LOGGER.error("Failed to call bank account service's deleteBank end-point. ", e);
        }
        return "FAILED!!";
    }

    private BankProto.BankList getAllBanks(String db) {
        return bankServiceFeign.getBanks(db, BankFields.ALL.name(), Constants.EMPTY_STR);
    }

    private FixedDepositProto.FixedDepositList getAllFixedDeposits(String db, boolean considerActiveFdOnly) {
        return bankServiceFeign.getFds(db, FixedDepositProto.FilterBy.ALL.name(), Constants.EMPTY_STR, considerActiveFdOnly);
    }

    private InspectResult inspectInput(String fdNumber, String customerId, String bankIfsc, double depositAmount,
                                       double rateOfInterest, String startDate, int months, int days) {
        final InspectResult result = new InspectResult().setPass(false);
        if (!fdNumber.matches("[0-9-]+"))
            return result.setPassResult("FD-number in incorrect format. Correct => [0-9-]+, your fd number is: " + fdNumber);
        if (!customerId.matches("[0-9]+"))
            return result.setPassResult("CustomerId in incorrect format. Correct => [0-9]+");
        // stopping ifsc checks - 20250120
//        if (!bankIfsc.matches("[A-Z]{4}[0-9]{7}") && !bankIfsc.matches("PO-[0-9]{6}") && !bankIfsc.matches("[0-9]{5}"))
//            return result.setPassResult("IFSC in incorrect format. Correct => [A-Z]{4}[0-9]{7} || PO-[0-9]{6} || [0-9]{5}");
        if (depositAmount <= 0.0) return result.setPassResult("Deposit amt has to be positive");
        if (rateOfInterest <= 0.0) return result.setPassResult("Rate of interest has to be positive");
        if (DateUtil.transmuteToLocalDate(startDate) == null)
            return result.setPassResult("Start date in incorrect format. Correct => YYYYMMDD");
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
            String sb = "InspectResult{" + "pass=" + pass +
                    ", passResult='" + passResult + '\'' +
                    '}';
            return sb;
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
