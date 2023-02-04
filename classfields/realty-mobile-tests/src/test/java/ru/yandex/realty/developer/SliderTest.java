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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.beans.developer.slide.SlideResponse.slideTemplate;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.developer.Slide.CHOOSE_FLAT;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. Ссылки в слайдере")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SliderTest {

    private static final String ID = "2000";
    private static final String NAME = "Кекс";
    private static final String NAME_TRANSLIT = "keks";
    private static final String SITE_RGID = "1489";

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
        mockRuleConfigurable.developerStub(ENHANCED_DEV_ID,
                mockEnhancedDeveloper().setSlides(
                        slideTemplate().withSiteId(ID).withSiteName(NAME).withSiteRgid(SITE_RGID)).build())
                .createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка слайда")
    public void shouldSeeUrlInSlide() {
        basePageSteps.onDeveloperPage().slides().get(FIRST).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                        .path(format("%s-%s/", NAME_TRANSLIT, ID)).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка кнопки слайда")
    public void shouldSeeUrlInSlideButton() {
        basePageSteps.onDeveloperPage().slides().get(FIRST).button(CHOOSE_FLAT).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                        .path(format("%s-%s/", NAME_TRANSLIT, ID)).toString())));
    }

}
