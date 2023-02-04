package ru.yandex.webmaster3.validator.xml;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.validator.model.ErrorDescription;
import ru.yandex.webmaster3.validator.common.wsw.model.WswErrorDescription;

import java.io.InputStream;
import java.util.List;


/**
 * @author leonidrom
 *
 * У команды Маркета нет xsd файла, поэтому тесты проверяют что наш xsd соответствует
 * примерам из https://yandex.ru/support/partnermarket/elements/shop.html
 */
public class MarketValidationTest {
    @Test
    public void testValidateDeliveryOptions() {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options1.xml");
        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options2.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options3.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options4.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options5.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options6.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_delivery_options7.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);
    }

    @Test
    public void testValidatePickupOptions() {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/shop_pickup_options1.xml");
        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_pickup_options2.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_pickup_options3.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_pickup_options4.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_pickup_options5.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_pickup_options6.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

    }

    @Test
    public void testValidateOffer() {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/shop_offer1.xml");
        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer2.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer3.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer4.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer5.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer6.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer7.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer8.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer9.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer10.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_offer11.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);
    }

    @Test
    public void testValidatePromo() {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/shop_promo1.xml");
        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_promo2.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_promo3.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);

        xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        xmlIs = getClass().getResourceAsStream("/xml/shop_promo4.xml");
        errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);
    }

    @Test
    public void testValidateExample() {
        XmlParser xmlParser = new XmlParser();
        InputStream xsdIs = getClass().getResourceAsStream("/xsd/shops.xsd");
        InputStream xmlIs = getClass().getResourceAsStream("/xml/shop_example.xml");

        List<ErrorDescription> errors = xmlParser.validate(xmlIs, xsdIs);
        validateResponse(errors);
    }

    private void validateResponse(List<ErrorDescription> errors) {
        Assert.assertTrue(errorsToString(errors), errors.isEmpty());

        WswErrorDescription descr = new WswErrorDescription(errors);

        StringBuilder sb = new StringBuilder();
        descr.toJson(sb);
        Assert.assertEquals("\"ok\"", sb.toString());

        sb = new StringBuilder();
        descr.toXml(sb);
        Assert.assertEquals("<ok/>\n", sb.toString());
    }

    private String errorsToString(List<ErrorDescription> errors) {
        StringBuilder sb = new StringBuilder();
        errors.forEach(e -> e.toJson(sb));

        return sb.toString();
    }
}
