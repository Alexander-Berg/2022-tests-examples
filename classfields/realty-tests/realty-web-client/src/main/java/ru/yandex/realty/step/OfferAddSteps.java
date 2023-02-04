package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.config.RealtyApiConfig;
import ru.yandex.realty.matchers.CurrentUrlMatcher;
import ru.yandex.realty.page.OfferAddPage;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.OfferAdd.APART_HOUSE;
import static ru.yandex.realty.consts.OfferAdd.AREA;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE_DIRECT;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.FLOOR;
import static ru.yandex.realty.consts.OfferAdd.GARAGE;
import static ru.yandex.realty.consts.OfferAdd.HOUSE;
import static ru.yandex.realty.consts.OfferAdd.HOUSE_TYPE;
import static ru.yandex.realty.consts.OfferAdd.PRIVATE;
import static ru.yandex.realty.consts.OfferAdd.REASSIGNMENT;
import static ru.yandex.realty.consts.OfferAdd.ROOMS_TOTAL;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.OfferAdd.STATUS;
import static ru.yandex.realty.consts.OfferAdd.TYPE;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_SALE;
import static ru.yandex.realty.page.OfferAddPage.PROMOTION;
import static ru.yandex.realty.rules.MockRule.addOfferId;

/**
 * Created by ivanvan on 17.07.17.
 */
public class OfferAddSteps extends WebDriverSteps {

    private static final int USER_FLOOR = 0;
    private static final int TOTAL_FLOOR = 1;
    private static final String ROOMS_AMOUNT = "1";
    private static long MIN_AREA = 10L;

    public static final int DEFAULT_PHOTO_COUNT_FOR_DWELLING = 4;
    public static final int DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL  = 2;

    public static final String DEFAULT_LOCATION = "Суздальский проспект, 1к1";
    public static final String MOSCOW_LOCATION = "улица Цюрупы, 20к1";
    public static final String REGION_LOCATION = "Нефтезаводская улица, 59";

    @Inject
    private RealtyApiConfig config;

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private Vos2Adaptor vos2Adaptor;

    public OfferAddPage onOfferAddPage() {
        return on(OfferAddPage.class);
    }

    @Step("Публикуем объявление бесплатно без услуг")
    public OfferAddSteps publish() {
        waitSaveOnBackend();
        onOfferAddPage().publishBlock().sellTab(NORMAL_SALE).click();
        onOfferAddPage().publishBlock().deSelectPaySelector(PROMOTION);
        onOfferAddPage().publishBlock().payButton().click();
        return this;
    }

    @Step("Публикуем объявление бесплатно без услуг (Для агенств)")
    public OfferAddSteps normalPlacement() {
        waitSaveOnBackend();
        onOfferAddPage().publishBlock().payButton().click();
        return this;
    }

    public OfferAddSteps waitPublish() {
        Account account = accountKeeper.get().get(0);
        addOfferId(vos2Adaptor.waitUserOffers(account.getId()).getOffers().get(0).getId());
        shouldUrl("Должны перейти на страницу " + MANAGEMENT_NEW, allOf(containsString(MANAGEMENT_NEW),
                not(containsString(MANAGEMENT_NEW_ADD))));
        return this;
    }

    @Step("Проверяем url: {reason}")
    @Override
    public void shouldUrl(String reason, Matcher<String> matcher) {
        assertThat(reason, getDriver(), withWaitFor(CurrentUrlMatcher.shouldUrl(matcher)));
    }

    @Step("Заполняем необходимые поля для «Продать - Квартиру»")
    public void fillRequiredFieldsForSellFlat(String location) {
        onOfferAddPage().dealType().selectButton(SELL);
        onOfferAddPage().offerType().selectButton(FLAT);
        addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);
        fillWithoutPhone(location);
    }

    @Step("Заполняем необходимые поля для появления блока публикации")
    public void fillRequiredFieldsForPublishBlock(String location) {
        onOfferAddPage().dealType().selectButton(SELL);
        onOfferAddPage().offerType().selectButton(FLAT);
        addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        selectGeoLocation(location);
        setFlat("1");
    }

    public void fillWithoutPhone(String location) {
        long price = 1000000 + Utils.getRandomShortLong();
        long floor = Utils.getRandomShortLong();
        long area = MIN_AREA + Utils.getRandomShortLong();

        onOfferAddPage().featureField(ROOMS_TOTAL).selectButton(ROOMS_AMOUNT);
        onOfferAddPage().featureField(FLOOR).inputList().waitUntil(hasSize(greaterThan(TOTAL_FLOOR))).get(TOTAL_FLOOR)
                .clearSign().clickIf(isDisplayed());
        onOfferAddPage().featureField(FLOOR).inputInItem(TOTAL_FLOOR, String.valueOf(floor + 1));
        onOfferAddPage().featureField(FLOOR).inputList().waitUntil(hasSize(greaterThan(USER_FLOOR))).get(USER_FLOOR)
                .clearSign().clickIf(isDisplayed());
        onOfferAddPage().featureField(FLOOR).inputInItem(USER_FLOOR, String.valueOf(floor));
        onOfferAddPage().featureField(AREA).inputList().waitUntil(hasSize(greaterThan(0))).get(0)
                .clearSign().clickIf(isDisplayed());
        onOfferAddPage().featureField(AREA).inputInItem(0, String.valueOf(area));

        onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
        selectGeoLocation(location);
        setFlat("1");
    }

    public void fillRequiredFieldsForSellFlat() {
        fillRequiredFieldsForSellFlat(DEFAULT_LOCATION);
    }

    @Step("Заполняем необходимые поля для продать дом")
    public void fillRequiredFieldsForSellHouse() {
        onOfferAddPage().dealType().selectButton(SELL);
        onOfferAddPage().offerType().selectButton(HOUSE);
        addPhoto(DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        long area = MIN_AREA + Utils.getRandomShortLong();
        long price = Utils.getRandomShortLong();

        selectGeoLocation(DEFAULT_LOCATION);
        onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(area));
        onOfferAddPage().featureField(HOUSE_TYPE).selectButton(APART_HOUSE);
        onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
        onOfferAddPage().featureField(DEAL_TYPE).selectButton(REASSIGNMENT);
    }

    @Step("Заполняем необходимые поля для продать гараж")
    public void fillRequiredFieldsForSellGarage() {
        onOfferAddPage().dealType().selectButton(SELL);
        onOfferAddPage().offerType().selectButton(GARAGE);
        long price = 1000000 + Utils.getRandomShortLong();

        onOfferAddPage().featureField(TYPE).selectButton(GARAGE);
        onOfferAddPage().featureField(STATUS).selectButton(PRIVATE);
        addPhoto(DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL);
        selectGeoLocation(DEFAULT_LOCATION);
        onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
    }

    @Step("Заполняем необходимые поля для всех видов коммерческой")
    public void fillRequiredFieldsForCommercial() {
        addPhoto(DEFAULT_PHOTO_COUNT_FOR_NON_RESIDENTIAL);
        long area = MIN_AREA + Utils.getRandomShortLong();
        long price = Utils.getRandomShortLong();

        selectGeoLocation(REGION_LOCATION);
        onOfferAddPage().featureField(AREA).input().sendKeys(String.valueOf(area));
        onOfferAddPage().priceField().priceInput().sendKeys(String.valueOf(price));
    }

    @Step("Вводим адрес {location}")
    public void selectGeoLocation(String location) {
        onOfferAddPage().locationControls().suggest().waitUntil(isDisplayed()).click();
        onOfferAddPage().locationControls().iconClear().clickWhile(isDisplayed());
        onOfferAddPage().locationControls().suggest().sendKeys(location);
        onOfferAddPage().locationControls().suggestListItem(location).clickWhile(not(isDisplayed()));
        onOfferAddPage().locationControls().suggest().should(hasValue(location));
        waitSaveOnBackend();
    }

    @Step("Вводим квартиру «{flat}»")
    public void setFlat(String flat) {
        onOfferAddPage().locationControls().clearSign("Квартира").clickIf(isDisplayed());
        onOfferAddPage().locationControls().flat().sendKeys(flat);
        onOfferAddPage().locationControls().flat().should(hasValue(flat));
        waitSaveOnBackend();
    }

    @Step("Двигаем курсор на элемент")
    public void moveCursorToElement(AtlasWebElement element) {
        Actions actions = new Actions(getDriver());
        actions.moveToElement(element).build().perform();
    }

    @Step("Добавляем фото к объявлению")
    public void addPhoto() {
        setFileDetector();
        onOfferAddPage().addPhoto().input().sendKeys(getDefaultImagePath());
    }

    @Step("Добавляем фото номер {number} к объявлению")
    public void addPhotoNumber(int number) {
        setFileDetector();
        onOfferAddPage().addPhoto().input().sendKeys(getNumberedImagePath(number));
    }

    @Step("Добавляем {count} фото к объявлению")
    public void addPhoto(int count) {
        setFileDetector();
        for (int i = 0; i < count; i++) {
            // TODO: 26.02.2021 шаг нужен потому что в инпут почему-то загружается еще и предыдущий файл
            onOfferAddPage().gallery().deleteAllPhotos();
            onOfferAddPage().addPhoto().input().sendKeys(getNumberedImagePath(i + 1));
            onOfferAddPage().gallery().attachCards().waitUntil(Matchers.hasSize(i + 1));
        }
    }

    public static String getDefaultImagePath() {
        return new File("src/test/resources/offer/offer_img.jpg").getAbsolutePath();
    }

    public static String getNumberedImagePath(int number) {
        return new File(format("src/test/resources/offer/offer_photo_%d.jpg", number)).getAbsolutePath();
    }

    private void setFileDetector() {
        if (!config.isLocalDebug()) {
            ((RemoteWebDriver) getDriver()).setFileDetector(new LocalFileDetector());
        }
    }

    public OfferAddSteps waitSaveOnBackend() {
        //todo: нужно подождать пока не отработает бэкэнд... заменить или на мок или обрубать запрос
        // иначе данные которые будут введены в течении этого времени могут не сохраниться
        waitSomething(3, TimeUnit.SECONDS);
        return this;
    }

}
