package ru.yandex.payments.fnsreg;

import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.assertj3.XmlAssert;

import ru.yandex.payments.fnsreg.controller.dto.CreateRegistrationAppRequest;
import ru.yandex.payments.fnsreg.controller.dto.CreateReregistrationAppRequest;
import ru.yandex.payments.fnsreg.dto.ApplicationVersion;
import ru.yandex.payments.fnsreg.dto.Firm;
import ru.yandex.payments.fnsreg.dto.Fn;
import ru.yandex.payments.fnsreg.dto.Kkt;
import ru.yandex.payments.fnsreg.dto.Ofd;
import ru.yandex.payments.fnsreg.dto.Report;
import ru.yandex.payments.fnsreg.dto.ReregistrationInfo;
import ru.yandex.payments.fnsreg.dto.ReregistrationInfo.Change;
import ru.yandex.payments.fnsreg.manager.RegistrationManager;

import static org.mockito.Mockito.mock;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_CITY_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_FIAS;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_HOME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_MUNICIPALITY_REGION_CODE;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_MUNICIPALITY_REGION_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_POST_CODE;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_REGION_CODE;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_SONO;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_ADDRESS_STREET_NAME;
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
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FN_MODEL_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FN_SN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_KKT_AUTOMATED_SYS_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_KKT_MODEL_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_KKT_SN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_MARKED_GOODS_USAGE_FLAG;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_OFD_INN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_OFD_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_PREV_REG_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REREG_REPORT_FN_SIGN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REREG_REPORT_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_REREG_REPORT_TIME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_FIRST_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_LAST_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_SIGNER_MIDDLE_NAME;
import static ru.yandex.payments.fnsreg.dto.Kkt.Mode.INTERNET;

@MicronautTest
@Property(name = TestClockFactory.ENABLE_TEST_CLOCK, value = "true")
class RegApplicationGenerationTest {
    @Inject
    FnsRegClient client;

    @MockBean(RegistrationManager.class)
    public RegistrationManager registrationManagerMock() {
        return mock(RegistrationManager.class);
    }

    private static Optional<Kkt.AddressElement> addrElement(String value, String type) {
        return Optional.of(new Kkt.AddressElement(value, type));
    }

    private static Optional<Kkt.AddressElement> noAddrElement() {
        return Optional.empty();
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that `/registration/application/create` returns expected registration application")
    void testRegistrationAppGeneration(ApplicationVersion version) {
        val signer = new Firm.Signer(
                DEFAULT_SIGNER_FIRST_NAME,
                Optional.of(DEFAULT_SIGNER_MIDDLE_NAME),
                DEFAULT_SIGNER_LAST_NAME,
                Optional.empty()
        );

        val address = new Kkt.Address(
                DEFAULT_ADDRESS_FIAS,
                DEFAULT_ADDRESS_SONO,
                DEFAULT_ADDRESS_POST_CODE,
                DEFAULT_ADDRESS_REGION_CODE,
                DEFAULT_ADDRESS_MUNICIPALITY_REGION_NAME,
                DEFAULT_ADDRESS_MUNICIPALITY_REGION_CODE,
                addrElement(DEFAULT_ADDRESS_CITY_NAME, "город"),
                addrElement(DEFAULT_ADDRESS_STREET_NAME, "улица"),
                addrElement(DEFAULT_ADDRESS_HOME, "дом")
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

        val fn = new Fn(
                DEFAULT_FN_SN,
                DEFAULT_FN_MODEL_NAME
        );

        val kktMode = EnumSet.of(INTERNET);
        val kkt = new Kkt(
                DEFAULT_KKT_SN,
                DEFAULT_KKT_MODEL_NAME,
                kktMode,
                fn,
                address,
                Optional.of(DEFAULT_KKT_AUTOMATED_SYS_NUMBER),
                Optional.of(Kkt.FfdVersion.V120)
        );

        val ofd = new Ofd(
                DEFAULT_OFD_INN,
                DEFAULT_OFD_NAME
        );

        val markedGoodsUsage = version ==  ApplicationVersion.V504
                ? Optional.<Boolean>empty()
                : Optional.of(DEFAULT_MARKED_GOODS_USAGE_FLAG);

        val ffdVersion = version == ApplicationVersion.V504
                ? Optional.<Kkt.FfdVersion>empty()
                : Optional.of(Kkt.FfdVersion.V120);

        val request = new CreateRegistrationAppRequest(
                kkt,
                firm,
                ofd,
                Optional.of(version),
                markedGoodsUsage
        );

        val expectedResult = TestAppGenerator.builder()
                .appVersion(version)
                .kktMode(kktMode)
                .markedGoodsUsage(markedGoodsUsage)
                .ffdVersion(ffdVersion)
                .build()
                .toRegistrationApp();

        val result = client.createApplication(request);
        XmlAssert.assertThat(result.getValue()).and(expectedResult)
                .ignoreWhitespace()
                .ignoreChildNodesOrder()
                .ignoreComments()
                .areIdentical();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testReregistrationAppGeneration() {
        return Stream.of(ApplicationVersion.values())
                .flatMap(ver -> Stream.of(Arguments.of(ver, true),
                                          Arguments.of(ver, false)));
    }

    @ParameterizedTest
    @MethodSource()
    @DisplayName("Verify that `/reregistration/application/create` returns expected reregistration application")
    void testReregistrationAppGeneration(ApplicationVersion version, boolean passMarkedGoodsUsage) {
        val signer = new Firm.Signer(
                DEFAULT_SIGNER_FIRST_NAME,
                Optional.of(DEFAULT_SIGNER_MIDDLE_NAME),
                DEFAULT_SIGNER_LAST_NAME,
                Optional.of("doc")
        );

        val address = new Kkt.Address(
                DEFAULT_ADDRESS_FIAS,
                DEFAULT_ADDRESS_SONO,
                DEFAULT_ADDRESS_POST_CODE,
                DEFAULT_ADDRESS_REGION_CODE,
                DEFAULT_ADDRESS_MUNICIPALITY_REGION_NAME,
                DEFAULT_ADDRESS_MUNICIPALITY_REGION_CODE,
                addrElement(DEFAULT_ADDRESS_CITY_NAME, "город"),
                noAddrElement(),
                addrElement(DEFAULT_ADDRESS_HOME, "дом")
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

        val fn = new Fn(
                DEFAULT_FN_SN,
                DEFAULT_FN_MODEL_NAME
        );

        val kktMode = EnumSet.of(INTERNET);
        val kkt = new Kkt(
                DEFAULT_KKT_SN,
                DEFAULT_KKT_MODEL_NAME,
                kktMode,
                fn,
                address,
                Optional.of(DEFAULT_KKT_AUTOMATED_SYS_NUMBER),
                Optional.empty()
        );

        val ofd = new Ofd(
                DEFAULT_OFD_INN,
                DEFAULT_OFD_NAME
        );

        val kktChanges = EnumSet.of(
                Change.FN,
                Change.OTHER
        );

        val reregReport = new Report(
                DEFAULT_REREG_REPORT_TIME,
                DEFAULT_REREG_REPORT_NUMBER,
                DEFAULT_REREG_REPORT_FN_SIGN
        );

        val closeFiscalReport = new Report(
                DEFAULT_CLOSE_FISCAL_REPORT_TIME,
                DEFAULT_CLOSE_FISCAL_REPORT_NUMBER,
                DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN
        );

        val reregistrationInfo = new ReregistrationInfo(
                kktChanges,
                DEFAULT_PREV_REG_NUMBER,
                Optional.of(reregReport),
                Optional.of(closeFiscalReport)
        );

        val markedGoodsUsage = (version ==  ApplicationVersion.V504) || !passMarkedGoodsUsage
                ? Optional.<Boolean>empty()
                : Optional.of(DEFAULT_MARKED_GOODS_USAGE_FLAG);

        val request = new CreateReregistrationAppRequest(
                kkt,
                firm,
                ofd,
                reregistrationInfo,
                Optional.of(version),
                markedGoodsUsage
        );

        val expectedResult = TestAppGenerator.builder()
                .appVersion(version)
                .signerDocument(Optional.of("doc"))
                .addressStreetName(Optional.empty())
                .kktMode(kktMode)
                .changeSet(kktChanges)
                .markedGoodsUsage(markedGoodsUsage)
                .build()
                .toReregistrationApp();

        val result = client.createApplication(request);
        XmlAssert.assertThat(result.getValue()).and(expectedResult)
                .ignoreWhitespace()
                .ignoreChildNodesOrder()
                .ignoreComments()
                .areIdentical();
    }

    @Test
    @DisplayName("Verify that a reregistration report without fn change returns expected rregistration application")
    void testReregistrationWithoutFNReplacement() {
        val signer = new Firm.Signer(
                DEFAULT_SIGNER_FIRST_NAME,
                Optional.of(DEFAULT_SIGNER_MIDDLE_NAME),
                DEFAULT_SIGNER_LAST_NAME,
                Optional.of("doc")
        );

        val address = new Kkt.Address(
                DEFAULT_ADDRESS_FIAS,
                DEFAULT_ADDRESS_SONO,
                DEFAULT_ADDRESS_POST_CODE,
                DEFAULT_ADDRESS_REGION_CODE,
                DEFAULT_ADDRESS_MUNICIPALITY_REGION_NAME,
                DEFAULT_ADDRESS_MUNICIPALITY_REGION_CODE,
                addrElement(DEFAULT_ADDRESS_CITY_NAME, "город"),
                noAddrElement(),
                addrElement(DEFAULT_ADDRESS_HOME, "дом")
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

        val fn = new Fn(
                DEFAULT_FN_SN,
                DEFAULT_FN_MODEL_NAME
        );

        val kktMode = EnumSet.of(INTERNET);
        val kkt = new Kkt(
                DEFAULT_KKT_SN,
                DEFAULT_KKT_MODEL_NAME,
                kktMode,
                fn,
                address,
                Optional.of(DEFAULT_KKT_AUTOMATED_SYS_NUMBER),
                Optional.of(Kkt.FfdVersion.V120)
        );

        val ofd = new Ofd(
                DEFAULT_OFD_INN,
                DEFAULT_OFD_NAME
        );

        val kktChanges = EnumSet.of(
                Change.OTHER
        );

        val reregistrationInfo = new ReregistrationInfo(
                kktChanges,
                DEFAULT_PREV_REG_NUMBER,
                Optional.empty(),
                Optional.empty()
        );

        val markedGoodsUsage = Optional.of(true);

        val request = new CreateReregistrationAppRequest(
                kkt,
                firm,
                ofd,
                reregistrationInfo,
                Optional.of(ApplicationVersion.V505),
                markedGoodsUsage
        );

        val expectedResult = TestAppGenerator.builder()
                .appVersion(ApplicationVersion.V505)
                .signerDocument(Optional.of("doc"))
                .addressStreetName(Optional.empty())
                .kktMode(kktMode)
                .changeSet(kktChanges)
                .markedGoodsUsage(markedGoodsUsage)
                .ffdVersion(Optional.of(Kkt.FfdVersion.V120))
                .build()
                .toReregistrationApp();

        val result = client.createApplication(request);
        XmlAssert.assertThat(result.getValue()).and(expectedResult)
                .ignoreWhitespace()
                .ignoreChildNodesOrder()
                .ignoreComments()
                .areIdentical();
    }
}
