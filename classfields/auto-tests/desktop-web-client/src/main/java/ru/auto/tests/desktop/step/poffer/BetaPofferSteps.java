package ru.auto.tests.desktop.step.poffer;

import io.qameta.allure.Step;
import ru.auto.tests.desktop.models.Offer;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import java.io.File;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.element.poffer.beta.BetaAddressBlock.ONLINE_SHOW;
import static ru.auto.tests.desktop.element.poffer.beta.BetaBadgesBlock.ADD_BUTTON;
import static ru.auto.tests.desktop.element.poffer.beta.BetaBadgesBlock.ADD_CUSTOM_BADGE_BUTTON;
import static ru.auto.tests.desktop.element.poffer.beta.BetaBadgesBlock.CUSTOM_TEXT_INPUT;
import static ru.auto.tests.desktop.element.poffer.beta.BetaContactsBlock.EMAIL;
import static ru.auto.tests.desktop.element.poffer.beta.BetaContactsBlock.NAME;
import static ru.auto.tests.desktop.element.poffer.beta.BetaDescriptionBlock.BEATEN;
import static ru.auto.tests.desktop.element.poffer.beta.BetaDescriptionBlock.CUSTOM;
import static ru.auto.tests.desktop.element.poffer.beta.BetaOptionsCurtainFooter.SAVE;
import static ru.auto.tests.desktop.element.poffer.beta.BetaPriceBlock.EXCHANGE;
import static ru.auto.tests.desktop.element.poffer.beta.BetaPtsBlock.MONTH;
import static ru.auto.tests.desktop.element.poffer.beta.BetaPtsBlock.ON_WARRANTY;
import static ru.auto.tests.desktop.element.poffer.beta.BetaPtsBlock.YEAR;
import static ru.auto.tests.desktop.element.poffer.beta.BetaPtsBlock.YEAR_OF_END;
import static ru.auto.tests.desktop.element.poffer.beta.BetaStsBlock.PLATE_NUMBER;
import static ru.auto.tests.desktop.element.poffer.beta.BetaStsBlock.STS;
import static ru.auto.tests.desktop.element.poffer.beta.BetaStsBlock.VIN;
import static ru.auto.tests.desktop.element.poffer.beta.TechSection.MILEAGE_PLACEHOLDER;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class BetaPofferSteps extends BasePageSteps {

    @Inject
    private Offer offer;

    @Step("Выбираем марку")
    public void selectMark() {
        onBetaPofferPage().markBlock().mark(offer.getMark()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем модель")
    public void selectModel() {
        onBetaPofferPage().modelBlock().model(offer.getModel()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем год")
    public void selectYear() {
        onBetaPofferPage().yearBlock().button(offer.getYear()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем тип кузова")
    public void selectBodyType() {
        onBetaPofferPage().bodyTypeBlock().bodyType(offer.getBodyType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем поколение")
    public void selectGeneration() {
        onBetaPofferPage().generationBlock().radioButton(offer.getGeneration()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем тип двигателя")
    public void selectEngineType() {
        onBetaPofferPage().engineTypeBlock().button(offer.getEngineType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем коробку передач")
    public void selectGearType() {
        onBetaPofferPage().gearTypeBlock().button(offer.getGearType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем привод")
    public void selectTransmission() {
        onBetaPofferPage().transmissionBlock().button(offer.getTransmission()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем технические характеристики")
    public void selectTechParam() {
        onBetaPofferPage().techParamBlock().radioButton(offer.getTechParam()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем цвет")
    public void selectColor() {
        onBetaPofferPage().colorBlock().color(offer.getColor()).waitUntil(isDisplayed()).click();
    }

    @Step("Вводи пробег")
    public void enterMileage() {
        onBetaPofferPage().mileageBlock().input(MILEAGE_PLACEHOLDER, offer.getMileage());
    }

    @Step("Выбираем газобалонное оборудование")
    public void selectGBO() {
        onBetaPofferPage().extraBlock().checkbox("Газобаллонное оборудование").click();
    }

    @Step("Переходим к следующему шагу")
    public void nextStep() {
        onBetaPofferPage().button("Продолжить").waitUntil(isDisplayed()).click();
        waitSomething(1, SECONDS);
    }

    @Step("Пропускаем шаг")
    public void skipStep() {
        onBetaPofferPage().button("Пропустить").waitUntil(isDisplayed()).click();
    }

    @Step("Пропускаем загрузку фото")
    public void skipPhoto() {
        onBetaPofferPage().photoBlock().checkboxContains("Загрузить фото позже").waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем количество владельцев")
    public void selectOwnersCount() {
        onBetaPofferPage().ptsBlock().ownersCount(offer.getOwnersCount()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем тип ПТС")
    public void selectPtsType() {
        onBetaPofferPage().ptsBlock().ptsType(offer.getPtsType()).waitUntil(isDisplayed()).click();
    }

    @Step("Выбираем дату покупки")
    public void selectPurchaseDate() {
        onBetaPofferPage().ptsBlock().selectItem(YEAR, offer.getPurchaseYear());
        onBetaPofferPage().ptsBlock().selectItem(MONTH, offer.getPurchaseMonth());
    }

    @Step("Выбираем гарантию и дату")
    public void selectWarranty() {
        onBetaPofferPage().ptsBlock().checkbox(ON_WARRANTY).click();
        onBetaPofferPage().ptsBlock().selectItem(YEAR_OF_END, offer.getWarrantyYear());
        onBetaPofferPage().ptsBlock().selectItem(MONTH, offer.getWarrantyMonth());
    }

    @Step("Вводим описание")
    public void enterDescription() {
        onBetaPofferPage().descriptionBlock().descriptionTextArea()
                .waitUntil(isDisplayed())
                .sendKeys(offer.getDescription());
    }

    @Step("Добавляем фото")
    public void fillPhoto() {
        onBetaPofferPage().photoBlock().input().sendKeys(new File(format("src/test/resources/images/%s", offer.getPhoto())).getAbsolutePath());
        onBetaPofferPage().photoBlock().photos().waitUntil(hasSize(equalTo(1)));
    }

    @Step("Очищаем описание")
    public void clearDescription() {
        onBetaPofferPage().descriptionBlock().clearDescription();
    }

    @Step("Выбираем комплектацию")
    public void selectComplectation() {
        onBetaPofferPage().complectationBlock().button(offer.getComplectation()).click();
    }

    @Step("Выбираем опции")
    public void selectOptions() {
        // Дичайший костыль со скрытием футера в этой шторке. Потому, что при ховере он подскролливает
        // нужный элемент прям вниз страницы, а там сверху вот этот вот футер. И если сделать скролл
        // по пикселям вниз - он не сработает, потому что это скролл внутри скролла (не вся страница, а честь)
        // Поэтому, кстати, только один элемент выбираем из селекта - дропдаун открывается черт пойми куда
        hideElement(onBetaPofferPage().optionCurtain().footer());
        onBetaPofferPage().optionCurtain()
                .selectItem(offer.getSelectOption().getKey(), offer.getSelectOption().getValue());
        offer.getCheckboxOptions().forEach(option ->
                onBetaPofferPage().optionCurtain().checkbox(option).hover().click());
        setElementAttribute(onBetaPofferPage().optionCurtain().footer(), "style", "display:flex");
        onBetaPofferPage().optionCurtain().footer().button(SAVE).click();
    }

    @Step("Выбираем Битый или не на ходу")
    public void selectBeaten() {
        onBetaPofferPage().descriptionBlock().checkbox(BEATEN).click();
    }

    @Step("Выбираем Не растаможен")
    public void selectCustom() {
        onBetaPofferPage().descriptionBlock().checkbox(CUSTOM).click();
    }

    @Step("Вводим имя")
    public void enterName() {
        onBetaPofferPage().contactsBlock().input(NAME, offer.getContactName());
    }

    @Step("Вводим email")
    public void enterEmail() {
        onBetaPofferPage().contactsBlock().input(EMAIL, offer.getContactEmail());
    }

    @Step("Выбираем тип связи")
    public void selectCommunicationType() {
        onBetaPofferPage().contactsBlock().communicationType(offer.getCommunicationType()).click();
    }

    @Step("Вводим адрес")
    public void enterAddress() {
        String address = offer.getAddress();

        onBetaPofferPage().addressBlock().input("Улица, метро, район", address);
        onBetaPofferPage().geoSuggest().waitUntil(isDisplayed());
        onBetaPofferPage().geoSuggest().regionContains(address)
                .waitUntil(isDisplayed())
                .click();
    }

    @Step("Выбираем Онлайн-показ")
    public void selectOnlineView() {
        onBetaPofferPage().addressBlock().checkboxContains(ONLINE_SHOW).click();
    }

    @Step("Вводим цену")
    public void enterPrice() {
        onBetaPofferPage().priceBlock().clearPriceInput();
        onBetaPofferPage().priceBlock().priceInput().sendKeys(offer.getPrice());
    }

    @Step("выбираем валюту")
    public void selectCurrency(String k, String v) {
        onBetaPofferPage().priceBlock().selectItem(k, v);
    }

    @Step("Выбираем Возможен обмен")
    public void selectExchange() {
        onBetaPofferPage().priceBlock().checkbox(EXCHANGE).click();
    }

    @Step("Вводим госномер")
    public void enterPlateNumber() {
        onBetaPofferPage().stsBlock().input(PLATE_NUMBER, offer.getPlateNumber());
    }

    @Step("Вводим VIN")
    public void enterVin() {
        onBetaPofferPage().stsBlock().input(VIN, offer.getVin());
    }

    @Step("Вводим СТС")
    public void enterSts() {
        onBetaPofferPage().stsBlock().input(STS, offer.getSts());
    }

    @Step("Добавляем кастомные бейджики")
    public void addCustomBadges(String... badges) {
        onBetaPofferPage().badges().button(ADD_CUSTOM_BADGE_BUTTON).click();

        for (String badge : badges) {
            enterCustomBadge(badge);
        }
    }

    @Step("Вводим свой кастомный бейдж")
    public void enterCustomBadge(String badgeText) {
        onBetaPofferPage().badges().input(CUSTOM_TEXT_INPUT, badgeText);
        onBetaPofferPage().badges().button(ADD_BUTTON).click();
    }

    @Step("Размещаем объявление")
    public void submitForm() {
        onBetaPofferPage().vasBlock().free().submitButton().click();
    }

    @Step("Скрываем плавающую кнопку поддержки и таймер скидки")
    public void hideFloatingSupportButtonAndDiscountTimer() {
        hideElement(onBetaPofferPage().supportFloatingButton());
        hideElement(onBetaPofferPage().discountTimer());
    }

    @Step("Сравниваем созданный оффер с ожидаемым")
    public void compareOffers(String actualOffer, String expectedOfferPath) {
        assertThat("Не создался нужный оффер", actualOffer,
                jsonEquals(getResourceAsString(expectedOfferPath))
                        .when(IGNORING_EXTRA_ARRAY_ITEMS).when(IGNORING_ARRAY_ORDER));
    }

    public void fillAllFields() {
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

        nextStep();

//        fillVideo("https://www.youtube.com/watch?v=qQHEtfNY_cU");
        fillPhoto();
//        skipPhoto();

        nextStep();

        selectPtsType();
        selectOwnersCount();
        selectPurchaseDate();
        selectWarranty();

        nextStep();

        enterDescription();
        selectBeaten();
        selectCustom();

        nextStep();
        skipStep();
        waitSomething(1, SECONDS);
        skipStep();

        enterName();
        enterEmail();
        selectCommunicationType();

        nextStep();

        enterAddress();
        selectOnlineView();

        nextStep();

        enterPrice();
        selectExchange();

        nextStep();

        enterPlateNumber();
        enterVin();
        enterSts();

        nextStep();
    }

    public void fillMinimumFields() {
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

        nextStep();
//        fillPhoto();
        skipPhoto();
        nextStep();

        selectPtsType();
        selectOwnersCount();
        selectPurchaseDate();

        nextStep();
        skipStep();
        waitSomething(3, SECONDS);
        skipStep();
        waitSomething(3, SECONDS);
        skipStep();

        enterName();
        enterEmail();

        nextStep();
        nextStep();

        enterPrice();

        nextStep();

        enterPlateNumber();
        enterVin();

        nextStep();
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
        enterSts();
    }
}
