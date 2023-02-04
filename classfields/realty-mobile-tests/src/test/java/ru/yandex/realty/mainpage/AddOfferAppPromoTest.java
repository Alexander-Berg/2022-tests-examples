package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Промо баннер добавления оффера через приложение")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class AddOfferAppPromoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка Загрузить приложение в модалке Опубликуйте оффер через приложение")
    public void shouldSeeDownloadAppUrl() {
        basePageSteps.onMobileMainPage().addOfferAppPromo().click();

        basePageSteps.onBasePage().modal().waitUntil(isDisplayed());
        basePageSteps.onBasePage().modal().link("Загрузить приложение").should(
                hasHref(equalTo("https://redirect.appmetrica.yandex.com/serve/744703322706954699")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка «Полная версия сайта» в модалке «Опубликуйте оффер через приложение»")
    public void shouldSeeFullSiteVersion() {
        basePageSteps.onMobileMainPage().addOfferAppPromo().click();
        basePageSteps.onBasePage().modal().waitUntil(isDisplayed());
        basePageSteps.onBasePage().modal().button("Полная версия сайта").click();

        assertThat("Проверяем соответствие URL",
                urlSteps.getCurrentUrl(), equalTo(urlSteps.testing().path("/management-new/add/").toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрытие модалки «Опубликуйте оффер через приложение»")
    public void shouldSeeClosedModal() {
        basePageSteps.onMobileMainPage().addOfferAppPromo().click();
        basePageSteps.onBasePage().modal().waitUntil(isDisplayed());
        basePageSteps.onBasePage().modal().close().click();

        basePageSteps.onBasePage().modal().should(not(isDisplayed()));
        basePageSteps.onMobileMainPage().searchFilters().should(isDisplayed());
    }

}
