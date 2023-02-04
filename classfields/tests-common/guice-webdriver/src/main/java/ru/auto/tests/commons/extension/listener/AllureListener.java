package ru.auto.tests.commons.extension.listener;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ResultsUtils;
import io.qameta.atlas.core.api.Listener;
import io.qameta.atlas.core.internal.Configuration;
import io.qameta.atlas.core.util.MethodInfo;
import io.qameta.atlas.webdriver.context.WebDriverContext;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.commons.extension.context.StepContext;
import ru.auto.tests.commons.extension.interfaces.MethodFormat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Accessors(chain = true)
public class AllureListener implements Listener {

    @Getter
    @Setter
    private List<StepContext> locatorsList;

    private final Map<String, MethodFormat> loggableMethods;

    private final AllureLifecycle lifecycle = Allure.getLifecycle();

    public AllureListener() {
        loggableMethods = StepTitleContainer.loggableMethods;
    }

    private Optional<MethodFormat> getStepTitle(MethodInfo method) {
        return method.getMethod().isDefault()
                ? Optional.empty()
                : Optional.ofNullable(loggableMethods.get(method.getMethod().getName()));
    }

    @Override
    public void beforeMethodCall(MethodInfo methodInfo, Configuration configuration) {
        if (isSupported(methodInfo)) {
            Optional<WebDriverContext> driverContext = configuration.getContext(WebDriverContext.class);

            if (driverContext.isPresent()) {
                String url = driverContext.get().getValue().getCurrentUrl();
//                String session = ((RemoteWebDriver) driverContext.get().getValue()).getSessionId().toString();
                configuration.getContext(StepContext.class)
                        .ifPresent(stepContext -> stepContext
                                .setAction(methodInfo.getMethod().getName())
                                .setArgs(methodInfo.getArgs())
                                .setUrl(url)
                                .currentContext());
            }
        }

        String name = methodInfo.getMethod().getName();
        if (configuration.getContext(StepContext.class).isPresent()) {
            name = configuration.getContext(StepContext.class).get().name();
        }
        String finalName = name;
        getStepTitle(methodInfo).ifPresent(formatter ->
                lifecycle.startStep(UUID.randomUUID().toString(),
                        new StepResult()
                                .setName(formatter.format(finalName, methodInfo.getArgs()))
                                .setStatus(Status.PASSED)
                ));
    }

    @Override
    public void afterMethodCall(MethodInfo methodInfo, Configuration configuration) {
        if (isSupported(methodInfo)) {
            configuration.getContext(StepContext.class)
                    .ifPresent(stepContext -> {
                        locatorsList.add(stepContext.copy());
                    });
        }

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

    private boolean isSupported(MethodInfo methodInfo) {
        String name = methodInfo.getMethod().getName();
        return name.equals("click")
                || name.equals("waitUntil")
                || name.equals("should")
                || name.equals("sendKeys")
                || name.equals("clear")
                || name.equals("getText")
                || name.equals("hover");
    }

}
