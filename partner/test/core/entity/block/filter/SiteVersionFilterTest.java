package ru.yandex.partner.core.entity.block.filter;

import java.util.List;

import org.jooq.Condition;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.filter.operator.FilterOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SiteVersionFilterTest {

    @Test
    void getCondition() {
        TableField<?, TestEnum> mockTableField = mock(TableField.class);
        SiteVersionFilter<BaseBlock, TestEnum> siteVersionFilter = new SiteVersionFilter<>("site_version",
                BaseBlock.class, TestEnum.class, mockTableField);

        Condition condition = siteVersionFilter.getCondition(FilterOperator.EQUALS, List.of(
                "invalid_value"));

        assertEquals(DSL.falseCondition(), condition);

        siteVersionFilter.getCondition(FilterOperator.NOT_EQUALS, List.of("invalid_value", "correct_value"));

        verify(mockTableField, times(1)).ne(TestEnum.correct_value);
    }

    private enum TestEnum {
        correct_value("correct_value");
        private final String name;

        TestEnum(String name) {
            this.name = name;
        }
    }
}
