package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface GaragePage extends BasePage, WithButton {

    String ADD_CAR = "Поставить автомобиль";

    String OWNER_CHECK_POPUP_TEXT = "Отчёт ПроАвто в подарок!\nСкачайте приложение Авто.ру и добавьте машину в " +
            "Гараж. Далее подтвердите, что вы собственник, и получите отчëт ПроАвто в подарок! А при продаже " +
            "автомобиля в вашем объявлении появится бейдж «Продаëт собственник» — это привлечëт больше покупателей.\n" +
            "Пройти проверку";

    @Name("Попап проверенного собственника")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'OwnerCheckModal')]]")
    Popup ownerCheckPopup();

}
