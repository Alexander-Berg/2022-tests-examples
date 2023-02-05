package ru.yandex.market.clean.data.mapper;

import com.annimon.stream.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import ru.yandex.market.domain.discount.model.DiscountType;
import ru.yandex.market.clean.data.mapper.discount.DiscountTypeMapper;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class DiscountTypeMapperTest {

    @Parameterized.Parameter
    public String input;

    @Parameterized.Parameter(1)
    public Optional<DiscountType> expectedOutput;

    private DiscountTypeMapper mapper;

    @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {DiscountType.PRIME.toString(), Optional.of(DiscountType.PRIME)},
                {DiscountType.PRIME.toString().toLowerCase(), Optional.of(DiscountType.PRIME)},
                {DiscountType.YANDEX_PLUS.toString(), Optional.of(DiscountType.YANDEX_PLUS)},
                {DiscountType.YANDEX_PLUS.toString().toLowerCase(),
                        Optional.of(DiscountType.YANDEX_PLUS)},
                {DiscountType.THRESHOLD.toString(), Optional.of(DiscountType.THRESHOLD)},
                {DiscountType.THRESHOLD.toString().toLowerCase(),
                        Optional.of(DiscountType.THRESHOLD)},
                {DiscountType.UNKNOWN.toString(), Optional.of(DiscountType.UNKNOWN)},
                {DiscountType.UNKNOWN.toString().toLowerCase(), Optional.of(DiscountType.UNKNOWN)},
                {"abc", Optional.of(DiscountType.UNKNOWN)},
                {"", Optional.of(DiscountType.NONE)},
                {null, Optional.of(DiscountType.NONE)}
        });
    }

    @Before
    public void setUp() {
        mapper = new DiscountTypeMapper();
    }

    @Test
    public void testMap() {
        final Optional<DiscountType> actualOutput = mapper.map(input);
        assertThat(actualOutput, equalTo(expectedOutput));
    }
}