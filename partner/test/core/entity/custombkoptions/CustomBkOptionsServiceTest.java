package ru.yandex.partner.core.entity.custombkoptions;

import java.util.List;

import com.fasterxml.jackson.databind.node.LongNode;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.custombkoptions.filter.CustomBkOptionsFilters;
import ru.yandex.partner.core.entity.custombkoptions.model.CustomBkOptions;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.operator.FilterOperator;
import ru.yandex.partner.core.multistate.custombkoptions.CustomBkOptionsStateFlag;

import static java.util.function.Predicate.not;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;

@CoreTest
class CustomBkOptionsServiceTest {

    @Autowired
    CustomBkOptionsTypedRepository bkOptionsTypedRepository;

    @Autowired
    CustomBkOptionsService customBkOptionsService;

    @Test
    void setOptionTest() throws JSONException {
        String customBlockData = null;

        List<CustomBkOptions> options = bkOptionsTypedRepository.getAll(QueryOpts.forClass(CustomBkOptions.class)
                .withProps(CustomBkOptions.BK_NAME, CustomBkOptions.BK_VALUE)
                .withFilter(CoreFilterNode.in(CustomBkOptionsFilters.ID, List.of(1L, 2L, 19L)))
                .withFilter(CoreFilterNode.create(
                        CustomBkOptionsFilters.MULTISTATE,
                        FilterOperator.IN,
                        not(has(CustomBkOptionsStateFlag.DELETED)))
                )
        );


        for (var option : options) {
            customBlockData = customBkOptionsService.setOption(customBlockData, option);
        }

        JSONAssert.assertEquals(customBlockData, "{\"CustomOptions\":{\"test\":\"test\",\"test2\":\"test2\"," +
                "\"TestGroup\":{\"Child1\":\"test_group_child_1\"}}}", false);
    }

    @Test
    void setTest() throws JSONException {
        String customBlockData = null;

        customBlockData = customBkOptionsService.set(customBlockData, "CustomOptions.only-portal-trusted-banners",
                LongNode.valueOf(1L)
        );

        JSONAssert.assertEquals(customBlockData, "{\"CustomOptions\":{\"only-portal-trusted-banners\":1}}", false);
    }
}
