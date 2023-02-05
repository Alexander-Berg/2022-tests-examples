package ru.yandex.market.checkout.data.dto;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import androidx.annotation.NonNull;
import ru.yandex.market.testcase.JavaSerializationTestCase;
import ru.yandex.market.testcase.JsonSerializationTestCase;

@RunWith(Enclosed.class)
public class DeliveryIntervalDTOTest {

    public static class JsonSerializationTest extends JsonSerializationTestCase {

        @NonNull
        @Override
        protected Object getInstance() {
            return DeliveryIntervalDto.testBuilder()
                    .isDefault(true)
                    .timeTo("18:00:00")
                    .timeFrom("10:00:00")
                    .price(BigDecimal.valueOf(499))
                    .build();
        }

        @NonNull
        @Override
        protected Type getType() {
            return DeliveryIntervalDto.class;
        }

        @NonNull
        @Override
        protected JsonSource getJsonSource() {
            return file("DeliveryIntervalDTO.json");
        }
    }

    public static class JavaSerializationTest extends JavaSerializationTestCase {

        @NonNull
        @Override
        protected Object getInstance() {
            return DeliveryIntervalDto.testBuilder().build();
        }
    }
}
