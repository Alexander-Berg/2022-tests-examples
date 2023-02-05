package ru.yandex.market.data.order.description;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;

import androidx.annotation.NonNull;
import ru.yandex.market.checkout.data.dto.DeliveryIntervalDto;
import ru.yandex.market.data.passport.Address;
import ru.yandex.market.testcase.JsonSerializationTestCase;

@RunWith(Enclosed.class)
public class AddressDeliveryPointDtoTest {

    public static class JsonSerializationTest extends JsonSerializationTestCase {

        @NonNull
        @Override
        protected Object getInstance() {
            final AddressDeliveryPointDto instance = new AddressDeliveryPointDto();
            instance.setRegionId(213L);
            instance.setRecipient("market_user_api_d93");
            instance.setPhone("+7111222333444");
            instance.setEmail("example@yandex.ru");
            instance.setDeliveryId(
                    "xY94kCEPaNK57fjL7gaP_oXXsNLnMeviNjhi6a7bYtsLQG-rQdDxudwNiQpAxEPS9LlGeeSQwi1A9uzbs9aX9sYT8yvScZ-N3-rn42s3hih_l_uvOrg4Egel9a7qYNOwCf4THnOb5oWTzlywFcXnrVSUA3A8IcoemA3x_XDbbWYQDxw9nFVMxG8iNVGohNbG_cndlR2rokRjb0kWbyh5GMFxpBrDfzzXwqBykmN8OrKKFgBIuwU6chzm-MFfSF5m3PrIYGPYU44lrRIrwk0ENYO6EAQGagPwlhGV6CyS6SjKsLM86mj2X8WBQlhG4_xKJk8nzXMNqzU");

            final Address address = Address.builder()
                    .country("Россия")
                    .city("Москва")
                    .street("Ленина")
                    .district("Московский")
                    .house("51")
                    .block("1")
                    .room("72")
                    .postCode("127495")
                    .build();
            instance.setAddress(address);

            instance.setDateFrom("2018-05-21");
            instance.setDateTo("2018-05-21");
            instance.setIntervals(Collections.singletonList(
                    DeliveryIntervalDto.testBuilder()
                            .timeFrom("10:00:00")
                            .timeTo("18:00:00")
                            .price(BigDecimal.valueOf(499))
                            .isDefault(true)
                            .build()
            ));
            instance.setUnloadSelected(false);
            return instance;
        }

        @NonNull
        @Override
        protected Type getType() {
            return AddressDeliveryPointDto.class;
        }

        @NonNull
        @Override
        protected JsonSource getJsonSource() {
            return file("AddressDeliveryPointDto.json");
        }
    }
}
