package ru.auto.tests.commons.extension;

import io.qameta.atlas.core.api.MethodExtension;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import ru.auto.tests.commons.extension.context.ElementInfo;
import ru.auto.tests.commons.extension.context.StepContext;
import ru.auto.tests.commons.extension.interfaces.ListElement;

import java.lang.reflect.Method;
import java.util.List;

public class ListElementExtension implements MethodExtension {

    @Override
    public boolean test(Method method) {
        return method.isAnnotationPresent(ListElement.class);
    }

    @Override
    public Object invoke(Object proxy,
                         MethodInfo methodInfo,
                         Configuration configuration) throws Throwable {
        assert proxy instanceof List;
        int position = (int) methodInfo.getArgs()[0];

        configuration.getContext(StepContext.class)
                .ifPresent(findByContext -> findByContext.addElement(new ElementInfo(
                        String.format("[%s]", position),
                        String.format("Элемент[%s]", position))
                ));

        return ((List) proxy).get(position);
    }
}
