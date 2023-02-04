package ru.yandex.general.seo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralRequestModule;
import ru.yandex.general.step.JSoupSteps;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.general.consts.GeneralFeatures.AGGREGATE_OFFER_SEO_MARK;
import static ru.yandex.general.consts.GeneralFeatures.SEO_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.page.BasePage.AGGREGATE_OFFER;

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
        jSoupSteps.testing().path(MOSKVA).setMobileUserAgent().get();

        Assert.assertThat("Нет разметки ShemaOrg AggregateOffer",
                jSoupSteps.select("div[itemtype='http://schema.org/AggregateOffer']").html(),
                equalTo(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет LdJson AggregateOffer разметки на главной")
    public void shouldNotSeeAggregateOfferLdJsonMarkOnHomepage() {
        jSoupSteps.testing().path(MOSKVA).setMobileUserAgent().get();

        jSoupSteps.noLdJsonMark(AGGREGATE_OFFER);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет ShemaOrg AggregateOffer разметки на карточке оффера")
    public void shouldNotSeeAggregateOfferShemaOrgMarkOnOfferCard() {
        jSoupSteps.testing().uri(jSoupSteps.getActualOfferCardUrl()).setMobileUserAgent().get();

        Assert.assertThat("Нет разметки ShemaOrg AggregateOffer",
                jSoupSteps.select("div[itemtype='http://schema.org/AggregateOffer']").html(),
                equalTo(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет LdJson AggregateOffer разметки на карточке оффера")
    public void shouldNotSeeAggregateOfferLdJsonMarkOnOfferCard() {
        jSoupSteps.testing().uri(jSoupSteps.getActualOfferCardUrl()).setMobileUserAgent().get();

        jSoupSteps.noLdJsonMark(AGGREGATE_OFFER);
    }

}
