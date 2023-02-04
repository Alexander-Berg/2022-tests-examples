package ru.yandex.realty.developer;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.page.NewBuildingSitePage.PXLS_TO_HIDEABLE_BLOCK;

@DisplayName("Ссылка на застройщика на карточке новостройки")
@Feature(NEWBUILDING_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
@Issue("VERTISTEST-1461")
public class NewbuildingCardUrlTest {

    private static final String NAME_TRANSLIT = "vektorstrojfinans";
    private static final String ID = "400209";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.resize(1400, 1600);
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path("/kvartaly-21-19-193551/").open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика")
    public void shouldSeeDeveloperUrl() {
        basePageSteps.onNewBuildingSitePage().siteCardAbout().developerPageLink().should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA_I_MO).developerPath(NAME_TRANSLIT, ID).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика в плавающей шапке")
    public void shouldSeeDeveloperUrlInHideableBlock() {
        basePageSteps.scrollDown(PXLS_TO_HIDEABLE_BLOCK);

        basePageSteps.onNewBuildingSitePage().hideableBlock().link().should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA_I_MO).developerPath(NAME_TRANSLIT, ID).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика в квартирографии")
    public void shouldSeeDeveloperUrlInSitePlans() {
        basePageSteps.onNewBuildingSitePage().sitePlans().click();
        basePageSteps.onNewBuildingSitePage().chooseFrom().click();

        basePageSteps.onNewBuildingSitePage().sitePlansModal().sitePlansDevInfo().link().should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA_I_MO).developerPath(NAME_TRANSLIT, ID).toString())));
    }

}
