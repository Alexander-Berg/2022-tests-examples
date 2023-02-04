package ru.yandex.wmconsole.servantlet;

import java.io.IOException;
import java.io.StringReader;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.framework.core.DefaultServantInfo;
import ru.yandex.common.framework.http.HttpServRequest;
import ru.yandex.common.framework.http.HttpServResponse;
import ru.yandex.common.framework.xml.StringXmlBuilder;
import ru.yandex.wmtools.common.error.InternalException;
import ru.yandex.wmtools.common.error.UserException;

/**
 * User: azakharov
 * Date: 14.06.12
 * Time: 18:36
 */
public class ValidateValueServantletTest {

    private ValidateValueServantlet servantlet = new ValidateValueServantlet();

    private void validate(String type, String value, boolean expectValid) throws UserException, InternalException, JDOMException, IOException {
        HttpServRequest req = new HttpServRequest(0L, "", "validateValue");
        req.setParam("type", type);
        req.setParam("value", value);
        StringXmlBuilder sb = new StringXmlBuilder();
        DefaultServantInfo si = new DefaultServantInfo();
        si.setName("validateValue");
        sb.setServantInfo(si);
        HttpServResponse res = new HttpServResponse(null, sb, null);
        servantlet.doProcess(req, res);
        String response = new String(res.getXML());
        final StringReader stringReader = new StringReader(response);
        final SAXBuilder builder = new SAXBuilder();
        final Document eventDocument = builder.build(stringReader);
        final Element rootElement = eventDocument.getRootElement();
        Element status = rootElement.getChild("status");
        Assert.assertNotNull(status);
        if (expectValid) {
            Assert.assertEquals("ok", status.getText());
        } else {
            Assert.assertEquals("failed", status.getText());
        }
    }

    @Test
    public void testUrlSimple() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "http://webmaster.yandex.ru", true);
    }

    @Test
    public void testHostname() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "webmaster.yandex.ru", true);
    }

    @Test
    public void testHttps() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "https://webmaster.yandex.ru", true);
    }

    @Test
    public void testPathFragmentQueryParams() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "http://test.ru/site/add.xml#fragment?action=do&host=12345678", true);
    }

    @Test
    public void testCyrrilic() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "http://президент.рф", true);
    }

    @Test
    public void testPunycode() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "http://xn----7sbdclg3arpmwc0ltd.xn--p1ai", true);
    }

    @Test
    public void testUrlEncoded() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "http://host.ru/%20%3A%3C%20", true);
    }

    @Test
    public void testEmptyUrl() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "", false);
    }

    @Test
    public void testUrlSpace() throws JDOMException, IOException, UserException, InternalException {
        validate("url", " ", false);
    }

    @Test(expected = UserException.class)
    public void testUrlNull() throws JDOMException, IOException, UserException, InternalException {
        validate("url", null, false);
    }

    @Test
    public void testInvalidUrl() throws JDOMException, IOException, UserException, InternalException {
        validate("url", "oneword", false);
    }

    @Test
    public void testEmailSimple() throws JDOMException, IOException, UserException, InternalException {
        validate("email", "devnull@yandex.ru", true);
    }

    @Test
    public void testInvalidEmail() throws JDOMException, IOException, UserException, InternalException {
        validate("email", "oneword", false);
    }

    @Test
    public void testEmptyEmail() throws JDOMException, IOException, UserException, InternalException {
        validate("email", "", false);
    }

    @Test
    public void testEmailSpace() throws JDOMException, IOException, UserException, InternalException {
        validate("email", " ", false);
    }

    @Test(expected = UserException.class)
    public void testEmailNull() throws JDOMException, IOException, UserException, InternalException {
        validate("email", null, false);
    }

    @Test
    public void testEmailList() throws JDOMException, IOException, UserException, InternalException {
        validate("email", "devnull@yandex.ru; devnull@narod.ru; devnull@pochta.ru", true);
    }

    @Test
    public void testNewTldEmail() throws JDOMException, IOException, UserException, InternalException {
        validate("email", "info@cottage.center", true);
        validate("email", "info@cottage.centera", false);
    }

    @Test
    public void testPhone() throws JDOMException, IOException, UserException, InternalException {
        // классический вариант
        validate("phone", "+7 (111) 111–11–11", true);
    }

    @Test
    public void testPhoneSpace() throws JDOMException, IOException, UserException, InternalException {
        // допустимый
        validate("phone", "+7 111 111 11 11", true);
    }

    @Test
    public void testPhoneDash() throws JDOMException, IOException, UserException, InternalException {
        // допустимый
        validate("phone", "+7-111-1111111", true);
    }

    @Test
    public void testPhoneUkr() throws JDOMException, IOException, UserException, InternalException {
        // допустимый
        validate("phone", "+380 (800) 1111111", true);
    }
}
