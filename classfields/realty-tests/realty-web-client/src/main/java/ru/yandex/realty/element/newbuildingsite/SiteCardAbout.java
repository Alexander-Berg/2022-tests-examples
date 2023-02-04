package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface SiteCardAbout extends CardPhoneNb, Button {

    String CHAT_WITH_DEVELOPER = "Чат с застройщиком";

    @Name("Ссылка на страницу застройщика")
    @FindBy("//div[contains(@class,'CardDevBadge__text')]//a")
    Link developerPageLink();
}
