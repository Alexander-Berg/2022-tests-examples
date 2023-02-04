package ru.yandex.webmaster3.core.turbo.model.commerce.delivery;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author: ishalaru
 * DATE: 10.07.2019
 */
public class DeliverySettingsTest {
    @Test
    public void test() throws IOException {

        ObjectMapper OM = new ObjectMapper();
        MailDeliverySettings mailDeliverySettings = new MailDeliverySettings("test", 1, 100, BigDecimal.ONE, BigDecimal.ONE);
        PickupDeliverySettings pickupDeliverySettings = new PickupDeliverySettings("new", "time", BigDecimal.ONE, BigDecimal.ONE, 0, 1);
        CourierDeliverySettings courierDeliverySettings = new CourierDeliverySettings("self", 2, 2, BigDecimal.ONE, BigDecimal.ONE);
        DeliverySection deliverySection = new DeliverySection(List.of(mailDeliverySettings), List.of(pickupDeliverySettings), List.of(courierDeliverySettings), null, null, null);

        String value = OM.writerWithDefaultPrettyPrinter().writeValueAsString(deliverySection);
        System.out.println(value);
        DeliverySettings deliverySettings1 = OM.readValue(value, DeliverySection.class);
        Assert.assertEquals("Double transformation", value, OM.writerWithDefaultPrettyPrinter().writeValueAsString(deliverySettings1));
    }
}
