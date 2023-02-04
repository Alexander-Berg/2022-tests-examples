package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.page.OfferAddPage.SAVE_CHANGES;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class PublishBlockWithoutAddressTest {

    public static final String FILL_ADDRESS = "Указать адрес объекта";

    private static final int DIFF = 58;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        compareSteps.resize(1920, 10000);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        urlSteps.shouldUrl(containsString("add"));
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
    }

    @Test
    @DisplayName("Скриншот блока публикации если нет адреса")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeWithoutAddressPublishBlock() {
        offerAddSteps.onOfferAddPage().publishBlock().button(FILL_ADDRESS).should(isDisplayed());
        offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_CHANGES).should(isDisplayed());
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock().content());

        urlSteps.setProductionHost().open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.onOfferAddPage().publishBlock().button(FILL_ADDRESS).should(isDisplayed());
        offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_CHANGES).should(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock().content());
        compareSteps.screenshotsShouldBeTheSame(testing, production, DIFF);
    }
}