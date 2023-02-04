package ru.yandex.realty.favorites;

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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mock.MockSite;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.FAVORITES;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.UrlSteps.FAVORITE_TYPE_PARAM;
import static ru.yandex.realty.step.UrlSteps.SITE_URL_VALUE;

@DisplayName("Ссылка на застройщика в новостройке из избранного")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
@Issue("VERTISTEST-1461")
public class DeveloperUrlTest {

    private static final int INT_ID = 123456;
    private static final String NAME = "Кекс";
    private static final String NAME_TRANSLIT = "keks";
    private static final String ID = "26500";

    private MockSite site;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Before
    public void before() {
        mockRuleConfigurable.favoritesStub(favoritesTemplate().addItem(format("site_%s", INT_ID)).build());
        site = mockSite(SITE_TEMPLATE).setId(INT_ID).setDeveloperId(ID).setDeveloperName(NAME);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на застройщика в новостройке из избранного")
    public void shouldSeeDeveloperUrlInFavorites() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                site.setSalesDepartment())).buildSite()).createWithDefaults();
        urlSteps.testing().path(Pages.FAVORITES).queryParam(FAVORITE_TYPE_PARAM, SITE_URL_VALUE).open();

        basePageSteps.onFavoritesPage().offer(0).link("Застройщик").should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(ZASTROYSCHIK)
                        .path(format("%s-%s/", NAME_TRANSLIT, ID)).toString())));
    }

}
