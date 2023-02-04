package ru.yandex.general.feed;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.FeedSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Button.ARIA_DISABLED;
import static ru.yandex.general.element.Button.TRUE;
import static ru.yandex.general.page.FeedPage.FEED_URL_INPUT;
import static ru.yandex.general.page.FeedPage.SEND;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Проверка валидации инпута ссылки на фид")
@DisplayName("Проверка валидации инпута ссылки на фид, негативные кейсы")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeedLinkInputValidationErrorTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FeedSteps feedSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "{index}. Ссылка «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"my-files.su/Save/nspznb/general_test_feed.xml"},
                {"https://my-files.s"},
                {"192.168.1.1/general_test_feed.xml"},
                {"my-files"}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FEED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ошибка валидации в инпуте ссылки на фид")
    public void shouldSeeFeedUrlInputValidationError() {
        feedSteps.onFeedPage().input(FEED_URL_INPUT).sendKeys(url);

        feedSteps.onFeedPage().inputHint("URL введен неверно").should(isDisplayed());
        feedSteps.onFeedPage().button(SEND).should(hasAttribute(ARIA_DISABLED, TRUE));
    }
}
