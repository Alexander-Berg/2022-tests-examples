package ru.yandex.general.feed;

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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.page.FeedPage.CARS;
import static ru.yandex.general.page.FeedPage.HELP;
import static ru.yandex.general.page.FeedPage.REALTY;
import static ru.yandex.general.page.FeedPage.TERMS;
import static ru.yandex.general.page.FeedPage.XML;
import static ru.yandex.general.page.FeedPage.YMARKET_FOR_BUSINESS;
import static ru.yandex.general.page.FeedPage.YML;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(FEEDS_FEATURE)
@Feature("Проверка ссылок")
@DisplayName("Проверка ссылок")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedLinksTest {

    private static final String XML_LINK = "https://yandex.ru/support/o-desktop/price-list-requirements.html";
    private static final String YML_LINK = "https://yandex.ru/support/o-desktop/price-list-requirements-yml.html";
    private static final String HELP_LINK = "https://yandex.ru/support/o-desktop/price-list-rules.html";
    private static final String FOR_BUSINESS_LINK = "https://yandex.ru/support/market-cms/";
    private static final String TERMS_LINK = "https://yandex.ru/legal/classified_termsofuse";
    private static final String CARS_LINK = "https://yandex.ru/support/autoru-legal/price-list.html";
    private static final String REALTY_LINK = "https://yandex.ru/support/realty/rules/content-requirements.html";

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
        urlSteps.testing().path(MY).path(FEED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «XML» в блоке информации")
    public void shouldSeeXmlLink() {
        basePageSteps.onFeedPage().info().link(XML).should(hasAttribute(HREF, XML_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «YML» в блоке информации")
    public void shouldSeeYmlLink() {
        basePageSteps.onFeedPage().info().link(YML).should(hasAttribute(HREF, YML_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на справку в блоке информации")
    public void shouldSeeHelpLink() {
        basePageSteps.onFeedPage().info().link(HELP).should(hasAttribute(HREF, HELP_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Яндекс.Маркет для бизнеса» в блоке информации")
    public void shouldSeeForBusinessLink() {
        basePageSteps.onFeedPage().info().link(YMARKET_FOR_BUSINESS).should(hasAttribute(HREF, FOR_BUSINESS_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на условия")
    public void shouldSeeTermsLink() {
        basePageSteps.onFeedPage().link(TERMS).should(hasAttribute(HREF, TERMS_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Автомобили»")
    public void shouldSeeCarsLink() {
        basePageSteps.onFeedPage().link(CARS).should(hasAttribute(HREF, CARS_LINK));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на «Недвижимость»")
    public void shouldSeeRealtyLink() {
        basePageSteps.onFeedPage().link(REALTY).should(hasAttribute(HREF, REALTY_LINK));
    }

}
