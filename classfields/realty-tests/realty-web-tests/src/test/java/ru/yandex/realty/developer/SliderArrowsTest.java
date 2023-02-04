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
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.SITES_FOR_DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. Стрелки слайдера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SliderArrowsTest {

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
        mockRuleConfigurable
                .developerStub(ENHANCED_DEV_ID, mockEnhancedDeveloper().build())
                .siteWithOffersStatStub(SITES_FOR_DEVELOPER_CARD)
                .createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключение слайдов налево")
    public void shouldSeeLeftSlide() {
        basePageSteps.onDeveloperPage().slider().arrowLeft().click();

        basePageSteps.onDeveloperPage().slider().getLastSlide().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переключение слайдов направо")
    public void shouldSeeRightSlide() {
        basePageSteps.onDeveloperPage().slider().arrowRight().click();

        basePageSteps.onDeveloperPage().slider().slides().get(2).should(isDisplayed());
    }

}
