package ru.yandex.realty.newbuilding.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@Issue("VERTISTEST-1350")
@DisplayName("Карточка новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SubscribePromoTest {

    public static final String PATH_TO_ZH_K_WITHOUT_OFFERS = "/oktyabrskoe-pole/";
    public static final String ZH_K_WITHOUT_OFFERS = "189856";
    public static final String SUBSCRIBE = "Подписаться";
    public static final String EMAIL = "Электронная почта";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписка на новые объявления")
    public void shouldSeeSubscribeOnNewOffers() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(PATH_TO_ZH_K_WITHOUT_OFFERS)
                .queryParam("id", ZH_K_WITHOUT_OFFERS).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().button("Оставить заявку"));
        basePageSteps.scroll(300);
        basePageSteps.onNewBuildingCardPage().button("Оставить заявку").click();
        basePageSteps.onNewBuildingCardPage().surveyForm().input()
                .sendKeys(format("+%s", getRandomPhone()));
        basePageSteps.onNewBuildingCardPage().surveyForm().button("Отправить заявку").click();
        basePageSteps.onNewBuildingCardPage().surveyForm().should(hasText(containsString("Заявка отправлена!")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Подписка на акции")
    public void shouldSeeSubscribeOnPromo() {
        mockRuleConfigurable.siteWithOffersStatStub(mockSiteWithOffersStatTemplate().build()).createWithDefaults();
        urlSteps.testing().newbuildingSiteMobile().open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onNewBuildingCardPage().subscribeBlock().input(EMAIL));
        basePageSteps.onNewBuildingCardPage().subscribeBlock().button(SUBSCRIBE).waitUntil(isDisabled());
        basePageSteps.onNewBuildingCardPage().subscribeBlock().input(EMAIL, getRandomEmail());
        basePageSteps.onNewBuildingCardPage().subscribeBlock().button(SUBSCRIBE).waitUntil(not(isDisabled())).click();
        basePageSteps.onNewBuildingCardPage().subscribeBlock().should(hasText(containsString("Подписка оформлена")));
    }
}

