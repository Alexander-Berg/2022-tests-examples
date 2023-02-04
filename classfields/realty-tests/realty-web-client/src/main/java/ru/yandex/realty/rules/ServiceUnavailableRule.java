package ru.yandex.realty.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.realty.step.BasePageSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.page.BasePage.PAGE_NOT_FOUND;
import static ru.yandex.realty.page.BasePage.SERVICE_UNAVAILABLE;

public class ServiceUnavailableRule extends TestWatcher {

    @Inject
    private BasePageSteps basePageSteps;

    @Override
    @Step("Проверяем на отсутствие «Произошла ошибка. Сервис недоступен» ")
    protected void failed(Throwable e, Description description) throws RuntimeException {
        if (exists().matches(basePageSteps.onBasePage().errorPage(SERVICE_UNAVAILABLE))) {
            throw new RuntimeException("Произошла ошибка. Сервис недоступен", e);
        }
        if (exists().matches(basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND))) {
            throw new RuntimeException("Страница не найдена", e);
        }
    }
}
