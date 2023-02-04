package ru.yandex.payments.fnsreg;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.Valid;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.fnsreg.signapi.SignMeta;
import ru.yandex.payments.fnsreg.signapi.YaSignClient;
import ru.yandex.payments.fnsreg.signapi.dto.DSSSignDocumentResponse;
import ru.yandex.payments.fnsreg.signapi.dto.DssCertStatus;
import ru.yandex.payments.fnsreg.signapi.dto.DssCertType;
import ru.yandex.payments.fnsreg.signapi.dto.DssCertificate;
import ru.yandex.payments.fnsreg.signapi.dto.DssCertificateStatus;
import ru.yandex.payments.fnsreg.signapi.dto.SignRequest;
import ru.yandex.payments.fnsreg.signapi.dto.SignResponse;
import ru.yandex.payments.fnsreg.signapi.dto.UserCertificateRequest;
import ru.yandex.payments.fnsreg.signapi.dto.UserCertificateResponse;
import ru.yandex.payments.fnsreg.signapi.dto.YaSignError;
import ru.yandex.payments.fnsreg.types.CertificateId;
import ru.yandex.payments.fnsreg.types.EncodedApplication;
import ru.yandex.payments.fnsreg.types.Inn;
import ru.yandex.payments.fnsreg.types.Ogrn;
import ru.yandex.payments.fnsreg.types.SignedApplication;
import ru.yandex.payments.fnsreg.types.TaxUnitCode;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "classpath:application-ya-sign-client-test.yml")
class YaSignClientTest {
    // CHECKSTYLE:OFF
    // language=XML
    private static final String APPLICATION =
            """
            <?xml version='1.0' encoding='utf-8'?>
            <Файл ВерсПрог="1.0" ВерсФорм="5.04" ИдФайл="KO_ZVLREGKKT_0123_4567_7704448842770401001_19700101_381004050054">
                <Документ ДатаДок="01.01.1970" КНД="1110061" КодНО="4567">
                    <СвНП>
                        <НПЮЛ ИННЮЛ="7704448842"
                              КПП="770401001"
                              НаимОрг="ООО &quot;Рога и копыта&quot;"
                              ОГРН="5177746308394"/>
                    </СвНП>
                    <Подписант ПрПодп="1">
                        <ФИО Имя="Зиц" Отчество="Председатель" Фамилия="Фунт"/>
                    </Подписант>
                    <ЗаявРегККТ ВидДок="1" КодНОМУст="0536">
                        <СведРегККТ ЗаводНомерККТ="88888888888888888888"
                                    ЗаводНомерФН="8888888888888888"
                                    МоделККТ="РП Система 1ФА"
                                    МоделФН="«ФН-1.1» исполнение 5-15-2"
                                    ПрАвтоматУстр="1"
                                    ПрАвтоном="2"
                                    ПрАзарт="2"
                                    ПрАкцизТовар="2"
                                    ПрБанкПлат="2"
                                    ПрБланк="2"
                                    ПрИнтернет="1"
                                    ПрЛотерея="2"
                                    ПрПлатАгент="2"
                                    ПрРазвозРазнос="2">
                            <СведОФД ИННЮЛ="7704358518" НаимОрг="ООО &quot;Лучший.ОФД&quot;"/>
                            <СведАдрМУст НаимМУст="hh.com">
                                <АдрМУстККТ>
                                    <АдрФИАС ИдНом="1b533431-3f0a-45b8-addc-69c3e1b19a8b" Индекс="141281">
                                        <Регион>50</Регион>
                                        <МуниципРайон ВидКод="2" Наим="Город Н."/>
                                        <НаселенПункт Наим="Н" Вид="город"/>
                                        <ЭлУлДорСети Наим="Советская" Тип="улица"/>
                                        <Здание Номер="42" Тип="дом"/>
                                    </АдрФИАС>
                                </АдрМУстККТ>
                            </СведАдрМУст>
                            <СведАвтУстр НомерАвтоматУстр="test-as-num" НаимМУст="hh.com">
                                <АдрМУстАвтУстр>
                                    <АдрФИАС ИдНом="1b533431-3f0a-45b8-addc-69c3e1b19a8b" Индекс="141281">
                                        <Регион>50</Регион>
                                        <МуниципРайон ВидКод="2" Наим="Город Н."/>
                                        <НаселенПункт Наим="Н" Вид="город"/>
                                        <ЭлУлДорСети Наим="Советская" Тип="улица"/>
                                        <Здание Номер="42" Тип="дом"/>
                                    </АдрФИАС>
                                </АдрМУстАвтУстр>
                            </СведАвтУстр>
                        </СведРегККТ>
                    </ЗаявРегККТ>
                </Документ>
            </Файл>
            """;
    // CHECKSTYLE:ON

    private static final EncodedApplication ENCODED_APPLICATION = new EncodedApplication(
            new String(
                    Base64.getEncoder().encode(APPLICATION.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8
            )
    );

    private static final SignedApplication SIGNED_APPLICATION = new SignedApplication(
            ENCODED_APPLICATION.getValue() + "---sign"
    );

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String FILENAME = "KO_ZVLREGKKT_0123_4567_7704448842770401001_19700101_381004050054";
    private static final String TEST_LOGIN = "test";
    private static final TaxUnitCode TAX_UNIT_CODE = new TaxUnitCode("YNDX");
    private static final String CERT_META = "CN=ООО \"Рога и копыта\", SN=ФУНТ, G=ЗИЦ ПРЕДСЕДАТЕЛЬ, C=RU, S=77 " +
            "Москва, L=Москва, STREET=\"ул. Халтурина, д 18\", O=ООО \"Рога и копыта\", T=Руководитель, " +
            "OGRN=5177746308394, SNILS=00000000000, INN=7704448842, E=devnull@yandex-team.ru";

    @Inject
    YaSignClient client;

    @Controller
    static class YaSignMockController {
        @Post("/api/sign/uniSignCMS")
        public SignResponse sign(@SuppressWarnings("unused") @Body @Valid SignRequest request) {
            val signDoc = new DSSSignDocumentResponse(singletonList(SIGNED_APPLICATION.getValue()), emptyList());
            return new SignResponse.Success(signDoc);
        }

        @Post("/api/cert/getUserCertList")
        public UserCertificateResponse findCertificates(@Body @Valid UserCertificateRequest request) {
            if (request.login().equals(TEST_LOGIN)) {
                val now = LocalDateTime.now();
                val cert = new DssCertificate(
                        DssCertStatus.ACTIVE,
                        new CertificateId(42L),
                        DssCertificateStatus.ACTIVE,
                        CERT_META,
                        DssCertType.ORG,
                        Optional.of(TAX_UNIT_CODE),
                        now,
                        now.plusDays(1),
                        true
                );
                return new UserCertificateResponse.Result(singletonList(cert));
            } else {
                return new YaSignError("No certificates", 42L);
            }
        }
    }

    @Test
    @DisplayName("Verify that YaSignClient signs applications correctly")
    void testSign() {
        val meta = new SignMeta(
                FILENAME,
                new Inn("7704448842"),
                new Ogrn("5177746308394"),
                "Зиц",
                "Фунт",
                Optional.of("Председатель")
        );
        assertThat(client.sign(ENCODED_APPLICATION, meta).block(WAIT_TIMEOUT))
                .isNotNull()
                .isEqualTo(SIGNED_APPLICATION);
    }
}
