package ru.yandex.payments.fnsreg;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpStatus;
import io.micronaut.runtime.context.scope.Refreshable;
import lombok.Value;
import lombok.With;
import lombok.val;

import ru.yandex.payments.fnsreg.controller.dto.CreateRegReportRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateRegistrationAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateReregistrationAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateWithdrawAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.RegReportState;
import ru.yandex.payments.fnsreg.controller.dto.RegState;
import ru.yandex.payments.fnsreg.controller.dto.WithdrawState;
import ru.yandex.payments.fnsreg.dto.ApplicationVersion;
import ru.yandex.payments.fnsreg.dto.Kkt;
import ru.yandex.payments.fnsreg.dto.Report;
import ru.yandex.payments.fnsreg.dto.ReregistrationInfo;
import ru.yandex.payments.fnsreg.dto.WithdrawReason;
import ru.yandex.payments.fnsreg.manager.ApplicationManager;
import ru.yandex.payments.fnsreg.manager.RegistrationManager;
import ru.yandex.payments.fnsreg.types.Application;
import ru.yandex.payments.fnsreg.types.RegRequestId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@With
@Value
class KktState {
    @NonNull
    Kkt kkt;
    @Nullable
    Application application;
    @Nullable
    RegRequestId regRequestId;
    @Nullable
    RegState regState;
    @Nullable
    RegReportState regReportState;
    @Nullable
    WithdrawState withdrawState;
}

@Refreshable
class StateManager {
    private final Map<String, KktState> kktState = new HashMap<>();

    @PostConstruct
    public void init() {
        kktState.clear();
    }

    Optional<KktState> findState(String kktName) {
        return Optional.ofNullable(kktState.get(kktName));
    }

    void updateState(String kktName, KktState newState) {
        kktState.put(kktName, newState);
    }
}

public class StepDefinitions {
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(10);

    @Inject
    FnsRegClient client;

    @Inject
    StateManager stateManager;

    @Inject
    ApplicationManager applicationManager;

    @Inject
    RegistrationManager registrationManager;

    private Optional<KktState> findState(String kktName) {
        return stateManager.findState(kktName);
    }

    private KktState getState(String kktName) {
        return findState(kktName)
                .orElseThrow(() -> new IllegalStateException("KKT '" + kktName + "' not found"));
    }

    private void updateState(String kktName, KktState newState) {
        stateManager.updateState(kktName, newState);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @ParameterType(value = "'([^']+)'", name = "withdraw_reason")
    public WithdrawReason withdrawReasonType(String name) {
        return WithdrawReason.valueOf(name);
    }

    @Given("New KKT {string}")
    public void newKKT(String kktName) {
        val kkt = TestData.obtainKkt();
        updateState(kktName, new KktState(kkt, null, null, null, null, null));
    }

    @When("We generate registration application for KKT {string}")
    public void weGenerateRegistrationApplicationForKKT(String kktName) {
        weGenerateRegistrationApplicationForKKTImpl(kktName, Optional.of(false));
    }

    @When("We generate registration application for marked goods KKT {string}")
    public void weGenerateRegistrationApplicationForMarkedGoodsKKT(String kktName) {
        weGenerateRegistrationApplicationForKKTImpl(kktName, Optional.of(true));
    }

    private void weGenerateRegistrationApplicationForKKTImpl(String kktName, Optional<Boolean> markedGoodsUsage) {
        val state = getState(kktName);
        val request = new CreateRegistrationAppRequest(state.getKkt(), TestData.FIRM, TestData.OFD,
                Optional.of(ApplicationVersion.V505), markedGoodsUsage);
        val application = client.createApplication(request);
        updateState(kktName, state.withApplication(application));
    }

    @When("We send obtained registration application for KKT {string} to fns")
    public void weSendObtainedRegistrationApplicationForKKTToFns(String kktName) {
        val state = getState(kktName);
        assertThat(state.getApplication())
                .describedAs("Application for KKT '%s' not found", kktName)
                .isNotNull();

        val requestId = RegRequestId.random();
        val response = client.createRegistration(requestId, state.getApplication().getValue());
        assertThat((Object) response.status())
                .isEqualTo(HttpStatus.OK);

        updateState(kktName, state.withRegRequestId(requestId));
    }

    @When("We poll KKT {string} registration state within {int} minute(s)")
    public void wePollKKTRegistrationStateWithinMinute(String kktName, int waitForMinutes) {
        val state = getState(kktName);
        assertThat(state.getRegRequestId())
                .describedAs("Registration request for KKT '%s' not found", kktName)
                .isNotNull();

        val resultState = await()
                .ignoreNoExceptions()
                .timeout(Duration.ofMinutes(waitForMinutes))
                .pollInterval(POLL_INTERVAL)
                .until(
                        () -> client.getRegistrationState(state.getRegRequestId()),
                        regState -> !(regState instanceof RegState.InProcess)
                );

        updateState(kktName, state.withRegState(resultState));
    }

    @When("We generate registration report application for KKT {string}")
    public void weGenerateRegistrationReportApplicationForKKT(String kktName) {
        val state = getState(kktName);
        assertThat(state.getRegState())
                .describedAs("KKT %s needs to be registered before generating registration report", kktName)
                .isNotNull()
                .isInstanceOf(RegState.Success.class);
        val regNumber = ((RegState.Success) state.getRegState()).number();
        val kkt = state.getKkt();
        val fakeReport = new Report(LocalDateTime.now(), 1, "1");
        val request = new CreateRegReportRequest(regNumber, kkt.fn().serialNumber(), TestData.FIRM, TestData.OFD, fakeReport);
        val application = client.createApplication(request);
        updateState(kktName, state.withApplication(application));
    }

    @When("We send obtained registration report application for KKT {string} to fns")
    public void weSendObtainedRegistrationReportApplicationForKKTToFns(String kktName) {
        val state = getState(kktName);
        assertThat(state.getApplication())
                .describedAs("Application for KKT '%s' not found", kktName)
                .isNotNull();

        val requestId = RegRequestId.random();
        val kkt = state.getKkt();

        val response = client.createReport(requestId, kkt.serialNumber(), kkt.modelName(), state.getApplication().getValue());
        assertThat((Object) response.status())
                .isEqualTo(HttpStatus.OK);

        updateState(kktName, state.withRegRequestId(requestId));
    }

    @When("We poll KKT {string} registration report state within {int} minute(s)")
    public void wePollKKTRegistrationReportStateWithinMinute(String kktName, int waitForMinutes) {
        val state = getState(kktName);
        assertThat(state.getRegRequestId())
                .describedAs("Registration request for KKT '%s' not found", kktName)
                .isNotNull();

        val resultState = await()
                .ignoreNoExceptions()
                .timeout(Duration.ofMinutes(waitForMinutes))
                .pollInterval(POLL_INTERVAL)
                .until(
                        () -> client.getRegReportState(state.getRegRequestId()),
                        regState -> !(regState instanceof RegReportState.InProcess)
                );

        updateState(kktName, state.withRegReportState(resultState));
    }

    @When("We generate reregistration application for marked goods KKT {string}")
    public void weGenerateReregistrationApplicationForMarkedGoodsKKT(String kktName) {
        weGenerateReregistrationApplicationForKKTImpl(kktName, Optional.of(true));
    }

    @When("We generate reregistration application for marked goods without replacing fn KKT {string}")
    public void weGenerateReregistrationApplicationForMarkedGoodsWithoutFnKKT(String kktName) {
        weGenerateReregistrationApplicationForKKTWithoutFNChangeImpl(kktName, Optional.of(true));
    }

    @When("We generate reregistration application for non-marked goods KKT {string}")
    public void weGenerateReregistrationApplicationForNonMarkedGoodsKKT(String kktName) {
        weGenerateReregistrationApplicationForKKTImpl(kktName, Optional.of(false));
    }

    @When("We generate reregistration application for KKT {string}")
    public void weGenerateReregistrationApplicationForKKT(String kktName) {
        weGenerateReregistrationApplicationForKKTImpl(kktName, Optional.empty());
    }

    public void weGenerateReregistrationApplicationForKKTImpl(String kktName, Optional<Boolean> markedGoodsUsage) {
        val state = getState(kktName);
        assertThat(state.getRegState())
                .describedAs("KKT %s needs to be registered before reregistration", kktName)
                .isNotNull()
                .isInstanceOf(RegState.Success.class);

        val prevRegNumber = ((RegState.Success) state.getRegState()).number();
        val fakeReport = new Report(LocalDateTime.now(), 1, "1");
        val changes = EnumSet.of(ReregistrationInfo.Change.FN);
        val reregInfo = new ReregistrationInfo(changes, prevRegNumber,
                Optional.of(fakeReport), Optional.of(fakeReport));

        val prevKkt = state.getKkt();
        val newFn = TestData.obtainUnusedFn();
        val updatedKkt = new Kkt(prevKkt.serialNumber(), prevKkt.modelName(), prevKkt.mode(), newFn, prevKkt.address(),
                prevKkt.automatedSystemNumber(), Optional.empty());

        val request = new CreateReregistrationAppRequest(updatedKkt, TestData.FIRM, TestData.OFD, reregInfo,
                Optional.of(ApplicationVersion.V505), markedGoodsUsage);
        val application = client.createApplication(request);

        updateState(kktName, state.withApplication(application).withKkt(updatedKkt));
    }

    public void weGenerateReregistrationApplicationForKKTWithoutFNChangeImpl(String kktName, Optional<Boolean> markedGoodsUsage) {
        val state = getState(kktName);
        assertThat(state.getRegState())
                .describedAs("KKT %s needs to be registered before reregistration", kktName)
                .isNotNull()
                .isInstanceOf(RegState.Success.class);

        val prevRegNumber = ((RegState.Success) state.getRegState()).number();
        val changes = EnumSet.of(ReregistrationInfo.Change.OTHER);
        val reregInfo = new ReregistrationInfo(changes, prevRegNumber,
                Optional.empty(), Optional.empty());

        val prevKkt = state.getKkt();
        val newFn = TestData.obtainUnusedFn();
        val updatedKkt = new Kkt(prevKkt.serialNumber(), prevKkt.modelName(), prevKkt.mode(), newFn, prevKkt.address(),
                prevKkt.automatedSystemNumber(), Optional.of(Kkt.FfdVersion.V120));

        val request = new CreateReregistrationAppRequest(updatedKkt, TestData.FIRM, TestData.OFD, reregInfo,
                Optional.of(ApplicationVersion.V505), markedGoodsUsage);
        val application = client.createApplication(request);

        updateState(kktName, state.withApplication(application).withKkt(updatedKkt));
    }

    @When("We send obtained reregistration application for KKT {string} to fns")
    public void weSendObtainedReregistrationApplicationForKKTToFns(String kktName) {
        val state = getState(kktName);
        assertThat(state.getApplication())
                .describedAs("Application for KKT '%s' not found", kktName)
                .isNotNull();

        val requestId = RegRequestId.random();
        val response = client.createReregistration(requestId, state.getApplication().getValue());
        assertThat((Object) response.status())
                .isEqualTo(HttpStatus.OK);

        updateState(kktName, state.withRegRequestId(requestId));
    }

    @When("We poll KKT {string} reregistration state within {int} minutes")
    public void wePollKKTReregistrationStateWithinMinutes(String kktName, int waitForMinutes) {
        val state = getState(kktName);
        assertThat(state.getRegRequestId())
                .describedAs("Reregistration request for KKT '%s' not found", kktName)
                .isNotNull();

        val resultState = await()
                .ignoreNoExceptions()
                .timeout(Duration.ofMinutes(waitForMinutes))
                .pollInterval(POLL_INTERVAL)
                .until(
                        () -> client.getReregistrationState(state.getRegRequestId()),
                        regState -> !(regState instanceof RegState.InProcess)
                );

        updateState(kktName, state.withRegState(resultState));
    }

    @Then("Application generation for KKT {string} completes with success")
    public void applicationGenerationForKKTCompletesWithSuccess(String kktName) {
        val state = findState(kktName);
        assertThat(state)
                .describedAs("Application for KKT '%s' not found", kktName)
                .get()
                .extracting(KktState::getApplication)
                .isNotNull();
    }

    @Then("Application sending for KKT {string} completes with success")
    public void applicationSendingForKKTCompletesWithSuccess(String kktName) {
        val state = findState(kktName);
        assertThat(state)
                .describedAs("RegRequest for KKT '%s' not found", kktName)
                .get()
                .extracting(KktState::getRegRequestId)
                .isNotNull();
    }

    @Then("KKT {string} registration state become registered")
    public void kktRegistrationStateBecomeRegistered(String kktName) {
        val state = findState(kktName);
        assertThat(state)
                .describedAs("[Re]Registration result for KKT '%s' not found", kktName)
                .get()
                .extracting(KktState::getRegState)
                .isNotNull();

        assertThat(state)
                .get()
                .extracting(KktState::getRegState)
                .isInstanceOf(RegState.Success.class);
    }

    @Then("KKT {string} registration report state become rejected due to fpd invalid value")
    public void kktRegistrationReportStateBecomeRejected(String kktName) {
        val state = findState(kktName);
        assertThat(state)
                .describedAs("[Re]Registration report result for KKT '%s' not found", kktName)
                .get()
                .extracting(KktState::getRegReportState)
                .isNotNull();

        assertThat(state)
                .get()
                .extracting(KktState::getRegReportState)
                .isInstanceOf(RegReportState.Rejected.class)
                .hasFieldOrPropertyWithValue("code", "9")
                .hasFieldOrPropertyWithValue("reason", "фискальный признак отчета ФН имеет неверное значение");
    }

    @Then("KKT {string} registration state become rejected")
    public void kktRegistrationStateBecomeRejected(String kktName) {
        val state = findState(kktName);
        assertThat(state)
                .describedAs("[Re]Registration result for KKT '%s' not found", kktName)
                .get()
                .extracting(KktState::getRegState)
                .isNotNull();

        assertThat(state)
                .get()
                .extracting(KktState::getRegState)
                .isInstanceOf(RegState.Rejected.class);
    }

    @When("We generate withdraw application for KKT {string} with reason {withdraw_reason}")
    public void weGenerateWithdrawApplicationForKKT(String kktName, WithdrawReason reason) {
        val state = getState(kktName);
        assertThat(state.getRegState())
                .describedAs("KKT %s needs to be registered before withdraw", kktName)
                .isNotNull()
                .isInstanceOf(RegState.Success.class);

        val kkt = state.getKkt();
        val prevRegNumber = ((RegState.Success) state.getRegState()).number();

        val report = (reason == WithdrawReason.FISCAL_CLOSE)
                ? Optional.of(new Report(LocalDateTime.now(), 1, "1"))
                : Optional.<Report>empty();

        val request = new CreateWithdrawAppRequest(TestData.FIRM, kkt.serialNumber(), kkt.modelName(), prevRegNumber,
                reason, report);
        val application = client.createApplication(request);

        updateState(kktName, state.withApplication(application));
    }

    @When("We send obtained withdraw application for KKT {string} to fns")
    public void weSendObtainedWithdrawApplicationForKKTToFns(String kktName) {
        val state = getState(kktName);
        assertThat(state.getApplication())
                .describedAs("Application for KKT '%s' not found", kktName)
                .isNotNull();

        val requestId = RegRequestId.random();
        val response = client.createWithdraw(requestId, state.getApplication().getValue());
        assertThat((Object) response.status())
                .isEqualTo(HttpStatus.OK);

        updateState(kktName, state.withRegRequestId(requestId));
    }

    @When("We poll KKT {string} withdraw state within {int} minute(s)")
    public void wePollKKTWithdrawStateWithinMinutes(String kktName, int waitForMinutes) {
        val state = getState(kktName);
        assertThat(state.getRegRequestId())
                .describedAs("Withdraw request for KKT '%s' not found", kktName)
                .isNotNull();

        val resultState = await()
                .ignoreNoExceptions()
                .timeout(Duration.ofMinutes(waitForMinutes))
                .pollInterval(POLL_INTERVAL)
                .until(
                        () -> client.getWithdrawState(state.getRegRequestId()),
                        regState -> !(regState instanceof WithdrawState.InProcess)
                );

        updateState(kktName, state.withWithdrawState(resultState));
    }

    @Then("KKT {string} withdraw state become rejected")
    public void kktWithdrawStateBecomeRejected(String kktName) {
        val state = findState(kktName);
        assertThat(state)
                .describedAs("Withdraw result for KKT '%s' not found", kktName)
                .get()
                .extracting(KktState::getWithdrawState)
                .isNotNull();

        assertThat(state)
                .get()
                .extracting(KktState::getWithdrawState)
                .isInstanceOf(WithdrawState.Rejected.class);
    }
}
