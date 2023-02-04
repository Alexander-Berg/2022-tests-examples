package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SAVE_SEARCHES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.SEARCHES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поп-ап про подтверждение/удаление подписки")
@Feature(SAVE_SEARCHES)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubscriptionPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String paramName;

    @Parameterized.Parameter(1)
    public String paramValue;

    @Parameterized.Parameter(2)
    public String text;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"subs_confirm_popup", "true",
                        "Поиск сохранён\nНа адресбудут отправляться письма со свежими объявлениями.\nПосмотреть сохранённые поиски"},
                {"subs_confirm_popup", "false", "Произошла ошибка, попробуйте снова."},
                {"subs_delete_popup", "true",
                        "Подписка поставлена в очередь на удаление.\nПисьма со свежими объявлениями перестанут приходить в ближайшее время."},
                {"subs_delete_popup", "false", "Произошла ошибка, попробуйте снова."}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(LIKE).path(SEARCHES).addParam("show-searches", "true")
                .addParam(paramName, paramValue).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeePopup() {
        basePageSteps.onSearchesPage().popup().waitUntil(isDisplayed()).should(hasText(text));
    }
}
