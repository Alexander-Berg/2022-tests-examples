package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PromoDealerPanoramasPage extends PromoDealerPage {

    @Name("Видео «Как снимать панорамы»")
    @FindBy("//iframe[contains(@class, 'BasePromoPage__video')]")
    VertisElement panoramasHowToVideo();

    @Name("Изображение в блоке «Как сделать объявление заметнее»")
    @FindBy("//section[@id = 'noticeable']//img")
    VertisElement noticeableImage();

    @Name("Изображение в блоке «Точки на внешних панорамах»")
    @FindBy("//section[@id = 'points']//img")
    VertisElement pointsImage();

    @Name("Изображение в блоке «Панорамы интерьера»")
    @FindBy("//section[@id = 'interier']//img")
    VertisElement interiorImage();
}