package ru.auto.tests.desktop.step.cabinet;

import io.qameta.allure.Step;
import ru.auto.tests.desktop.step.BasePageSteps;

import java.util.concurrent.TimeUnit;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 14.09.18
 */
public class AgencyCabinetPagesSteps extends BasePageSteps {

    public static final String ACTIVE_FILTER_CLASS_NAME = "ClientsFiltersItem_active";

    @Step("Ждём, пока страница полностью загрузится")
    public void waitUntilPageIsFullyLoaded() {
        waitSomething(3, TimeUnit.SECONDS);
    }
}
