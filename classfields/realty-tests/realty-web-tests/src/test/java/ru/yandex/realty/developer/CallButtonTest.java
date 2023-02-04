package ru.yandex.realty.developer;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_CARD;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_GEO_ID_PATH;
import static ru.yandex.realty.mock.MockDeveloper.ENHANCED_DEV_ID;
import static ru.yandex.realty.mock.MockDeveloper.mockEnhancedDeveloper;
import static ru.yandex.realty.mock.MockDeveloper.SITES_FOR_DEVELOPER_CARD;

@Issue("VERTISTEST-1461")
@Feature(DEVELOPER_CARD)
@DisplayName("Карточка застройщика. Кнопка «Позвонить»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CallButtonTest {

    private static final String PHONE = "+7 (800) 555-35-35";

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

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Позвонить»")
    public void shouldSeePhone() {
        mockRuleConfigurable
                .developerStub(ENHANCED_DEV_ID, mockEnhancedDeveloper().setPhones(PHONE).build())
                .siteWithOffersStatStub(SITES_FOR_DEVELOPER_CARD)
                .createWithDefaults();
        urlSteps.testing().path(ENHANCED_DEV_GEO_ID_PATH).path(ZASTROYSCHIK).path(ENHANCED_DEV_ID).open();
        basePageSteps.onDeveloperPage().callButton().click();

        basePageSteps.onDeveloperPage().callButton().should(hasText(equalTo(PHONE)));
    }

}
