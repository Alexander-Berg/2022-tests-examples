package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.test.api.realty.BathEstateLocation.INSIDE;
import static ru.auto.test.api.realty.BathEstateLocation.OUTSIDE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.EXTRA;
import static ru.yandex.realty.consts.OfferAdd.HOUSE_TYPE;
import static ru.yandex.realty.consts.OfferAdd.LAND_AREA;
import static ru.yandex.realty.consts.OfferAdd.LAND_TYPE;
import static ru.yandex.realty.consts.OfferAdd.MORTGAGE;
import static ru.yandex.realty.consts.OfferAdd.NUMBER_OF_FLOOR;
import static ru.yandex.realty.consts.OfferAdd.SHOWER;
import static ru.yandex.realty.consts.Owners.IVANVAN;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_EDIT;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.model.offer.HouseType.PARTHOUSE;
import static ru.yandex.realty.model.offer.LotType.IGS;
import static ru.yandex.realty.utils.AccountType.OWNER;

/**
 * Created by ivanvan on 25.07.17.
 */

@DisplayName("Добавление оффера для продать дом")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class CreateSellHouseTest {

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
        offerAddSteps.fillRequiredFieldsForSellHouse();
    }

    @Test
    @DisplayName("Вводим площади участка")
    @Owner(IVANVAN)
    public void shouldSeeLandArea() {
        long value = Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().featureField(LAND_AREA).input().sendKeys(String.valueOf(value));

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasLotAreaValue(value);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(LAND_AREA).input().should(hasValue(String.valueOf(value)));
    }

    @Test
    @DisplayName("Кликаем на попам выбора единицы площади площади, устанавливаем «Гектар»")
    @Owner(IVANVAN)
    public void shouldSeeHectare() {
        long value = Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().featureField(LAND_AREA).input().sendKeys(String.valueOf(value));
        offerAddSteps.onOfferAddPage().featureField(LAND_AREA).button("Соток").click();
        offerAddSteps.onOfferAddPage().popupWithSpan().item("Гектар").click();

        offerAddSteps.publish();
        offerAddSteps.waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasLotAreaUnit("HECTARE");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(LAND_AREA).button("Гектар").should(isDisplayed());
    }

    @Test
    @DisplayName("Выбираем количество этажей")
    @Owner(IVANVAN)
    public void shouldSeeNumberOfFloors() {
        long value = Utils.getRandomShortLong();

        offerAddSteps.onOfferAddPage().featureField(NUMBER_OF_FLOOR).input().sendKeys(String.valueOf(value));

        offerAddSteps.publish();
        offerAddSteps.waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasFloorsTotal(value);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(NUMBER_OF_FLOOR).input().should(hasValue(String.valueOf(value)));
    }

    @Test
    @DisplayName("Выбираем тип участка")
    @Owner(IVANVAN)
    public void shouldSeeLandType() {
        offerAddSteps.onOfferAddPage().featureField(LAND_TYPE).selectButton("ИЖС");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasLotType("IGS");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(LAND_TYPE).button("ИЖС").should(isChecked());
    }

    @Test
    @DisplayName("Кликаем в поле «Душ» на кнопку «В доме»")
    @Owner(IVANVAN)
    public void shouldSeeShower() {
        offerAddSteps.onOfferAddPage().featureField(SHOWER).selectButton("В доме");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasShower(INSIDE.name());

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(SHOWER).button("В доме").should(isChecked());
    }

    @Test
    @DisplayName("Кликаем в поле «Санузел» на кнопку «На улице»")
    @Owner(IVANVAN)
    public void shouldSeeToilet() {
        offerAddSteps.onOfferAddPage().featureField("Санузел").selectButton("На улице");

        offerAddSteps.publish();
        offerAddSteps.waitPublish();
        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasToilet(OUTSIDE.toString());

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField("Санузел").button("На улице").should(isChecked());
    }

    @Test
    @DisplayName("В поле «Тип дома» кликаем на кнопку «Часть дома»")
    @Owner(IVANVAN)
    public void shouldSeeTypeOfHouse() {
        offerAddSteps.onOfferAddPage().featureField(HOUSE_TYPE).selectButton("Часть дома");

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasHouseType("PARTHOUSE");

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(HOUSE_TYPE).button("Часть дома").should(isChecked());
    }

    @Test
    @DisplayName("В поле «Цена» -> «Дополнительно» кликаем на кнопку «Ипотека»")
    @Owner(KANTEMIROV)
    public void shouldSeeMortgage() {
        offerAddSteps.onOfferAddPage().featureField(EXTRA).checkBox(MORTGAGE).click();

        offerAddSteps.publish().waitPublish();

        Assertions.assertThat(api.getOfferInfo(account).getSpecific()).hasMortgage(true);

        urlSteps.testing().path(MANAGEMENT_NEW_EDIT).path(api.getOfferInfo(account).getOffer().getId()).open();
        offerAddSteps.onOfferAddPage().featureField(EXTRA).checkBox(MORTGAGE).should(isChecked());
    }

}
