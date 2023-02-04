package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithAdfox;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;
import ru.yandex.partner.core.entity.block.service.BlockUpdateOperationFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class BlockWithAdfoxUpdateValidationTest extends BaseValidationTest {
    @Autowired
    BlockUpdateOperationFactory blockUpdateOperationFactory;

    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateOptionalAdfoxBlock() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        List<ModelChanges<RtbBlock>> modelChanges =
                List.of(ModelChanges.build(347649081345L, RtbBlock.class,
                        BlockWithAdfox.ADFOX_BLOCK, null));

        List<ModelChanges<BaseBlock>> modelChangesList =
                modelChanges.stream().map(mc -> mc.castModel(BaseBlock.class)).collect(Collectors.toList());

        Optional<MassResult<Long>> result = blockUpdateOperationFactory.createUpdateOperationWithPreloadedModels(
                        Applicability.PARTIAL,
                        modelChangesList,
                        new IncomingFields(),
                        null,
                id -> new RtbBlock().withId(id)
                )
                .prepare();

        // Если валидация не выявила ошибок - result должен быть пустым
        assertThat(result).isEmpty();
    }
}
