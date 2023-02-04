package ru.yandex.realty.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Screenshooter;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.element.offers.auth.ContactInfo;
import ru.yandex.realty.module.RealtyWebWithProxyModuleWithoutPhone;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.OfferAdd.HOW_TO_ADDRESS;
import static ru.yandex.realty.consts.OfferAdd.TYPE_OF_ACCOUNT;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;

/**
 * Created by ivanvan on 10.08.17.
 */
@DisplayName("Форма добавления объявления. Частник без привязанного телефона")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModuleWithoutPhone.class)
public class OwnerWithoutPhoneTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private ProxySteps proxySteps;

    @Before
    public void openManagement() {
        api.createYandexAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.onOfferAddPage().featureField(TYPE_OF_ACCOUNT).selectButton("Собственник");
    }

    @Test
    @DisplayName("Проверяем, что телефон сохраняется на back")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeePhoneInVos() {
        offerAddSteps.onOfferAddPage().contactInfo().featureField(HOW_TO_ADDRESS).input().sendKeys("Валера");
        String phone = proxySteps.fillAndConfirmPhone();
        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getUser()).hasPhones("+" + phone);
    }

    @Test
    @DisplayName("Должны увидеть предупреждение, что не выбран номер")
    @Owner(IVANVAN)
    @Category({Regression.class, Screenshooter.class, Testing.class})
    public void shouldSeeWarning() {
        offerAddSteps.publish();
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage().contactInfo()
                .featureField(ContactInfo.YOUR_PHONE).error());

        urlSteps.production().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat();
        offerAddSteps.publish();

        Screenshot productionScreenshot = compareSteps.getElementScreenshot(offerAddSteps.onOfferAddPage()
                .contactInfo().featureField(ContactInfo.YOUR_PHONE).error());

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
