package ru.yandex.general.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.HEADER_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.element.Header.HELP;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(HEADER_FEATURE)
@Feature("Ссылки в попапе «Юзер инфо»")
@DisplayName("Ссылки в попапе «Юзер инфо» на внешние странички")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class HeaderUserInfoExternalLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в попапе «Юзер инфо» на «Помощь»")
    public void shouldSeeUserInfoHelpLink() {
        basePageSteps.onListingPage().header().avatar().click();

        basePageSteps.onListingPage().userInfoPopup().link(HELP).should(
                hasAttribute(HREF, "https://yandex.ru/support/o-desktop/"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в попапе «Юзер инфо» на паспорт")
    public void shouldSeePassportLink() {
        basePageSteps.onListingPage().header().avatar().click();

        basePageSteps.onListingPage().userInfoPopup().link("Vasily Pupkin").should(
                hasAttribute(HREF, urlSteps.getPassportUrl() + "profile"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Помощь» в попапе «Юзер инфо» из прилипшего хэдера")
    public void shouldSeeUserInfoHelpLinkFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().avatar().click();

        basePageSteps.onListingPage().userInfoPopup().link(HELP).should(
                hasAttribute(HREF, "https://yandex.ru/support/o-desktop/"));
    }

}
