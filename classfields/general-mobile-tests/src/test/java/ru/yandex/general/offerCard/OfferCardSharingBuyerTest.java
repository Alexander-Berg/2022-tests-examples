package ru.yandex.general.offerCard;

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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(OFFER_CARD_FEATURE)
@Feature("Шаринг")
@DisplayName("Проверка ссылок шаринга на карточке")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardSharingBuyerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String shareService;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"vkontakte", "https://vk.com/share.php"},
                {"telegram", "https://t.me/share/"},
                {"facebook", "https://www.facebook.com/sharer.php"},
                {"twitter", "https://twitter.com/intent/tweet"},
                {"odnoklassniki", "https://connect.ok.ru/offer"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().snippetFirst().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка ссылок шаринга на карточке покупателем")
    public void shouldSeeCardOfferShareUrls() {
        basePageSteps.scrollingToElement(basePageSteps.onOfferCardPage().footer());
        basePageSteps.onOfferCardPage().shareButton().click();

        basePageSteps.onOfferCardPage().shareService(shareService).link().should(
                hasAttribute(HREF, allOf(
                        containsString(url),
                        containsString(encode(urlSteps.getCurrentUrl())))));
    }

}
