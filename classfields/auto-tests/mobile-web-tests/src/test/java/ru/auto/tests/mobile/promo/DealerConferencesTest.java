package ru.auto.tests.mobile.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CONFERENCES;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Промо - дилеры - конференции")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealerConferencesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DEALER).path(CONFERENCES).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказать мероприятие")
    public void shouldOrder() {
        basePageSteps.onPromoDealerPage().form().input("Имя", "Иван Иванов");
        basePageSteps.onPromoDealerPage().form().input("Телефон", getRandomPhone());
        basePageSteps.onPromoDealerPage().form().input("Электронная почта", getRandomEmail());
        basePageSteps.onPromoDealerPage().form().input("Название компании", "Спектр");
        basePageSteps.onPromoDealerPage().form().button("Оставить заявку").click();
        basePageSteps.onPromoDealerPage().successMessage()
                .waitUntil(hasText("Спасибо за заявку. В ближайшее время мы Вам перезвоним."));
    }
}
