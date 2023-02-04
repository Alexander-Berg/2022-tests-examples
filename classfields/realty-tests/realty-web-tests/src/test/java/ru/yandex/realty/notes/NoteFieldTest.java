package ru.yandex.realty.notes;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.NOTES;
import static ru.yandex.realty.element.saleads.ActionBar.YOUR_NOTE;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by kopitsa on 27.07.17.
 */
@DisplayName("Заметки для объявления. Наличие поля заметки")
@Feature(NOTES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class NoteFieldTest {

    private static final String USER_NOTE = "Заметка";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Видим поле добавления заметки")
    public void shouldSeeNoteField() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().offersList().should(hasSize(greaterThanOrEqualTo(2)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(YOUR_NOTE).waitUntil(isDisplayed()).click();
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(1));
        basePageSteps.onOffersSearchPage().offer(1).actionBar().buttonWithTitle(YOUR_NOTE).waitUntil(isDisplayed()).click();
        basePageSteps.scrollUp(1000);
        Screenshot testingScreenshot = compareSteps.getElementScreenshot(basePageSteps.onOffersSearchPage()
                .offersList().get(0).addNoteField());

        urlSteps.production().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().offersList().should(hasSize(greaterThanOrEqualTo(2)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(YOUR_NOTE).waitUntil(isDisplayed()).click();
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(1));
        basePageSteps.onOffersSearchPage().offer(1).actionBar().buttonWithTitle(YOUR_NOTE).waitUntil(isDisplayed()).click();
        basePageSteps.scrollUp(1000);
        Screenshot productionScreenshot = compareSteps.getElementScreenshot(basePageSteps.onOffersSearchPage()
                .offersList().get(0).addNoteField());

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим сохраненную заметку под неавторизованным пользователем")
    public void shouldSeeSavedNoteNotAuthorizedUser() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().offersList().should(hasSize(greaterThanOrEqualTo(1)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offer(FIRST).actionBar().buttonWithTitle(YOUR_NOTE).click();
        basePageSteps.onOffersSearchPage().offer(FIRST).addNoteField().input().sendKeys(USER_NOTE);
        basePageSteps.onOffersSearchPage().offer(FIRST).saveNote().click();
        basePageSteps.onOffersSearchPage().offer(FIRST).addNoteField().input().should(hasValue(USER_NOTE));
    }
}
