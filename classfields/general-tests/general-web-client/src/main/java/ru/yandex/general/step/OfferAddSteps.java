package ru.yandex.general.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.awaitility.core.ConditionTimeoutException;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.general.beans.Attribute;
import ru.yandex.general.beans.ajaxRequests.updateDraft.Address;
import ru.yandex.general.beans.ajaxRequests.updateDraft.UpdateDraft;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.consts.FormConstants.Categories;
import ru.yandex.general.consts.FormConstants.Conditions;
import ru.yandex.general.page.FormPage;

import java.io.File;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Address.address;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Contacts.contacts;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.Form.form;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.GeoPoint.geoPoint;
import static ru.yandex.general.beans.ajaxRequests.updateDraft.UpdateDraft.updateDraft;
import static ru.yandex.general.beans.card.AddressText.addressText;
import static ru.yandex.general.beans.card.Video.video;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Conditions.USED;
import static ru.yandex.general.element.Input.FILE;
import static ru.yandex.general.element.Input.VALUE;
import static ru.yandex.general.page.FormPage.ADD_MORE_ADDRESS;
import static ru.yandex.general.page.FormPage.FIRST;
import static ru.yandex.general.page.FormPage.GIVE_FREE;
import static ru.yandex.general.page.FormPage.NAZVANIE;
import static ru.yandex.general.page.FormPage.NEXT;
import static ru.yandex.general.page.FormPage.OPISANIE;
import static ru.yandex.general.page.FormPage.PHOTO;
import static ru.yandex.general.page.FormPage.PUBLISH;
import static ru.yandex.general.page.FormPage.RUB;
import static ru.yandex.general.page.FormPage.SAVE;
import static ru.yandex.general.page.FormPage.SEND_BY_TAXI;
import static ru.yandex.general.page.FormPage.SEND_RUSSIA;
import static ru.yandex.general.page.FormPage.START_NEW;
import static ru.yandex.general.page.FormPage.VESCHI;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class OfferAddSteps extends BasePageSteps {

    private static final String DESCRIPTION = "Описание, несколько слов + символы";
    private static final String DEFAULT_ADDRESS = "проспект Карла Маркса, 2";
    public static final String PHOTO_NAMESPACE = "o-yandex";
    public static final String GOODS = "Goods";
    public static final String RABOTA = "Rabota";
    public static final String USED_CONDITION = "Used";
    public static final String NEW_CONDITION = "New";
    public static final String NULL_STRING = "null";


    private Categories category = PERENOSKA;
    private String name;
    private String description;
    private Conditions condition;
    private String price;
    private boolean freePrice;
    private List<String> addresses;
    private List<Attribute> attributes;
    private boolean sendByCourier;
    private boolean sendWithinRussia;

    @Inject
    private GeneralWebConfig config;

    @Step("Заполяем поля и публикуем оффер")
    public void addOffer() {
        resetIfDraft();
        fillSection();
        fillPhoto();
        nextClick();
        fillName();
        fillCategory();
        fillDescription();
        fillCondition();
        fillAttributes();
        fillPrice();
        nextClick();
        fillAddress();
        fillDelivery();
        publish();
    }

    public void fillToNameStep() {
        resetIfDraft();
        fillSection();
        fillPhoto();
        nextClick();
    }

    public void fillToCategoryStep() {
        fillToNameStep();
        fillName();
    }

    public void fillToDescriptionStep() {
        fillToCategoryStep();
        fillCategory();
    }

    public void fillToConditionStep() {
        fillToDescriptionStep();
        fillDescription();
    }

    public void fillToAttributesStep() {
        fillToConditionStep();
        fillCondition();
    }

    public void fillToPriceStep() {
        fillToAttributesStep();
        fillAttributes();
    }

    public void fillToAddressStep() {
        fillToPriceStep();
        fillPrice();
        nextClick();
    }

    public void fillToPublishStep() {
        fillToAddressStep();
        fillAddress();
        fillDelivery();
    }

    public void addPhoto() {
        setFileDetector();
        onFormPage().field(PHOTO).inputWithType(FILE).sendKeys(getRandomImagePath());
        await().atMost(15, SECONDS).pollInterval(1, SECONDS).until(() -> onFormPage().photoList().size() == 1);
    }

    @Step("Добавляем «{count}» фото к объявлению")
    public void addPhoto(int count) {
        setFileDetector();
        StringBuilder sb = new StringBuilder(getNumberedImagePath(1));
        for (int i = 1; i < count; i++) {
            sb.append(format(" \n %s", getNumberedImagePath(i + 1)));
        }
        onFormPage().field(PHOTO).inputWithType(FILE).sendKeys(sb.toString());
        onFormPage().photoList().waitUntil(hasSize(count));
    }

    private static String getDefaultImagePath() {
        return new File("src/test/resources/offer/offer_img_1.png").getAbsolutePath();
    }

    private static String getRandomImagePath() {
        return new File(format("src/test/resources/offer/offer_img_%d.png", getRandomIntInRange(1, 20))).getAbsolutePath();
    }

    private static String getNumberedImagePath(int number) {
        return new File(format("src/test/resources/offer/offer_img_%d.png", number)).getAbsolutePath();
    }

    private void setFileDetector() {
        if (!config.isLocalDebug()) {
            ((RemoteWebDriver) getDriver()).setFileDetector(new LocalFileDetector());
        }
    }

    public void publish() {
        onFormPage().button(PUBLISH).click();

        try {
            onFormPage().offerCard().waitUntil(isDisplayed());
        } catch (WaitUntilException e) {
            onFormPage().button(PUBLISH).click();
            waitSomething(1, SECONDS);
        }
    }

    public void save() {
        onFormPage().button(SAVE).click();

        try {
            onFormPage().offerCard().waitUntil(isDisplayed());
        } catch (WaitUntilException e) {
            onFormPage().button(SAVE).click();
            waitSomething(1, SECONDS);
        }
    }

    public OfferAddSteps nextClick() {
        int formFieldsBeforeNext = onFormPage().formFields().size();
        onFormPage().button(NEXT).click();

        try {
            await().pollDelay(500, MILLISECONDS).atMost(6, SECONDS).pollInterval(1, SECONDS)
                    .until(() -> {
                        if (onFormPage().formFields().size() > formFieldsBeforeNext) {
                            return true;
                        } else {
                            return false;
                        }
                    });
        } catch (ConditionTimeoutException e) {
            onFormPage().button(NEXT).click();
        }
        return this;
    }

    public OfferAddSteps resetIfDraft() {
        try {
            await().pollInterval(500, MILLISECONDS).atMost(3, SECONDS)
                    .until(onFormPage()::presetsBlock, isDisplayed());
        } catch (ConditionTimeoutException e) {
            onFormPage().spanLink(START_NEW).click();
            waitSomething(1500, MILLISECONDS);
        }
        return this;
    }

    public OfferAddSteps fillSection() {
        if (category.isWorkCategory()) {
            onFormPage().section("Работа").click();
        } else {
            onFormPage().section(VESCHI).click();
        }
        return this;
    }

    @Step("Заполняем поле «Название»")
    public OfferAddSteps fillName() {
        if (name != null) {
            clearAndSetName(name);
        } else {
            clearAndSetName(category.getTitle());
        }
        waitSomething(1, SECONDS);
        return this;
    }

    private void clearAndSetName(String name) {
        if (!onFormPage().field(NAZVANIE).input().getAttribute(VALUE).equals(""))
            onFormPage().field(NAZVANIE).input().clearInput().click();
        onFormPage().field(NAZVANIE).input().sendKeys(name);
    }

    @Step("Добавляем фото")
    public OfferAddSteps fillPhoto() {
        addPhoto();
        return this;
    }

    @Step("Выбираем категорию")
    public OfferAddSteps fillCategory() {
        onFormPage().categorySelect().spanLink(category.getCategoryName()).click();
        return this;
    }

    @Step("Заполняем описание, если указано")
    public OfferAddSteps fillDescription() {
        if (description != null) {
            onFormPage().field(OPISANIE).textarea().waitUntil(isDisplayed()).sendKeys(description);
        }
        return this;
    }

    @Step("Заполняем состояние, если указано")
    public OfferAddSteps fillCondition() {
        if (!category.isWorkCategory()) {
            if (condition != null) {
                onFormPage().condition(condition.getCondition()).click();
            } else {
                onFormPage().condition(USED.getCondition()).click();
            }
        }
        return this;
    }

    @Step("Заполнение блока цены/зп")
    public OfferAddSteps fillPrice() {
        if (price != null) {
            onFormPage().input(RUB).sendKeys(price);
        } else if (freePrice) {
            onFormPage().checkboxWithLabel(GIVE_FREE).click();
        }
        return this;
    }

    @Step("Заполнение адреса")
    public OfferAddSteps fillAddress() {
        if (!onFormPage().addressesList().get(0).input().getAttribute(VALUE).equals("")) {
        } else if (addresses != null) {
            addAddress(addresses.get(0));
            for (int i = 1; i < addresses.size(); i++) {
                onFormPage().spanLink(ADD_MORE_ADDRESS).click();
                addAddress(addresses.get(i), i);
            }
        } else {
            addAddress(DEFAULT_ADDRESS);
        }
        return this;
    }

    @Step("Заполнение доставки")
    public OfferAddSteps fillDelivery() {
        if (sendByCourier) {
            onFormPage().delivery().checkboxWithLabel(SEND_BY_TAXI).click();
            wait500MS();
        }
        if (sendWithinRussia) {
            onFormPage().delivery().checkboxWithLabel(SEND_RUSSIA).click();
            wait500MS();
        }
        return this;
    }

    @Step("Заполнение атрибутов, если указаны")
    public OfferAddSteps fillAttributes() {
        if (attributes != null) {
            onFormPage().input(RUB).hover();
            onFormPage().spoilerOpen().click();
            waitSomething(500, MILLISECONDS);
            for (Attribute attribute : attributes)
                fillAttribute(attribute);
        }
        return this;
    }

    public OfferAddSteps fillAttribute(Attribute attribute) {
        switch (attribute.getAttributeType()) {
            case MULTISELECT:
                onFormPage().attribute(attribute.getName()).input().waitUntil(isDisplayed()).click();
                for (String value : attribute.getValues()) {
                    onFormPage().popup().menuItem(value).waitUntil(isDisplayed()).click();
                    waitSomething(500, MILLISECONDS);
                }
                onFormPage().attribute(attribute.getName()).click();
                waitSomething(500, MILLISECONDS);
                break;
            case SELECT:
                onFormPage().attribute(attribute.getName()).input().waitUntil(isDisplayed()).click();
                onFormPage().popup().menuItem(attribute.getValue()).waitUntil(isDisplayed()).click();
                waitSomething(500, MILLISECONDS);
                break;
            case SWITCHER:
                onFormPage().attribute(attribute.getName()).switcher().click();
                waitSomething(500, MILLISECONDS);
                break;
            case INPUT:
                onFormPage().attribute(attribute.getName()).input().sendKeys(attribute.getValue());
                waitSomething(500, MILLISECONDS);
                break;
        }
        return this;
    }


    public void addAddress(String address, int index) {
        onFormPage().spanLink(ADD_MORE_ADDRESS).hover();
        onFormPage().addressesList().get(index).input().sendKeys(address);
        onFormPage().addressesSuggestList().get(FIRST).waitUntil(isDisplayed()).click();

        waitSomething(500, MILLISECONDS);
    }

    private void addAddress(String address) {
        addAddress(address, 0);
    }

    public OfferAddSteps withName(String name) {
        this.name = name;
        return this;
    }

    public OfferAddSteps withPrice(String price) {
        this.price = price;
        return this;
    }

    public OfferAddSteps withDescription(String description) {
        this.description = description;
        return this;
    }

    public OfferAddSteps withCategory(Categories category) {
        this.category = category;
        return this;
    }

    public OfferAddSteps withAddress(String... addresses) {
        this.addresses = asList(addresses);
        return this;
    }

    public OfferAddSteps withAttributes(Attribute... attributes) {
        this.attributes = asList(attributes);
        return this;
    }

    public OfferAddSteps withFreePrice() {
        this.freePrice = true;
        return this;
    }

    public OfferAddSteps withCondition(Conditions condition) {
        this.condition = condition;
        return this;
    }

    public OfferAddSteps withSendByCourier(boolean sendByCourier) {
        this.sendByCourier = sendByCourier;
        return this;
    }

    public OfferAddSteps withSendWithinRussia(boolean sendWithinRussia) {
        this.sendWithinRussia = sendWithinRussia;
        return this;
    }

    public static UpdateDraft getUpdateDraftTemplate() {
        return updateDraft().setForm(
                        form().setAddresses(asList())
                                .setAttributes(asList())
                                .setCategoryId(NULL_STRING)
                                .setCategoryPreset(NULL_STRING)
                                .setContacts(contacts().setEmail("").setPhone("").setPreferredWayToContact("Chat"))
                                .setDescription(NULL_STRING)
                                .setPhotos(asList())
                                .setTitle(NULL_STRING)
                                .setVideo(video().setUrl("")))
                .setUseNewForm(true);
    }

    public static Address getAddressTemplate() {
        return address().setGeoPoint(geoPoint().setLatitude(54.98296).setLongitude(82.897568))
                .setAddress(addressText().setAddress("проспект Карла Маркса, 2"));
    }

}
