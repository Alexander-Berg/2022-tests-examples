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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.element.Image.SRC;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature("Блок информации о продавце")
@DisplayName("Блок информации о продавце")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardSellerInfoTest {

    private static final String ID = "12345";
    private static final String PROFILE_URL = "/profile/y3tua2d57ty041k24k12412jw/";
    private static final String NAME = "Снупп Догг";
    private static final int OFFERS_COUNT = 29;
    private static final String AVATAR_IMG = "https://avatars.mdst.yandex.net/get-yapic/1450/wSPcK5KpK2UKWEnj2Al8V1h88-1/";
    private static final String DUMMY_IMG = "https://avatars.mds.yandex.net/get-yapic/0/0-0/";

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public boolean isOwner;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Продавец", true},
                {"Покупатель", false}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на страницу продавца для продавца/покупателя")
    public void shouldSeeSellerLink() {
        mockRule.graphqlStub(mockResponse().setCategoriesTemplate().setRegionsTemplate()
                .setCard(mockCard(BASIC_CARD).setIsOwner(isOwner).setPublicProfileUrl(PROFILE_URL).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().sellerInfo().link().should(
                hasAttribute(HREF, urlSteps.testing().path(PROFILE_URL).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Имя продавца для продавца/покупателя")
    public void shouldSeeSellerName() {
        mockRule.graphqlStub(mockResponse().setCategoriesTemplate().setRegionsTemplate()
                .setCard(mockCard(BASIC_CARD).setIsOwner(isOwner).setSellerName(NAME).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().sellerInfo().name().should(hasText(NAME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во объявлений для продавца/покупателя")
    public void shouldSeeSellerOfferCount() {
        mockRule.graphqlStub(mockResponse().setCategoriesTemplate().setRegionsTemplate()
                .setCard(mockCard(BASIC_CARD).setIsOwner(isOwner).setSellerActiveOffersCount(OFFERS_COUNT).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().sellerInfo().offersCount().should(
                hasText(format("%s объявлений", OFFERS_COUNT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Аватар для продавца/покупателя")
    public void shouldSeeSellerAvatar() {
        mockRule.graphqlStub(mockResponse().setCategoriesTemplate().setRegionsTemplate()
                .setCard(mockCard(BASIC_CARD).setIsOwner(isOwner).setAvatar().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().sellerInfo().image().should(hasAttribute(SRC, containsString(AVATAR_IMG)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Заглушка аватара для продавца/покупателя")
    public void shouldSeeSellerAvatarDummyImg() {
        mockRule.graphqlStub(mockResponse().setCategoriesTemplate().setRegionsTemplate()
                .setCard(mockCard(BASIC_CARD).setIsOwner(isOwner).removeAvatar().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().sellerInfo().image().should(hasAttribute(SRC, containsString(DUMMY_IMG)));
    }

}
