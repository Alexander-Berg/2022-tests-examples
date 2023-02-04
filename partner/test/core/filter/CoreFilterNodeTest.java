package ru.yandex.partner.core.filter;

import java.util.Collections;
import java.util.List;

import org.jooq.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.filter.operator.BinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreFilterNodeTest {

    @Test
    void constructor() {
        assertThrows(NullPointerException.class,
                () -> new CoreFilterNode<>(null, List.of(mock(CoreFilterNode.class))));
        assertThrows(NullPointerException.class,
                () -> new CoreFilterNode<>(BinaryOperator.AND, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CoreFilterNode<>(BinaryOperator.AND, Collections.emptyList()));
        assertThrows(NullPointerException.class,
                () -> new CoreFilterNode<>(null));
    }

    @Test
    void toCondition() {
        CoreFilter<Object> mockCoreFilter1 = mock(CoreFilter.class);
        Condition mockCondition1 = mock(Condition.class);
        doReturn(mockCondition1).when(mockCoreFilter1).toCondition(any(), any());

        CoreFilterNode<Object> coreFilterNode = new CoreFilterNode<>(mockCoreFilter1);
        assertEquals(mockCondition1, coreFilterNode.toCondition(any(), any()));

        CoreFilter<Object> mockCoreFilter2 = mock(CoreFilter.class);
        Condition mockCondition2 = mock(Condition.class);
        doReturn(mockCondition2).when(mockCoreFilter2).toCondition(any(), any());

        coreFilterNode = new CoreFilterNode<>(BinaryOperator.AND,
                List.of(
                        new CoreFilterNode<>(mockCoreFilter1),
                        new CoreFilterNode<>(mockCoreFilter2)
                ));

        Condition mockCondition3 = mock(Condition.class);
        doReturn(mockCondition3).when(mockCondition1).and(mockCondition2);

        assertEquals(mockCondition3, coreFilterNode.toCondition(any(), any()));
    }

    @Test
    void addFilterNode() {
        CoreFilter<Object> mockCoreFilter1 = mock(CoreFilter.class);
        Condition mockCondition1 = mock(Condition.class);
        doReturn(mockCondition1).when(mockCoreFilter1).toCondition(any(), any());
        CoreFilter<Object> mockCoreFilter2 = mock(CoreFilter.class);
        Condition mockCondition2 = mock(Condition.class);
        doReturn(mockCondition2).when(mockCoreFilter2).toCondition(any(), any());
        CoreFilter<Object> mockCoreFilter3 = mock(CoreFilter.class);
        Condition mockCondition3 = mock(Condition.class);
        doReturn(mockCondition3).when(mockCoreFilter3).toCondition(any(), any());


        CoreFilterNode<Object> coreFilterNode = new CoreFilterNode<>(BinaryOperator.AND,
                List.of(
                        new CoreFilterNode<>(mockCoreFilter1),
                        new CoreFilterNode<>(mockCoreFilter2)
                ));

        coreFilterNode = coreFilterNode.addFilterNode(BinaryOperator.OR, new CoreFilterNode<>(mockCoreFilter3));

        Condition mockCondition4 = mock(Condition.class);
        doReturn(mockCondition4).when(mockCondition1).and(mockCondition2);

        Condition mockCondition5 = mock(Condition.class);
        doReturn(mockCondition5).when(mockCondition4).or(mockCondition3);

        assertEquals(mockCondition5, coreFilterNode.toCondition(any(), any()));
    }

    @Test
    void neutral() {
        CoreFilter<Object> mockCoreFilter1 = mock(CoreFilter.class);
        Condition mockCondition1 = mock(Condition.class);
        when(mockCoreFilter1.toCondition(any(), any())).thenReturn(mockCondition1);

        CoreFilterNode<Object> coreFilterNode = CoreFilterNode.neutral()
                .and(new CoreFilterNode<>(mockCoreFilter1));

        assertEquals(coreFilterNode.toCondition(any(), any()), mockCondition1);
    }

    @Test
    void toStringBasicTest() {
        String expected = "((page_id IN [123, 456] AND multistate = [2]) AND owner = [login = [avonav]])";
        var filter = PageFilters.PAGE_ID.in(List.of(123L, 456L))
                .and(PageFilters.MULTISTATE.eq(2L))
                .and(PageFilters.OWNER.eq(UserFilters.LOGIN.eq("avonav")));

        Assertions.assertEquals(expected, filter.toString());
    }
}
