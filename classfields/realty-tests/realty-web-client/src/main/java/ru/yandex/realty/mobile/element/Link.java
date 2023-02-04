package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.RealtyElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Link extends AtlasWebElement {

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//a[contains(.,'{{ value }}')]")
    RealtyElement link(@Param("value") String value);

    @Name("Картинка спецпроекта «{{ value }}»")
    @FindBy(".//img[contains(@class,'MenuSpecialProjectLogo')]")
    AtlasWebElement specialProject();

    @Name("Ссылка")
    @FindBy(".//a")
    AtlasWebElement link();

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//span[contains(.,'{{ value }}')]")
    RealtyElement spanLink(@Param("value") String value);

    // TODO: 10.08.2020 убрать в отдельный класс
    default String offerId() {
        String url = link().getAttribute("href");
        Matcher m = Pattern.compile("/(\\d{4,})/").matcher(url);
        m.find();
        return m.group(1);
    }

}
