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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.HOUSE;
import static ru.yandex.realty.consts.OfferAdd.RENT;
import static ru.yandex.realty.consts.OfferAdd.RENT_A_DAY;
import static ru.yandex.realty.consts.OfferAdd.ROOM;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.element.offers.PublishBlock.WARNING_PATTERN;
import static ru.yandex.realty.matchers.FindPatternMatcher.findPattern;
import static ru.yandex.realty.page.OfferAddPage.ADD_PHOTO;
import static ru.yandex.realty.page.OfferAddPage.PUBLISH_WITH_OPTIONS_FOR;
import static ru.yandex.realty.page.OfferAddPage.SAVE_CHANGES;
import static ru.yandex.realty.step.OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Добавление оффера. Валидация фото. Жилые")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PhotoValidationDwellingCreateTest {

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

    @Parameterized.Parameter
    public String dealType;

    @Parameterized.Parameter(1)
    public String realtyType;

    @Parameterized.Parameters(name = "{0} {1}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {SELL, FLAT},
                {SELL, ROOM},
                {SELL, HOUSE},
                {RENT, FLAT},
                {RENT, ROOM},
                {RENT, HOUSE},
                {RENT_A_DAY, FLAT},
                {RENT_A_DAY, ROOM},
                {RENT_A_DAY, HOUSE},
        });
    }

    @Before
    public void before() {
        api.createVos2Account(account, OWNER);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(dealType);
        offerAddSteps.onOfferAddPage().offerType().selectButton(realtyType);
        offerAddSteps.selectGeoLocation(OfferAddSteps.DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().publishBlock().button(ADD_PHOTO).should(isDisplayed());
        offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_CHANGES).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем по одному фото смотрим сколько осталось до подачи объявления")
    @Category({Regression.class, Testing.class})
    public void shouldSeeNeededCountOfPhoto() {
        addPhotoAndCheck();
        offerAddSteps.addPhotoNumber(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        offerAddSteps.onOfferAddPage().publishBlock().button(ADD_PHOTO).should(not(isDisplayed()));
        offerAddSteps.onOfferAddPage().publishBlock().payButton().should(isDisplayed())
                .should(hasText(findPattern(PUBLISH_WITH_OPTIONS_FOR)));
    }

    @Step("Добавляем {count} фото к объявлению и проверяем сколько осталось")
    private void addPhotoAndCheck() {
        for (int i = 1; i < DEFAULT_PHOTO_COUNT_FOR_DWELLING; i++) {
            offerAddSteps.addPhotoNumber(i);
            offerAddSteps.onOfferAddPage().publishBlock().button(ADD_PHOTO).should(isDisplayed());
            offerAddSteps.onOfferAddPage().publishBlock().button(SAVE_CHANGES).should(isDisplayed());
            offerAddSteps.onOfferAddPage().publishBlock().warning().should(hasText(findPattern(
                    format(WARNING_PATTERN, DEFAULT_PHOTO_COUNT_FOR_DWELLING - i))));
            // TODO: 26.02.2021 шаг нужен потому что в инпут почему-то загружается еще и предыдущий файл
            offerAddSteps.onOfferAddPage().gallery().deleteAllPhotos();
        }
    }
}
