package ru.auto.tests.desktop.mobile.step;

import io.qameta.allure.Step;
import ru.auto.tests.desktop.models.MobileOffer;

import javax.inject.Inject;
import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.element.poffer.beta.BetaStsBlock.PLATE_NUMBER;
import static ru.auto.tests.desktop.element.poffer.beta.BetaStsBlock.VIN;
import static ru.auto.tests.desktop.element.poffer.beta.TechSection.MILEAGE_PLACEHOLDER;
import static ru.auto.tests.desktop.mobile.element.poffer.ContactsBlock.EMAIL;
import static ru.auto.tests.desktop.mobile.element.poffer.ContactsBlock.NAME;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

public class PofferSteps extends BasePageSteps {

    @Inject
    private MobileOffer offer;

    @Step("Выбираем марку")
    public void selectMark() {
        onPofferPage().markBlock().mark(offer.getMark()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем модель")
    public void selectModel() {
        onPofferPage().modelBlock().model(offer.getModel()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем год")
    public void selectYear() {
        onPofferPage().yearBlock().button(offer.getYear()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем тип кузова")
    public void selectBodyType() {
        onPofferPage().bodyTypeBlock().bodyType(offer.getBodyType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем поколение")
    public void selectGeneration() {
        onPofferPage().generationBlock().radioButton(offer.getGeneration()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем тип двигателя")
    public void selectEngineType() {
        onPofferPage().engineTypeBlock().button(offer.getEngineType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем коробку передач")
    public void selectGearType() {
        onPofferPage().gearTypeBlock().button(offer.getGearType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем привод")
    public void selectTransmission() {
        onPofferPage().transmissionBlock().button(offer.getTransmission()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем технические характеристики")
    public void selectTechParam() {
        onPofferPage().techParamBlock().radioButton(offer.getTechParam()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем цвет")
    public void selectColor() {
        onPofferPage().colorBlock().color(offer.getColor()).waitUntil(isDisplayed()).click();
    }

    @Step("Вводим пробег")
    public void enterMileage() {
        onPofferPage().mileageBlock().input(MILEAGE_PLACEHOLDER, offer.getMileage());
    }

    @Step("Выбираем газобалонное оборудование")
    public void selectGBO() {
        onPofferPage().extraBlock().checkbox("Газобаллонное оборудование").click();
    }

    @Step("Выбираем тип ПТС")
    public void selectPtsType() {
        onPofferPage().ptsType(offer.getPtsType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем количество владельцев")
    public void selectOwnersCount() {
        onPofferPage().ownersCount(offer.getOwnersCount()).waitUntil(isDisplayed()).click();
    }

    @Step("Вводим описание")
    public void enterDescription() {
        onPofferPage().description()
                .waitUntil(isDisplayed())
                .sendKeys(offer.getDescription());
    }

    @Step("Добавляем фото")
    public void fillPhoto() {
        onPofferPage().photoBlock().input().sendKeys(new File("src/test/resources/images/audi_a3.jpeg").getAbsolutePath());
        onPofferPage().photoBlock().photos().waitUntil(hasSize(equalTo(1)));
    }

    @Step("Вводим имя")
    public void enterName() {
        onPofferPage().contactsBlock().input(NAME, offer.getContactName());
    }

    @Step("Вводим email")
    public void enterEmail() {
        onPofferPage().contactsBlock().input(EMAIL, offer.getContactEmail());
    }

    @Step("Вводим госномер")
    public void enterPlateNumber() {
        onPofferPage().stsBlock().input(PLATE_NUMBER, offer.getPlateNumber());
    }

    @Step("Вводим VIN")
    public void enterVin() {
        onPofferPage().stsBlock().input(VIN, offer.getVin());
    }

    @Step("Вводим цену")
    public void enterPrice() {
        onPofferPage().priceBlock().clearPriceInput();
        onPofferPage().priceBlock().priceInput().sendKeys(offer.getPrice());
    }

    @Step("Выбираем валюту")
    public void selectCurrency(String k, String v) {
        onPofferPage().priceBlock().selectItem(k, v);
    }

    @Step("Размещаем объявление")
    public void submitForm() {
        waitSomething(4, SECONDS);
        onPofferPage().submitBlock().buttonContains("Разместить").waitUntil(isEnabled()).click();
    }

    @Step("Сравниваем созданный оффер с ожидаемым")
    public void compareOffers(String actualOffer, String expectedOfferPath) {
        assertThat("Не создался нужный оффер", actualOffer,
                jsonEquals(getResourceAsString(expectedOfferPath))
                        .when(IGNORING_EXTRA_ARRAY_ITEMS).when(IGNORING_ARRAY_ORDER));
    }

    public void fillMinimumFields() {
        waitSomething(2, SECONDS);
        selectMark();
        selectModel();
        selectYear();
        selectBodyType();
        selectGeneration();
        selectEngineType();
        selectGearType();
        selectTransmission();
        selectTechParam();
        selectColor();
        enterMileage();
        selectPtsType();
        selectOwnersCount();
        fillPhoto();
        enterName();
        enterEmail();
        enterPrice();
        enterPlateNumber();
        enterVin();
    }

    public void fillAllFields() {
        waitSomething(2, SECONDS);
        selectMark();
        selectModel();
        selectYear();
        selectBodyType();
        selectGeneration();
        selectEngineType();
        selectGearType();
        selectTransmission();
        selectTechParam();
        selectColor();
        enterMileage();
        selectGBO();
        selectPtsType();
        selectOwnersCount();
        fillPhoto();
        enterName();
        enterEmail();
        enterPrice();
        enterDescription();
        enterPlateNumber();
        enterVin();
    }

    public void fillEditForm() {
        selectPtsType();
        selectOwnersCount();
        enterDescription();
        enterName();
        enterEmail();
        enterPrice();
        enterPlateNumber();
        enterVin();
    }

}
