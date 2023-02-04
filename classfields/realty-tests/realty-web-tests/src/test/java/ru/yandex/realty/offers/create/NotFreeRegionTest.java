package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.element.offers.PublishBlock.ACTIVE;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.page.OfferAddPage.FASTER_SALE;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.page.OfferAddPage.PUBLISH_FREE;
import static ru.yandex.realty.page.OfferAddPage.PUBLISH_WITH_OPTIONS_FOR;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class NotFreeRegionTest {

    private static final int DIFF = 103;

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
        compareSteps.resize(1280, 10000);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        urlSteps.shouldUrl(containsString("add"));
        offerAddSteps.fillRequiredFieldsForPublishBlock(OfferAddSteps.DEFAULT_LOCATION);
    }

    @Test
    @DisplayName("Скриншот блока публикации c табом «Продать быстрее» в платном регионе с фото")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeNotFreeWithPhotoFasterSalePublishBlock() {
        checkButtonHasTurboText();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        urlSteps.setProductionHost().open();

        offerAddSteps.fillRequiredFieldsForPublishBlock(OfferAddSteps.DEFAULT_LOCATION);
        checkButtonHasTurboText();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Скриншот блока публикации c табом «Обычная продажа» в платном регионе с фото c услугой")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeNotFreeWithPhotoNormalSalePublishBlock() {
        checkButtonHasServiceText();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        urlSteps.setProductionHost().open();

        offerAddSteps.fillRequiredFieldsForPublishBlock(OfferAddSteps.DEFAULT_LOCATION);
        checkButtonHasServiceText();
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production, DIFF);
    }

    @Test
    @DisplayName("Скриншот блока публикации c табом «Обычная продажа» в платном регионе с фото без услуг")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeNotFreeWithPhotoFreePublishBlock() {
        checkButtonWithoutServicesText();
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        urlSteps.setProductionHost().open();

        offerAddSteps.fillRequiredFieldsForPublishBlock(OfferAddSteps.DEFAULT_LOCATION);
        checkButtonWithoutServicesText();
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(PUBLISH_FREE));
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production, DIFF);
    }

    @Step("Проверяем что турбо выбрано c нужным текстом кнопки")
    private void checkButtonHasTurboText() {
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(FASTER_SALE).waitUntil(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishBlock().payButton()
                .should(hasText(findPattern(PUBLISH_WITH_OPTIONS_FOR)));
    }

    @Step("Проверяем что услуга выбрана c нужным текстом кнопки")
    private void checkButtonHasServiceText() {
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).clickWhile(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishBlock().payButton().should(hasText(findPattern(PUBLISH_WITH_OPTIONS_FOR)));
    }

    @Step("Проверяем что услуг нет c нужным текстом кнопки")
    private void checkButtonWithoutServicesText() {
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(FASTER_SALE).waitUntil(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).clickWhile(hasClass(containsString(ACTIVE)));
        offerAddSteps.onOfferAddPage().publishBlock().deSelectPaySelector(PROMOTION);
        offerAddSteps.onOfferAddPage().publishBlock().payButton().waitUntil(hasText(PUBLISH_FREE));
    }
}