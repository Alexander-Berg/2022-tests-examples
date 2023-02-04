package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.garage.AddBlock;
import ru.auto.tests.desktop.element.garage.MainBlock;

public interface GaragePage extends BasePage {

    String ADD_CAR = "Поставить автомобиль";

    String OWNER_CHECK_POPUP_TEXT = "Отчёт ПроАвто в подарок!\nКак получить отчёт?\nСкачайте приложение Авто.ру\n" +
            "Поставьте машину в Гараж и подтвердите, что вы собственник\nЗабирайте отчёт по VIN бесплатно\nА при " +
            "продаже авто вы получите бейдж «Продаëт собственник» — это привлечëт больше покупателей.\n" +
            "Google PlayAppStore";

    @Name("Блок «{{ text }}»")
    @FindBy("//div[contains(@class, 'PageGarageDesktop__block') and .//div[.= '{{ text }}']]")
    MainBlock block(@Param("text") String text);

    @Name("Блок «Добавьте свой автомобиль в гараж»")
    @FindBy("//div[contains(@class, 'GarageLandingGalleryDefaultItem__content')]")
    AddBlock addBlock();

    @Name("Попап проверенного собственника")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'OwnerCheckModal')]]")
    Popup ownerCheckPopup();

}
