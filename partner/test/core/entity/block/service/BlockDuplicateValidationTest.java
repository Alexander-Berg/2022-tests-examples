package ru.yandex.partner.core.entity.block.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.type.tags.TagService;
import ru.yandex.partner.core.service.msf.FormatSystemService;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class BlockDuplicateValidationTest {

    @Autowired
    BlockTypedRepository repository;

    @Autowired
    FormatSystemService formatSystemService;

    @Autowired
    BlockValidationService validationService;

    @Autowired
    private TagService tagService;


    @BeforeEach
    public void beforeEach() {
        //валидацию не прошла бы если бы режим был бы не Duplicate
        Mockito.when(tagService.getTagIds()).thenReturn(Set.of(1L, 2L, 3L, 4L, 5L, 6L));
    }

    @Test
    void validateCorrectBlock() {
        BaseBlock block = repository.getBlockByCompositeId(347649081345L);
        assertThat(block).isInstanceOf(RtbBlock.class);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validationService.validate(List.of(block),
                BlockContainerImpl.create(OperationMode.DUPLICATE));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos).isEmpty();
    }

}
