package ru.yandex.realty.showphone.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.NewbuildingContactResponse;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.element.offercard.PhoneBlock.TEL_HREF_PATTERN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.VillageCardResponse.villageCardTemplate;
import static ru.yandex.realty.step.UrlSteps.ID_VALUE;

@DisplayName("Показ телефона. Карточка поселка. Телефон в галерее")
@Feature(VILLAGE_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class CardShowPhoneGalleryTest {

    public static final String ID = "200200";
    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";

    private NewbuildingContactResponse newbuildingContactResponse;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в галерее. Бесплатный поселок")
    public void shouldSeePhoneFreeVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().link(CALL).click();
        basePageSteps.onVillageCardPage().gallery().link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в галерее. Платный ЖК")
    public void shouldSeePhonePayedVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().link(CALL).click();
        basePageSteps.onVillageCardPage().gallery().link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки в галерее.")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub500(ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageCardPage().photo().waitUntil(hasSize(greaterThan(0))).get(1).click();
        basePageSteps.onVillageCardPage().gallery().link(CALL).click();
        basePageSteps.acceptAlert();
        basePageSteps.onVillageCardPage().gallery().link(CALL).should(isDisplayed());
    }
}
