package ru.yandex.partner.core.utils;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.partner.core.action.exception.DefectInfoWithMsgId;
import ru.yandex.partner.core.action.exception.DefectInfoWithMsgParams;
import ru.yandex.partner.core.validation.defects.DefectInfoBuilder;
import ru.yandex.partner.core.validation.defects.presentation.CommonValidationMsg;

class DefectInfoBuilderTest {

    @Test
    void defectInfoBuilderTest() {
        var path = new Path(List.of(
                new PathNode.Field("test_model"),
                new PathNode.Field("attribute3"),
                new PathNode.Field("id")
        ));
        var value = "12345";

        var constructorVariable = new DefectInfo<>(
                path,
                value,
                new Defect(DefectInfoWithMsgId.ID,
                        new DefectInfoWithMsgParams(CommonValidationMsg.DATA_MUST_BE_INTEGER_NUMBER))
        );

        var builderVariable = DefectInfoBuilder.of(CommonValidationMsg.DATA_MUST_BE_INTEGER_NUMBER)
                .withPath(path)
                .withValue(value)
                .build();
        Assertions.assertEquals(constructorVariable, builderVariable);
    }
}
