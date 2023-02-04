package ru.yandex.webmaster3.validator.xml;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.webmaster3.core.validator.XmlValidatorSourceType;
import ru.yandex.webmaster3.core.validator.model.ErrorDescription;
import ru.yandex.webmaster3.validator.common.wsw.model.WswErrorDescription;

import java.io.InputStream;
import java.util.List;

/**
 * @author leonidrom
 */
public class XmlValidationTest {
    @Test
    public void testValidateLocalFileOk() throws Exception {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/timetable.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/test.xml");

        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        Assert.assertTrue(errors.isEmpty());

        WswErrorDescription descr = new WswErrorDescription(errors);

        StringBuilder sb = new StringBuilder();
        descr.toJson(sb);
        Assert.assertEquals("\"ok\"", sb.toString());

        sb = new StringBuilder();
        descr.toXml(sb);
        Assert.assertEquals("<ok/>\n", sb.toString());
    }

    @Test
    public void testValidateWithNamespacesOk() throws Exception {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/realty.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/realty.xml");

        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        Assert.assertTrue(errors.isEmpty());

        WswErrorDescription descr = new WswErrorDescription(errors);

        StringBuilder sb = new StringBuilder();
        descr.toJson(sb);
        Assert.assertEquals("\"ok\"", sb.toString());

        sb = new StringBuilder();
        descr.toXml(sb);
        Assert.assertEquals("<ok/>\n", sb.toString());
    }

    @Test
    public void testValidateLocalFileFail() throws Exception {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/timetable.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/test2.xml");

        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        Assert.assertTrue(!errors.isEmpty());

        WswErrorDescription descr = new WswErrorDescription(errors);

        StringBuilder sb = new StringBuilder();
        descr.toJson(sb);
        System.out.println(sb.toString());

        sb = new StringBuilder();
        descr.toXml(sb);
        System.out.println(sb.toString());
    }

    @Test
    @Ignore
    public void testValidateRemoteFileOk() throws Throwable {
        InputStream xsdStream = getClass().getResourceAsStream("/xsd/realty.xsd");
        ValidationSourceResolver resolver = new ValidationSourceResolver();
        InputStream feedStream = resolver.getSourceInputStreams(
                "http://www.realtysar.ru/realty2_xml.php?type=yandex",
                XmlValidatorSourceType.URL, false)[0];
        XmlParser xmlParser = new XmlParser();

        List<ErrorDescription> errors = xmlParser.validate(feedStream, xsdStream);
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    @Ignore
    public void testValidateRemoteGzFileOk() throws Throwable {
        InputStream xsdStream = getClass().getResourceAsStream("/xsd/realty.xsd");
        ValidationSourceResolver resolver = new ValidationSourceResolver();
        InputStream feedStream = resolver.getSourceInputStreams(
                "http://adres-realty.ru/d/774087/d/shop2_3006221-yml.xml.gz",
                XmlValidatorSourceType.URL, false)[0];
        XmlParser xmlParser = new XmlParser();

        List<ErrorDescription> errors = xmlParser.validate(feedStream, xsdStream);
        Assert.assertTrue(errors.isEmpty());
    }
}
