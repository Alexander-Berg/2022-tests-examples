package ru.auto.tests.commons.htmlelements;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import io.qameta.atlas.core.api.Listener;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import org.hamcrest.Matcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * eroshenkoam
 * 23.03.17
 */
public class AllureListener implements Listener {

    private final Map<String, MethodFormat> loggableMethods;

    private final AllureLifecycle lifecycle = Allure.getLifecycle();

    public AllureListener() {
        loggableMethods = new HashMap<>();
        loggableMethods.put("click", (description, args) -> String.format("Кликаем на элемент \'%s\'", description));
        loggableMethods.put("submit", (description, args) -> String.format("Нажимаем на элемент \'%s\'", description));
        loggableMethods.put("clear", (description, args) -> String.format("Очищаем элемент \'%s\'", description));
        loggableMethods.put("sendKeys", (description, args) -> {
            String arguments = Arrays.toString(((CharSequence[]) args[0]));
            return String.format("Вводим в элемент \'%s\' значение [%s]", description, arguments);
        });
        loggableMethods.put("waitUntil", (description, args) -> {
            Matcher matcher = (Matcher) (args[0] instanceof Matcher ? args[0] : args[1]);
            return String.format("Ждем пока элемент \'%s\' будет в состоянии [%s]", description, matcher);
        });
        loggableMethods.put("should", (description, args) -> {
            Matcher matcher = (Matcher) (args[0] instanceof Matcher ? args[0] : args[1]);
            return String.format("Проверяем что элемент \'%s\' в состоянии [%s]", description, matcher);
        });
    }

    private Optional<MethodFormat> getStepTitle(MethodInfo method) {
        return method.getMethod().isDefault()
                ? Optional.empty()
                : Optional.ofNullable(loggableMethods.get(method.getMethod().getName()));
    }

    @Override
    public void beforeMethodCall(MethodInfo methodInfo, Configuration configuration) {
        getStepTitle(methodInfo).ifPresent(formatter ->
                lifecycle.startStep(UUID.randomUUID().toString(),
                        new StepResult()
                                .setName(formatter.format(methodInfo.getMethod().getName(), methodInfo.getArgs()))
                                .setStatus(Status.PASSED)
                ));
    }

    @Override
    public void afterMethodCall(MethodInfo methodInfo, Configuration configuration) {
        getStepTitle(methodInfo).ifPresent(title -> lifecycle.stopStep());
    }

    @Override
    public void onMethodReturn(MethodInfo methodInfo, Configuration configuration, Object o) {

    }

    @Override
    public void onMethodFailure(MethodInfo methodInfo, Configuration configuration, Throwable throwable) {
        getStepTitle(methodInfo).ifPresent(title ->
                lifecycle.updateStep(stepResult -> {
                    stepResult.setStatus(ResultsUtils.getStatus(throwable).orElse(Status.BROKEN));
                    stepResult.withStatusDetails(ResultsUtils.getStatusDetails(throwable).orElse(null));
                })
        );
    }

    @FunctionalInterface
    private interface MethodFormat {

        String format(String description, Object[] args);

    }

}
