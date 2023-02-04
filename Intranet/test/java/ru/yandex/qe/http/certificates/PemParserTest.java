package ru.yandex.qe.http.certificates;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author lvovich
 */
public class PemParserTest {

    @Test
    public void certificates() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sample.pem");
        String pem = IOUtils.toString(inputStream);
        List<X509Certificate> certificates = PemParser.parseAll(pem).stream().filter(X509Certificate.class::isInstance).map(X509Certificate.class::cast).collect(Collectors.toList());
        Assertions.assertEquals(certificates.size(), 3);
        X509Certificate certificate = certificates.get(0);
        Assertions.assertEquals(certificate.getSubjectDN().getName(), "C=RU,ST=Russian Federation,L=Moscow,O=Yandex LLC,OU=ITO,CN=auth.qe-dev.yandex-team.ru,E=pki@yandex-team.ru");
        Assertions.assertEquals(certificate.getSubjectAlternativeNames().size(), 1);
        List<?> altNames = certificate.getSubjectAlternativeNames().iterator().next();
        Assertions.assertEquals(altNames.get(0), 2);
        Assertions.assertEquals(altNames.get(1), "auth.qe-dev.yandex-team.ru");
        // expected expired certificate - just for test
        Assertions.assertFalse(new DateTime(certificate.getNotAfter()).isAfter(DateTime.now()));
    }

    @Test
    public void privateKey() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sample.pem");
        String pem = IOUtils.toString(inputStream);
        List<PrivateKey> keys = PemParser.parseAll(pem).stream().filter(PrivateKey.class::isInstance).map(PrivateKey.class::cast).collect(Collectors.toList());
        Assertions.assertEquals(keys.size(), 1);
        Assertions.assertEquals(keys.get(0).getFormat(), "PKCS#8");
    }

    @Test
    public void questionMark() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("questionMark.pem");
        String pem = IOUtils.toString(inputStream);
        List<PrivateKey> keys = null;
        try {
            keys = PemParser.parseAll(pem).stream().filter(PrivateKey.class::isInstance).map(PrivateKey.class::cast).collect(Collectors.toList());
            Assertions.fail("Invalid pem accepted");
        } catch (IllegalArgumentException e) {
            //fine
        }
    }

    @Test
    public void extraSpaces() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extraSpaces.pem");
        String pem = IOUtils.toString(inputStream);
        List<PrivateKey> keys =  PemParser.parseAll(pem).stream().filter(PrivateKey.class::isInstance).map(PrivateKey.class::cast).collect(Collectors.toList());
        // private keys ans certificates with wrong headers are just ignored by PemParser
        Assertions.assertEquals(keys.size(), 0);
        List<X509Certificate> certificates = PemParser.parseAll(pem).stream().filter(X509Certificate.class::isInstance).map(X509Certificate.class::cast).collect(Collectors.toList());
        Assertions.assertEquals(certificates.size(), 2);
    }


}
