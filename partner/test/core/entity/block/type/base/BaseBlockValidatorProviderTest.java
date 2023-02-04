package ru.yandex.partner.core.entity.block.type.base;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.multistate.block.BlockMultistate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseBlockValidatorProviderTest {

    private BaseBlockValidatorProvider baseBlockValidatorProvider = new BaseBlockValidatorProvider();

    @Test
    void baseBlockValidatorError1() {
        var result = baseBlockValidatorProvider.validator().apply(new RtbBlock());

        var errors = getErrorFields(result.getSubResults());

        assertEquals(
                Set.of(
                        new PathNode.Field(BaseBlock.PAGE_ID.name()),
                        new PathNode.Field(BaseBlock.BLOCK_ID.name())
                ),
                errors
        );
    }

    @Test
    void baseBlockValidatorError2() {
        var result =
                baseBlockValidatorProvider.validator().apply(new RtbBlock().withPageId(1L).withBlockId(1L));

        var errors = getErrorFields(result.getSubResults());

        assertEquals(
                Set.of(
                        new PathNode.Field(BaseBlock.ID.name())
                ),
                errors
        );
    }

    @Test
    void baseBlockValidatorError3() {
        var result = baseBlockValidatorProvider.validator().apply(new RtbBlock()
                .withPageId(1L)
                .withBlockId(1L)
                .withId(0L)
        );

        var errors = getErrorFields(result.getSubResults());

        assertEquals(
                Set.of(
                        new PathNode.Field(BaseBlock.ID.name())
                ),
                errors
        );
    }

    @Test
    void baseBlockValidatorSuccess() {
        var result = baseBlockValidatorProvider.validator().apply(new RtbBlock()
                .withPageId(111L)
                .withBlockId(11L)
                .withId(931135499L)
                .withMultistate(new BlockMultistate(0))
        );

        var errors = getErrorFields(result.getSubResults());

        assertEquals(
                Set.of(),
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
