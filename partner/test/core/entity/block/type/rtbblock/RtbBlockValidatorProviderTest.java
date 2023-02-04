package ru.yandex.partner.core.entity.block.type.rtbblock;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.type.base.PageReachabilityValidator;
import ru.yandex.partner.core.entity.page.model.ContextPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CoreTest
class RtbBlockValidatorProviderTest {
    @Autowired
    PageReachabilityValidator pageReachabilityValidator;

    @Test
    public void pageReachability_success() {
        var result = pageReachabilityValidator.validator(
                Map.of(1L, new ContextPage())
        ).apply(new RtbBlock().withPageId(1L));

        var errors = getErrorFields(result.getSubResults());

        assertEquals(
                Set.of(),
                errors
        );
    }

    @Test
    public void pageReachability_noPages() {
        var result = pageReachabilityValidator.validator(
                Map.of()
        ).apply(new RtbBlock().withPageId(1L));

        var errors = getErrorFields(result.getSubResults());

        assertEquals(
                Set.of(
                        new PathNode.Field(BaseBlock.PAGE_ID.name())
                ),
                errors
        );
    }

    private Set<PathNode> getErrorFields(Map<PathNode, ValidationResult<?, Defect>> subResult) {
        return subResult.entrySet().stream()
                .filter(entry -> entry.getValue().hasErrors())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
