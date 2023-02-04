package ru.yandex.partner.core.configuration;

import java.util.Collection;

import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.operation.container.OperationContainerConfigurer;

public class TestOperationContainerConfigurer<C> implements OperationContainerConfigurer<C> {
    private OperationContainerConfigurer<C> currentConfigurer;

    @Override
    public void configureContainer(C container, Collection<BaseBlock> preloadedModels) {
        if (currentConfigurer != null) {
            currentConfigurer.configureContainer(container, preloadedModels);
        }
    }

    @Override
    public Class getModelClass() {
        return BaseBlock.class;
    }

    public void setCurrentConfigurer(OperationContainerConfigurer<C> currentConfigurer) {
        this.currentConfigurer = currentConfigurer;
    }
}
