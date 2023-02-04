package ru.yandex.general.adult;

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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FOR_ADULTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.ADULT_CONFIRMED;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FOR_ADULTS_FEATURE)
@Feature("Сниппет «Только для взрослых», блок похожих наверху карточки")
@DisplayName("Сниппет «Только для взрослых» в блоке похожих сверху")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class AdultSnippetInSimilarTopBlockOfferCardTest {

    private static final String CARD_ID = "12345";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        basePageSteps.setMoscowCookie();

        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setCategoryForAdults(true),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(CARD).path(CARD_ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается иконка «18+» на оффере из блока похожих на карточке сверху")
    public void shouldSeeAdultIconTopSimilarCardSnippet() {
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();

        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).adultAgeIcon().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается иконка «18+» на оффере из блока похожих на карточке сверху с кукой")
    public void shouldNotSeeAdultIconTopSimilarCardSnippetWithCookie() {
        basePageSteps.setCookie(ADULT_CONFIRMED, TRUE);

        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();

        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).adultAgeIcon().should(not(isDisplayed()));
    }

}
