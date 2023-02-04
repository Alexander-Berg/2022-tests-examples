package ru.yandex.general.commonLkCases;

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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.LK_SIDEBAR;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(LK_SIDEBAR)
@DisplayName("Ссылки в сайдбаре на разных страницах ЛК")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class LkSidebarLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Мои объявления", OFFERS},
                {"Статистика", STATS},
                {"Избранное", FAVORITES},
                {"Автозагрузка", FEED},
                {"Настройки", CONTACTS}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Ссылки на странице «Мои объявления»")
    @DisplayName("Ссылки в сайдбаре на странице «Мои объявления»")
    public void shouldSeeLinksInMyOffersSidebar() {
        urlSteps.testing().path(MY).path(OFFERS).open();

        basePageSteps.onBasePage().lkSidebar().link(title).should(
                hasAttribute(HREF, urlSteps.testing().path(MY).path(path).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Ссылки на странице «Статистика»")
    @DisplayName("Ссылки в сайдбаре на странице «Статистика»")
    public void shouldSeeLinksInStatsSidebar() {
        urlSteps.testing().path(MY).path(STATS).open();

        basePageSteps.onBasePage().lkSidebar().link(title).should(
                hasAttribute(HREF, urlSteps.testing().path(MY).path(path).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Ссылки на странице «Избранное»")
    @DisplayName("Ссылки в сайдбаре на странице «Избранное»")
    public void shouldSeeLinksInFavoritesSidebar() {
        urlSteps.testing().path(MY).path(FAVORITES).open();

        basePageSteps.onBasePage().lkSidebar().link(title).should(
                hasAttribute(HREF, urlSteps.testing().path(MY).path(path).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Ссылки на странице «Автозагрузка»")
    @DisplayName("Ссылки в сайдбаре на странице «Автозагрузка»")
    public void shouldSeeLinksInFeedSidebar() {
        urlSteps.testing().path(MY).path(FEED).open();

        basePageSteps.onBasePage().lkSidebar().link(title).should(
                hasAttribute(HREF, urlSteps.testing().path(MY).path(path).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Ссылки на странице «Настройки»")
    @DisplayName("Ссылки в сайдбаре на странице «Настройки»")
    public void shouldSeeLinksInContactsSidebar() {
        urlSteps.testing().path(MY).path(CONTACTS).open();

        basePageSteps.onBasePage().lkSidebar().link(title).should(
                hasAttribute(HREF, urlSteps.testing().path(MY).path(path).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Ссылка текущей страницы задизейблена")
    @DisplayName("Ссылка текущей страницы задизейблена")
    public void shouldSeeDisabledActualPageLinkInSidebar() {
        urlSteps.testing().path(MY).path(path).open();

        basePageSteps.onBasePage().lkSidebar().link(title).should(
                hasAttribute("aria-disabled", "true"));
    }

}
