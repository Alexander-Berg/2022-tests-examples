package ru.yandex.payments.fnsreg;

import java.util.Optional;

import javax.inject.Inject;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

import ru.yandex.payments.fnsreg.controller.dto.CreateRegReportRequest;
import ru.yandex.payments.fnsreg.dto.Firm;
import ru.yandex.payments.fnsreg.dto.Ofd;
import ru.yandex.payments.fnsreg.dto.Report;
import ru.yandex.payments.fnsreg.manager.RegistrationManager;

import static org.mockito.Mockito.mock;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_INN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_KPP;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_OGRN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_SONO_INITIAL;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_SONO_TARGET;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FIRM_URL;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FN_SN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_OFD_INN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_OFD_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REG_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REG_REPORT_FN_SIGN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REG_REPORT_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REG_REPORT_TIME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_FIRST_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_LAST_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_MIDDLE_NAME;

@MicronautTest
@Property(name = TestClockFactory.ENABLE_TEST_CLOCK, value = "true")
public class RegReportApplicationGenerationTest {
    @Inject
    FnsRegClient client;

    @MockBean(RegistrationManager.class)
    public RegistrationManager registrationManagerMock() {
        return mock(RegistrationManager.class);
    }

    @Test
    @DisplayName("Verify that `/regreport/application/create` returns expected registration report application")
    public void testRegReportGeneration() {
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

        val ofd = new Ofd(
                DEFAULT_OFD_INN,
                DEFAULT_OFD_NAME
        );

        val report = new Report(DEFAULT_REG_REPORT_TIME, DEFAULT_REG_REPORT_NUMBER, DEFAULT_REG_REPORT_FN_SIGN);

        val request = new CreateRegReportRequest(DEFAULT_REG_NUMBER, DEFAULT_FN_SN,
                firm, ofd, report);

        val expectedResult = TestAppGenerator.builder()
                .build()
                .toRegReportApp();

        val result = client.createApplication(request);
        XmlAssert.assertThat(result.getValue()).and(expectedResult)
                .ignoreWhitespace()
                .ignoreChildNodesOrder()
                .ignoreComments()
                .areIdentical();
    }
}
