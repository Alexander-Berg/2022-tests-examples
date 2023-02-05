package ru.yandex.market.net.disclaimer;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class DisclaimerDtoTest {

    @RunWith(Parameterized.class)
    public static class SuccessTest {

        @Parameterized.Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"someCode", "message", "message"},
                    {"someCode", "Возрастное ограничение", "Возрастное ограничение"},
                    {"Age", "Возрастное ограничение", "18+ Возрастное ограничение"},
                    {"age", "Возрастное ограничение", "18+ Возрастное ограничение"},
                    {"age", "Возрастное ограничение 18+", "18+ Возрастное ограничение"},
                    {"age", "Возрастное ограничение: 18+", "18+ Возрастное ограничение"},
                    {"age_6", "Возрастное ограничение", "6+ Возрастное ограничение"},
                    {"age_12", "Возрастное ограничение", "12+ Возрастное ограничение"},
                    {"age_18", "Возрастное ограничение", "18+ Возрастное ограничение"},
                    {"age_11", "Возрастное ограничение", "11+ Возрастное ограничение"},
                    {"Age_11", "Возрастное ограничение 11+", "11+ Возрастное ограничение"},
                    {"age_11", "Возрастное ограничение: 11+", "11+ Возрастное ограничение"},
                    {null, "Возрастное ограничение: 11+", "Возрастное ограничение: 11+"},
                    {"age_11", null, null},
                    {null, null, null},
            });
        }

        private String result;

        private DisclaimerDto disclaimerDto;

        public SuccessTest(String code, String message, String result) {
            this.result = result;
            disclaimerDto = DisclaimerDto.create(code, message);
        }

        @Test
        public void test() {
            assertEquals(disclaimerDto.getParsedMessage(), result);
        }
    }
}