package ru.auto.tests.desktop.step.cabinet;

import io.qameta.allure.Step;
import org.hamcrest.Matchers;
import org.openqa.selenium.interactions.Actions;
import ru.auto.tests.desktop.step.BasePageSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 29.03.18
 */
public class CabinetOffersPageSteps extends BasePageSteps {

    private static final String APPLY = "Сохранить расписание";

    @Step("Добавляем стикеры {stickers} к офферу")
    public void addStickers(int offerIndex, String... stickers) {
        selectStickers(offerIndex, stickers);

        onCabinetOffersPage().quickSaleStickers().apply().click();
        onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Услуга применена"));
        onCabinetOffersPage().quickSaleStickers().should(not(isDisplayed()));
    }

    @Step("Выделяем стикеры {stickers}")
    public void selectStickers(int offerIndex, String... stickers) {
        onCabinetOffersPage().snippet(offerIndex).serviceButtons().stickers().hover();
        onCabinetOffersPage().quickSaleStickers().should(isDisplayed());

        asList(stickers)
                .forEach(stickerName -> onCabinetOffersPage().quickSaleStickers().sticker(stickerName).click());
    }

    @Step("Применяем услугу «Премиум»")
    public void applyPremiumService(int offerIndex) {
        onCabinetOffersPage().snippet(offerIndex).serviceButtons().premium().click();
        onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Услуга успешно применена"));
    }

    @Step("Применяем услугу «Спец»")
    public void applySpecService(int offerIndex) {
        onCabinetOffersPage().snippet(offerIndex).serviceButtons().special().click();
        onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Услуга успешно применена"));
    }

    @Step("Применяем услугу «Турбо-продажа»")
    public void applyTurboService(int offerIndex) {
        onCabinetOffersPage().snippet(offerIndex).serviceButtons().turbo().click();
        onCabinetOffersPage().serviceConfirmPopup().button("Да").click();
        onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Услуга успешно применена"));
    }

    @Step("Устанавливаем автоподнятие")
    public void setUpAutocapture(int offerIndex) {
        onCabinetOffersPage().snippet(offerIndex).serviceButtons().fresh().hover();
        onCabinetOffersPage().autocapturePopup().button(APPLY).should(not(isEnabled()));

        onCabinetOffersPage().autocapturePopup().dayOfWeek("Пн").click();

        onCabinetOffersPage().autocapturePopup().timeOfActivation().click();
        onCabinetOffersPage().withMenuPopup().item("01:00").click();
        onCabinetOffersPage().withMenuPopup().should(not(isDisplayed()));
    }

    @Step("Применяем автоподнятие")
    public void applyAutocapture(int offerIndex) {
        setUpAutocapture(offerIndex);
        onCabinetOffersPage().autocapturePopup().button(APPLY).click();
        onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Автоприменение услуги «Поднятие в поиске» включено"));
        onCabinetOffersPage().autocapturePopup().should(Matchers.not(isDisplayed()));
    }

    @Step("Сворачиваем боковое меню")
    public void collapseSidebar() {
        waitSomething(2, TimeUnit.SECONDS);
        new Actions(driver()).moveToElement(onCabinetOffersPage().sidebar().item("Свернуть"))
                .click(onCabinetOffersPage().sidebar().item("Свернуть")).build().perform();

        onCabinetOffersPage().sidebar().should(hasClass(containsString("Sidebar Sidebar_collapsed")));
    }

}
