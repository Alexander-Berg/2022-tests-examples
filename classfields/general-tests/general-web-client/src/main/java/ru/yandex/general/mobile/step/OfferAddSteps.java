package ru.yandex.general.mobile.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.exception.WaitUntilException;
import org.awaitility.core.ConditionTimeoutException;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.general.beans.Attribute;
import ru.yandex.general.config.GeneralWebConfig;
import ru.yandex.general.consts.FormConstants.Categories;
import ru.yandex.general.consts.FormConstants.Conditions;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.consts.FormConstants.Categories.PERENOSKA;
import static ru.yandex.general.consts.FormConstants.Conditions.USED;
import static ru.yandex.general.element.Input.FILE;
import static ru.yandex.general.element.Input.VALUE;
import static ru.yandex.general.mobile.page.FormPage.ADD_MORE_ADDRESS;
import static ru.yandex.general.mobile.page.FormPage.ADRES;
import static ru.yandex.general.mobile.page.FormPage.COMPLETE;
import static ru.yandex.general.mobile.page.FormPage.ENTER_NAME;
import static ru.yandex.general.mobile.page.FormPage.NEXT;
import static ru.yandex.general.mobile.page.FormPage.PUBLISH;
import static ru.yandex.general.mobile.page.FormPage.RUB;
import static ru.yandex.general.mobile.page.FormPage.SEND_BY_TAXI;
import static ru.yandex.general.mobile.page.FormPage.SEND_RUSSIA;
import static ru.yandex.general.mobile.page.FormPage.VESCHI;
import static ru.yandex.general.mobile.page.FormPage.YOUTUBE_LINK;
import static ru.yandex.general.page.FormPage.RABOTA;
import static ru.yandex.general.page.FormPage.START_NEW;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class OfferAddSteps extends BasePageSteps {

    private static final String DEFAULT_ADDRESS = "проспект Карла Маркса, 2";

    private Categories category = PERENOSKA;
    private String name;
    private String description;
    private String videoUrl;
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
        fillName();
        fillCategory();
        fillDescription();
        fillVideo();
        fillCondition();
        fillAttributes();
        fillPrice();
        stepButtonClick();
        fillAddress();
        fillDelivery();
        finalStep(PUBLISH);
    }

    public void fillToPhotoStep() {
        resetIfDraft();
        fillSection();
    }

    public void fillToNameStep() {
        fillToPhotoStep();
        fillPhoto();
    }

    public void fillToCategoryStep() {
        fillToNameStep();
        fillName();
    }

    public void fillToDescriptionStep() {
        fillToCategoryStep();
        fillCategory();
    }

    public void fillToVideoStep() {
        fillToDescriptionStep();
        fillDescription();
    }

    public void fillToConditionStep() {
        fillToVideoStep();
        fillVideo();
    }

    public void fillToAttributesStep() {
        fillToConditionStep();
        fillCondition();
    }

    public void fillToPriceStep() {
        fillToAttributesStep();
        fillAttributes();
    }

    public OfferAddSteps fillToContactsStep() {
        fillToPriceStep();
        fillPrice();
        return this;
    }

    public OfferAddSteps fillToAddressStep() {
        fillToContactsStep();
        stepButtonClick();
        return this;
    }

    public OfferAddSteps fillToDeliveryStep() {
        fillToAddressStep();
        fillAddress();
        return this;
    }

    public OfferAddSteps fillToFinalStep() {
        fillToDeliveryStep();
        fillDelivery();
        return this;
    }

    @Step("Добавляем фото к объявлению")
    public void addPhoto() {
        setFileDetector();
        onFormPage().inputWithType(FILE).sendKeys(getRandomImagePath());
        await().atMost(15, SECONDS).pollInterval(1, SECONDS).until(() -> onFormPage().photoList().size() == 1);
        waitSomething(500, MILLISECONDS);
    }

    @Step("Добавляем «{count}» фото к объявлению")
    public void addPhoto(int count) {
        setFileDetector();
        StringBuilder sb = new StringBuilder(getNumberedImagePath(1));
        for (int i = 1; i < count; i++) {
            sb.append(format(" \n %s", getNumberedImagePath(i + 1)));
        }
        onFormPage().inputWithType(FILE).sendKeys(sb.toString());
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

    public void finalStep(String button) {
        onFormPage().button(button).click();

        try {
            onFormPage().offerCard().waitUntil(isDisplayed());
        } catch (WaitUntilException e) {
            onFormPage().button(button).click();
            waitSomething(1, TimeUnit.SECONDS);
        }
    }

    public OfferAddSteps stepButtonClick() {
        String screenBeforeButtonClick = onFormPage().screenTitle().getText();
        waitSomething(500, TimeUnit.MILLISECONDS);
        onFormPage().bottomButton().waitUntil(isDisplayed()).click();
        try {
            await().pollDelay(500, MILLISECONDS).atMost(6, SECONDS).pollInterval(1, SECONDS)
                    .until(() -> {
                        if (!screenBeforeButtonClick.equals(onFormPage().screenTitle().getText())) {
                            return true;
                        } else {
                            return false;
                        }
                    });
        } catch (ConditionTimeoutException e) {
            onFormPage().bottomButton().waitUntil(isDisplayed()).click();
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
            onFormPage().section(RABOTA).click();
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
        stepButtonClick();
        return this;
    }

    public void clearAndSetName(String name) {
        if (!onFormPage().input(ENTER_NAME).getAttribute(VALUE).equals(""))
            onFormPage().input(ENTER_NAME).clearInput().click();
        onFormPage().input(ENTER_NAME).sendKeys(name);
    }

    @Step("Добавляем фото")
    public OfferAddSteps fillPhoto() {
        addPhoto();
        stepButtonClick();
        return this;
    }

    @Step("Выбираем категорию")
    public OfferAddSteps fillCategory() {
        onFormPage().categorySelect().spanLink(category.getCategoryName()).click();
        stepButtonClick();
        return this;
    }

    @Step("Заполнение экрана описания")
    public OfferAddSteps fillDescription() {
        if (description != null) {
            onFormPage().textarea().sendKeys(description);
            stepButtonClick();
        } else {
            stepButtonClick();
        }
        return this;
    }

    @Step("Заполнение экрана видео")
    public OfferAddSteps fillVideo() {
        if (videoUrl != null) {
            onFormPage().input(YOUTUBE_LINK).sendKeys(videoUrl);
            stepButtonClick();
        } else {
            stepButtonClick();
        }
        return this;
    }

    @Step("Заполнение экрана состояния")
    public OfferAddSteps fillCondition() {
        if (!category.isSkipCondition()) {
            if (condition != null) {
                onFormPage().spanLink(condition.getCondition()).click();
            } else {
                onFormPage().spanLink(USED.getCondition()).click();
            }
        }
        return this;
    }

    @Step("Заполнение экрана цены")
    public OfferAddSteps fillPrice() {
        if (price != null) {
            onFormPage().input(RUB).sendKeys(price);
            stepButtonClick();
        } else if (freePrice) {
            onFormPage().switcher().click();
            stepButtonClick();
        } else {
            stepButtonClick();
        }
        return this;
    }

    @Step("Заполнение адреса")
    public OfferAddSteps fillAddress() {
        if (!onFormPage().input().getAttribute(VALUE).equals("")) {
            onFormPage().button(NEXT).click();
        } else if (addresses != null) {
            addAddress(addresses.get(0));
            for (int i = 1; i < addresses.size(); i++) {
                addMoreAddress(addresses.get(i));
            }
            stepButtonClick();
        } else {
            addAddress(DEFAULT_ADDRESS);
            stepButtonClick();
        }
        return this;
    }

    @Step("Заполнение экрана атрибутов")
    public OfferAddSteps fillAttributes() {
        if (attributes != null) {
            for (Attribute attribute : attributes)
                fillAttribute(attribute);
            stepButtonClick();
        } else {
            stepButtonClick();
        }
        return this;
    }

    @Step("Заполнение экрана доставки")
    public OfferAddSteps fillDelivery() {
        if (!category.isWorkCategory()) {
            if (sendByCourier) {
                onFormPage().checkboxWithLabel(SEND_BY_TAXI).click();
                wait500MS();
            }
            if (sendWithinRussia) {
                onFormPage().checkboxWithLabel(SEND_RUSSIA).click();
                wait500MS();
            }
            stepButtonClick();
        }
        return this;
    }

    public OfferAddSteps fillAttribute(Attribute attribute) {
        switch (attribute.getAttributeType()) {
            case MULTISELECT:
                onFormPage().attribute(attribute.getName()).waitUntil(isDisplayed()).click();
                for (String value : attribute.getValues()) {
                    onFormPage().popup().menuItem(value).waitUntil(isDisplayed()).click();
                    waitSomething(500, MILLISECONDS);
                }
                onFormPage().popup().closeFloatPopup().click();
                break;
            case SELECT:
                onFormPage().attribute(attribute.getName()).waitUntil(isDisplayed()).click();
                onFormPage().popup().menuItem(attribute.getValue()).waitUntil(isDisplayed()).click();
                break;
            case SWITCHER:
                onFormPage().attribute(attribute.getName()).checkboxWithLabel("").click();
                break;
            case INPUT:
                onFormPage().attribute(attribute.getName()).click();
                onFormPage().attribute(attribute.getName()).input().sendKeys(attribute.getValue());
                break;
        }
        return this;
    }


    public void addAddress(String address) {
        onFormPage().emptyInput().waitUntil(isDisplayed()).click();

        onFormPage().wrapper(ADRES).textarea().waitUntil(isDisplayed()).sendKeys(address);
        onFormPage().wrapper(ADRES).suggestItem(address).click();

        waitSomething(500, MILLISECONDS);
    }

    private void addMoreAddress(String address) {
        onFormPage().spanLink(ADD_MORE_ADDRESS).click();
        addAddress(address);
        onFormPage().button(COMPLETE).click();
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

    public OfferAddSteps withVideo(String videoUrl) {
        this.videoUrl = videoUrl;
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

}
