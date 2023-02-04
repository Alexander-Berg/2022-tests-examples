package ru.yandex.partner.core.entity.block.service;


import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.type.tags.TagService;

public class BaseValidationTest {

    @Autowired
    BlockValidationService validationService;

    @Autowired
    private TagService tagService;

    private final OperationMode mode;
    private BlockContainerImpl container;

    public BaseValidationTest() {
        this.mode = OperationMode.CRON;
    }

    public BaseValidationTest(OperationMode mode) {
        this.mode = mode;
    }

    @BeforeEach
    public void beforeEach() {
        container = BlockContainerImpl.create(mode);
        Mockito.when(tagService.getTagIds()).thenReturn(Set.of(1L, 2L, 3L, 4L, 5L, 6L));
    }

    @AfterEach
    public void afterEach() {
        Mockito.reset(tagService);
    }

    public BlockContainerImpl getContainer() {
        return container;
    }

    public ValidationResult<List<? extends BaseBlock>, Defect> validate(List<BaseBlock> models) {
        return validationService.validate(models, getContainer());
    }

}
