package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(VIN)
@DisplayName("Покупка отчёта под зарегом")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistorySearchErrorTest {

    private static final String VIN = "4S2CK58D924333406";
    private static final String ERROR_VIN = "4S2CK58D92433340";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Ввод некорректного VIN на промо странице")
    public void shouldSearchIncorrectVinInPromo() {
        urlSteps.testing().path(HISTORY).open();
        basePageSteps.onHistoryPage().topBlock().input("Госномер или VIN", ERROR_VIN);
        basePageSteps.onHistoryPage().topBlock().button("Проверить").click();
        urlSteps.path(SLASH).shouldNotSeeDiff();
        basePageSteps.onHistoryPage().topBlock().error().should(hasText("Введите правильный " +
                "VIN/госномер. Подробнее"));
        basePageSteps.onHistoryPage().topBlock().error().errorHelp().click();
        basePageSteps.onHistoryPage().popup().waitUntil(isDisplayed()).should(hasText("Где смотреть\nГосномер вы " +
                "найдёте в Свидетельстве о регистрации (СТС) в строке «Регистрационный знак», а VIN в строке " +
                "«Идентификационный номер».\nТакже эти данные можно найти в Паспорте транспортного средства (ПТС) и " +
                "полисе ОСАГО."
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Ввод некорректного VIN на странице отчета")
    public void shouldSearchIncorrectVinInHistory() {
        urlSteps.testing().path(HISTORY).path(VIN).open();
        basePageSteps.onHistoryPage().sidebar().input("Госномер или VIN", ERROR_VIN);
        basePageSteps.onHistoryPage().sidebar().button("Проверить").click();
        urlSteps.path(SLASH).shouldNotSeeDiff();
        basePageSteps.onHistoryPage().sidebar().error().waitUntil(isDisplayed()).should(hasText("Введите правильный " +
                "VIN/госномер. Подробнее"));
        basePageSteps.onHistoryPage().sidebar().error().errorHelp().click();
        basePageSteps.onHistoryPage().popup().waitUntil(isDisplayed()).should(hasText("Где смотреть\nГосномер вы " +
                "найдёте в Свидетельстве о регистрации (СТС) в строке «Регистрационный знак», а VIN в строке " +
                "«Идентификационный номер».\nТакже эти данные можно найти в Паспорте транспортного средства (ПТС) и " +
                "полисе ОСАГО."));
    }
}
