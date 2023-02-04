package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.AGGREGATE_OFFER_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.page.BasePage.AGGREGATE_OFFER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;

@Epic(SEO_FEATURE)
@Feature(AGGREGATE_OFFER_SEO_MARK)
@DisplayName("Нет AggregateOffer разметки на главной и карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralRequestModule.class)
public class SeoNoAggregateOfferMarkingTest {

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет ShemaOrg AggregateOffer разметки на главной")
    public void shouldNotSeeAggregateOfferShemaOrgMarkOnHomepage() {
        jSoupSteps.testing().path(MOSKVA).setDesktopUserAgent().get();

        Assert.assertThat("Нет разметки ShemaOrg AggregateOffer",
                jSoupSteps.select("div[itemtype='http://schema.org/AggregateOffer']").html(),
                equalTo(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет LdJson AggregateOffer разметки на главной")
    public void shouldNotSeeAggregateOfferLdJsonMarkOnHomepage() {
        jSoupSteps.testing().path(MOSKVA).setDesktopUserAgent().get();

        jSoupSteps.noLdJsonMark(AGGREGATE_OFFER);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет ShemaOrg AggregateOffer разметки на карточке оффера")
    public void shouldNotSeeAggregateOfferShemaOrgMarkOnOfferCard() {
        jSoupSteps.testing().uri(jSoupSteps.getActualOfferCardUrl()).setDesktopUserAgent().get();

        Assert.assertThat("Нет разметки ShemaOrg AggregateOffer",
                jSoupSteps.select("div[itemtype='http://schema.org/AggregateOffer']").html(),
                equalTo(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет LdJson AggregateOffer разметки на карточке оффера")
    public void shouldNotSeeAggregateOfferLdJsonMarkOnOfferCard() {
        jSoupSteps.testing().uri(jSoupSteps.getActualOfferCardUrl()).setDesktopUserAgent().get();

        jSoupSteps.noLdJsonMark(AGGREGATE_OFFER);
    }

}
