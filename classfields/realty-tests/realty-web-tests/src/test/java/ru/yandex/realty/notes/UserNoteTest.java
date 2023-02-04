package ru.yandex.realty.notes;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.RealtyFeatures.NOTES;
import static ru.yandex.realty.element.saleads.ActionBar.YOUR_NOTE;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by vicdev on 17.07.17.
 */
@DisplayName("Заметки для объявления. Создание/удаление заметки")
@Feature(NOTES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class UserNoteTest {

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

    @Before
    public void openPage() {
        apiSteps.createVos2Account(account, OWNER);
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Создаем заметку")
    public void shouldCreateUserNote() {
        String testingNote = getRandomString();
        String offerId = addNote(testingNote);
        urlSteps.testing().path(FAVORITES).open();
        basePageSteps.onFavoritesPage().favoritesList().get(0).waitUntil(isDisplayed());
        apiSteps.shouldSeeUserNote(account.getId(), offerId, equalTo(testingNote));
        basePageSteps.onFavoritesPage().favoritesList().get(0).addNoteField().input()
                .should("текст заметки неправильный", hasAttribute("value", testingNote));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Удаляем заметку")
    public void shouldDeleteUserNote() {
        String testingNote = getRandomString();
        String offerId = addNote(testingNote);
        basePageSteps.onOffersSearchPage().offersList().get(0).addNoteField().hover();
        basePageSteps.onOffersSearchPage().offersList().get(0).deleteNote().click();
        urlSteps.testing().path(FAVORITES).open();
        apiSteps.shouldNotSeeUserNote(account.getId(), offerId);
        basePageSteps.onFavoritesPage().favoritesList().should(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesList().get(0).addNoteField().should(not(isDisplayed()));
    }

    @Step("Добавляем заметку к объявлению")
    private String addNote(String testingNote) {
        basePageSteps.onOffersSearchPage().offersList().should(hasSize(greaterThanOrEqualTo(1)));
        basePageSteps.moveCursor(basePageSteps.onOffersSearchPage().offersList().get(0));
        basePageSteps.onOffersSearchPage().offersList().get(0).actionBar().buttonWithTitle(YOUR_NOTE).click();
        basePageSteps.onOffersSearchPage().offersList().get(0).addNoteField().input().sendKeys(testingNote);
        basePageSteps.onOffersSearchPage().offersList().get(0).saveNote().click();
        return basePageSteps.getOfferId(basePageSteps.onOffersSearchPage().offersList().get(0).offerLink());
    }
}
