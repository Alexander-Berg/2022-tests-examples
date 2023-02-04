package ru.yandex.payments.fnsreg;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.assertj3.XmlAssert;

import ru.yandex.payments.fnsreg.controller.dto.CreateWithdrawAppRequest;
import ru.yandex.payments.fnsreg.dto.Firm;
import ru.yandex.payments.fnsreg.dto.Report;
import ru.yandex.payments.fnsreg.manager.RegistrationManager;
import ru.yandex.payments.fnsreg.types.Withdraw;

import static org.mockito.Mockito.mock;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_CLOSE_FISCAL_REPORT_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_CLOSE_FISCAL_REPORT_TIME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_INN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_KPP;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_OGRN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_SONO_INITIAL;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_SONO_TARGET;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_URL;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_KKT_MODEL_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_KKT_SN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_PREV_REG_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_FIRST_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_LAST_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_MIDDLE_NAME;

@MicronautTest
@Property(name = TestClockFactory.ENABLE_TEST_CLOCK, value = "true")
class WithdrawApplicationGenerationTest {
    @Inject
    FnsRegClient client;

    @MockBean(RegistrationManager.class)
    public RegistrationManager registrationManagerMock() {
        return mock(RegistrationManager.class);
    }

    static Stream<Arguments> testArgs() {
        return Stream.of(
                Arguments.of(Withdraw.KKT_STOLEN),
                Arguments.of(Withdraw.KKT_MISSING),
                Arguments.of(Withdraw.FN_BROKEN),
                Arguments.of(new Withdraw.FiscalClose(
                                new Report(
                                        DEFAULT_CLOSE_FISCAL_REPORT_TIME,
                                        DEFAULT_CLOSE_FISCAL_REPORT_NUMBER,
                                        DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testArgs")
    @DisplayName("Verify that `/withdraw/application/create` returns expected withdraw application")
    void testWithdrawAppGeneration(Withdraw withdraw) {
        val signer = new Firm.Signer(
                DEFAULT_SIGNER_FIRST_NAME,
                Optional.of(DEFAULT_SIGNER_MIDDLE_NAME),
                DEFAULT_SIGNER_LAST_NAME,
                Optional.empty()
        );

        val firm = new Firm(
                DEFAULT_FIRM_INN,
                DEFAULT_FIRM_NAME,
                DEFAULT_FIRM_URL,
                DEFAULT_FIRM_KPP,
                DEFAULT_FIRM_OGRN,
                signer,
                DEFAULT_FIRM_SONO_INITIAL,
                DEFAULT_FIRM_SONO_TARGET
        );

        val request = new CreateWithdrawAppRequest(
                firm,
                DEFAULT_KKT_SN,
                DEFAULT_KKT_MODEL_NAME,
                DEFAULT_PREV_REG_NUMBER,
                withdraw.reason(),
                withdraw instanceof Withdraw.FiscalClose fiscalClose
                        ? Optional.of(fiscalClose.report())
                        : Optional.empty()
        );

        val expectedResult = TestAppGenerator.builder()
                .withdraw(withdraw)
                .build()
                .toWithdrawApp();

        val result = client.createApplication(request);
        XmlAssert.assertThat(result.getValue()).and(expectedResult)
                .ignoreWhitespace()
                .ignoreChildNodesOrder()
                .ignoreComments()
                .areIdentical();
    }
}
