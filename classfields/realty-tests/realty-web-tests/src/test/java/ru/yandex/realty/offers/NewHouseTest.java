package ru.yandex.realty.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
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
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.realty.consts.OfferAdd.BUILT_YEAR;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 19.07.17.
 */
@DisplayName("Форма добавления объявления. Проверка чекбокса  «Ещё не сдан»")
@Feature(OFFERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class NewHouseTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void openManagementAddPage() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.fillRequiredFieldsForSellFlat("Окуловская улица");
        offerAddSteps.onOfferAddPage().featureField(BUILT_YEAR).selectCheckBox("Не сдан");
    }

    @Test
    @Description("Проверяем, что при нажатых кнопках «Ещё не сдан» и «От застройщика», опубликовать объявление невозможно")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeNewHouse() {
        offerAddSteps.onOfferAddPage().button("Обычная публикация").should(not(isDisplayed()));
    }

    @Test
    @Description("Появляется кнопка «От застройщика»")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void ShouldSeeAssignmentButton() {
        offerAddSteps.onOfferAddPage().priceField().featureField(DEAL_TYPE).button("От застройщика")
                .waitUntil(isDisplayed());
    }

    @Test
    @Description("Публикуем объявление, проверяем, что информация прокидывается на бэк")
    @Owner(IVANVAN)
    @Category({Regression.class, Testing.class})
    public void shouldSeeNewFlat() {
        offerAddSteps.onOfferAddPage().priceField().featureField(DEAL_TYPE).selectButton("Переуступка");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasNewFlat(true);
    }
}
