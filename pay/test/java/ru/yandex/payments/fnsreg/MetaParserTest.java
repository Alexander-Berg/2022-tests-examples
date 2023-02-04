package ru.yandex.payments.fnsreg;

import java.util.Optional;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.fnsreg.signapi.meta.CertificateMeta;
import ru.yandex.payments.fnsreg.signapi.meta.MetaKeyNotFoundException;
import ru.yandex.payments.fnsreg.signapi.meta.MetaParseException;
import ru.yandex.payments.fnsreg.signapi.meta.YaSignCertificateMetaParser;
import ru.yandex.payments.fnsreg.types.Inn;
import ru.yandex.payments.fnsreg.types.Ogrn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetaParserTest {
    @Test
    @DisplayName("Verify that ya.sign meta parser correctly parses well-formed meta string")
    void testWellFormed() {
        val metaStr = "CN=ООО Время, SN=РОМАНОВА,G=АЛЛА РОМАНОВНА, C=RU,S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\",O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru,OID.1.2.840.113549.1.9.2=7704448440-770301001-002836413953";
        val expectedMeta = new CertificateMeta(new Inn("3328000568"), new Ogrn("1105252001546"), "АЛЛА РОМАНОВНА",
                "РОМАНОВА", Optional.empty());

        val actualMeta = YaSignCertificateMetaParser.parse(metaStr);
        assertThat(actualMeta)
                .isEqualTo(expectedMeta);
    }

    @Test
    @DisplayName("Verify that ya.sign meta parser throws an error while parsing ill-formed meta string")
    void testIllFormed() {
        val metaStr = "CN = ООО Время,SN==РОМАНОВА; G=АЛЛА РОМАНОВНА, 0C=RU, S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\", O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru";

        assertThatThrownBy(() -> YaSignCertificateMetaParser.parse(metaStr))
                .isInstanceOf(MetaParseException.class);
    }

    @Test
    @DisplayName("Verify that ya.sign meta parser throws an error if expected meta key not found")
    void testKeyNotFound() {
        val metaStr = "SN=РОМАНОВА, C=RU, S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\", O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru";

        assertThatThrownBy(() -> YaSignCertificateMetaParser.parse(metaStr))
                .isInstanceOf(MetaKeyNotFoundException.class);
    }

    @Test
    @DisplayName("Verify that ya.sign meta parser correctly parses well-formed meta string containing a lot of '\"' " +
            "characters")
    void testStringValues() {
        val metaStr = "CN=\"ООО \"Время\"\", SN=РОМАНОВА,G=АЛЛА РОМАНОВНА, C=RU,S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\",O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru,OID.1.2.840.113549.1.9.2=7704448440-770301001-002836413953";
        val expectedMeta = new CertificateMeta(new Inn("3328000568"), new Ogrn("1105252001546"), "АЛЛА РОМАНОВНА",
                "РОМАНОВА", Optional.empty());

        val actualMeta = YaSignCertificateMetaParser.parse(metaStr);
        assertThat(actualMeta)
                .isEqualTo(expectedMeta);
    }

    @Test
    @DisplayName("Verify that ya.sign meta parser correctly parses well-formed meta string containing '\"' characters" +
            " in the middle of a value")
    void testNonStringValuesContainingQuotes() {
        val metaStr = "CN=ООО \"Время\", SN=РОМАНОВА,G=АЛЛА РОМАНОВНА, C=RU,S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\",O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru,OID.1.2.840.113549.1.9.2=7704448440-770301001-002836413953";
        val expectedMeta = new CertificateMeta(new Inn("3328000568"), new Ogrn("1105252001546"), "АЛЛА РОМАНОВНА",
                "РОМАНОВА", Optional.empty());

        val actualMeta = YaSignCertificateMetaParser.parse(metaStr);
        assertThat(actualMeta)
                .isEqualTo(expectedMeta);
    }

    @Test
    @DisplayName("Verify that the INNLE field is parsed correctly")
    void testInnle() {
        val metaStr = "CN=ООО Время, SN=РОМАНОВА,G=АЛЛА РОМАНОВНА, C=RU,S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\",O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru,OID.1.2.840.113549.1.9.2=7704448440-770301001-002836413953,INNLE=7704340310";
        val expectedMeta = new CertificateMeta(new Inn("3328000568"), new Ogrn("1105252001546"), "АЛЛА РОМАНОВНА",
                "РОМАНОВА", Optional.of(new Inn("7704340310")));

        val actualMeta = YaSignCertificateMetaParser.parse(metaStr);
        assertThat(actualMeta)
                .isEqualTo(expectedMeta);
    }

    @Test
    @DisplayName("Verify that the INNLE alias field OID.1.2.643.100.4 is parsed correctly")
    void testInnleAlias() {
        val metaStr = "CN=ООО Время, SN=РОМАНОВА,G=АЛЛА РОМАНОВНА, C=RU,S=77 Москва, L=Москва, " +
                "STREET=\"ул. Халтурина, д 18\",O=ООО Время, T=Руководитель, OGRN=1105252001546, SNILS=00000000000, " +
                "INN=003328000568, E=devnull@yandex-team.ru,OID.1.2.840.113549.1.9.2=7704448440-770301001-002836413953,OID.1.2.643.100.4=7704340310";
        val expectedMeta = new CertificateMeta(new Inn("3328000568"), new Ogrn("1105252001546"), "АЛЛА РОМАНОВНА",
                "РОМАНОВА", Optional.of(new Inn("7704340310")));

        val actualMeta = YaSignCertificateMetaParser.parse(metaStr);
        assertThat(actualMeta)
                .isEqualTo(expectedMeta);
    }
}
