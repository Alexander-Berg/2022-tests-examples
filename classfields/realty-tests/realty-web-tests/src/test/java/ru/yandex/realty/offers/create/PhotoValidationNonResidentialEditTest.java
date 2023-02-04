package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
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
import org.junit.runners.Parameterized;
import ru.auto.test.api.realty.OfferType;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_RENT;
import static ru.auto.test.api.realty.OfferType.COMMERCIAL_SELL;
import static ru.auto.test.api.realty.OfferType.GARAGE_RENT;
import static ru.auto.test.api.realty.OfferType.GARAGE_SELL;
import static ru.auto.test.api.realty.OfferType.LOT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.EDIT_OFFER;
import static ru.yandex.realty.element.offers.PublishBlock.WARNING_PATTERN;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.page.OfferAddPage.ADD_PHOTO;
import static ru.yandex.realty.page.OfferAddPage.SAVE_AND_CONTINUE;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Редактирование оффера. Валидация фото. Не жилые")
@Feature(OFFERS)
@Story(EDIT_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhotoValidationNonResidentialEditTest {

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

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Parameterized.Parameter
    public OfferType offerType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {COMMERCIAL_SELL},
                {COMMERCIAL_RENT},
                {GARAGE_SELL},
                {GARAGE_RENT},
                {LOT_SELL},
        });
    }

    @Before
    public void before() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        String offerId = offerBuildingSteps.addNewOffer(account).withType(offerType).create().getId();
        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(offerId).open();
        offerAddSteps.onOfferAddPage().gallery().deleteAllPhotos();
        offerAddSteps.onOfferAddPage().publishBlock().button(ADD_PHOTO).should(isDisplayed());
        offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_AND_CONTINUE).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем по одному фото смотрим сколько осталось до подачи объявления")
    @Category({Regression.class, Testing.class})
    public void shouldSeeNeededCountOfPhoto() {
        addPhotoAndCheck();
        offerAddSteps.addPhotoNumber(DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL);
        offerAddSteps.onOfferAddPage().publishBlock().button(ADD_PHOTO).should(not(isDisplayed()));
        offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_AND_CONTINUE).should(isDisplayed());
    }

    @Step("Добавляем {count} фото к объявлению и проверяем сколько осталось")
    private void addPhotoAndCheck() {
        for (int i = 1; i < DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL; i++) {
            offerAddSteps.addPhotoNumber(i);
            offerAddSteps.onOfferAddPage().publishBlock().button(ADD_PHOTO).should(isDisplayed());
            offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_AND_CONTINUE).should(not(isDisplayed()));
            offerAddSteps.onOfferAddPage().publishBlock().warning().should(hasText(findPattern(
                    format(WARNING_PATTERN, DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL - i))));
            // TODO: 26.02.2021 шаг нужен потому что в инпут почему-то загружается еще и предыдущий файл
            offerAddSteps.onOfferAddPage().gallery().deleteAllPhotos();
        }
    }
}
