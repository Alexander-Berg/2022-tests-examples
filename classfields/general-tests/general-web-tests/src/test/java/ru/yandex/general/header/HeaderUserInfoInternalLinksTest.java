package ru.yandex.general.header;

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
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.HEADER_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(HEADER_FEATURE)
@Feature("Ссылки в попапе «Юзер инфо»")
@DisplayName("Ссылки в попапе «Юзер инфо» на внутренние странички")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderUserInfoInternalLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Parameterized.Parameter
    public String linkName;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Ссылка «{0}»")
    public static Collection<Object[]> links() {
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
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в попапе «Юзер инфо» на внутренние странички")
    public void shouldSeeHeaderInternalLinks() {
        basePageSteps.onListingPage().header().avatar().click();

        basePageSteps.onListingPage().userInfoPopup().link(linkName).should(
                hasAttribute(HREF, urlSteps.path(MY).path(path).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки в попапе «Юзер инфо» из прилипшего хэдера на внутренние странички")
    public void shouldSeeFloatedHeaderInternalLinks() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().avatar().click();

        basePageSteps.onListingPage().userInfoPopup().link(linkName).should(
                hasAttribute(HREF, urlSteps.path(MY).path(path).toString()));
    }

}
