package ru.auto.tests.desktop.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.page.BasePage;

public interface MyCarBlock extends BasePage, WithButton, WithInput {

    String MY_CAR_BLOCK_TEXT_ADD = "Мой автомобиль\nПросто введите госномер своей машины, и мы поставим её в Гараж" +
            "\nГосномер или VIN\nПоставить\nVolkswagen Jetta\nГод\n2011\nДвигатель\n1,4 л / 150 л.с." +
            "\nЦвет\nСерый\nРегион учёта\nМосква\nVIN\nWVWZZZ16*BM*****2\nПроверьте регион и поменяйте его, если " +
            "не соответствует действительности\nДобавить в гараж";

    String MY_CAR_BLOCK_TEXT_ADD_WITH_EDIT_REGION = "Мой автомобиль\nПросто введите госномер своей машины, и мы поставим" +
            " её в Гараж\nГосномер или VIN\nПоставить\nVolkswagen Jetta\nГод\n2011\nДвигатель\n1,4 л / 150 л.с.\nЦвет" +
            "\nСерый\nРегион учёта\nХимки\nVIN\nWVWZZZ16*BM*****2\nПроверьте регион и поменяйте его, если не " +
            "соответствует действительности\nДобавить в гараж";

    String MY_CAR_BLOCK_TEXT_PASS = "Мой автомобиль\nПросто введите госномер своей машины, и мы поставим её в Гараж" +
            "\nГосномер или VIN\nПоставить\nVolkswagen Jetta\nГод\n2011\nДвигатель\n1,4 л / 150 л.с.\nЦвет\nКрасный" +
            "\nРегион учёта\nМосква\nVIN\nWVWZZZ16ZBM121912\nПроверьте регион и поменяйте его, если не соответствует " +
            "действительности\nПерейти в гараж";

    String MY_CAR_BLOCK_TEXT = "Мой автомобиль\nПросто введите госномер своей машины, и мы " +
            "поставим её в Гараж\nГосномер или VIN";

    String EX_CAR_BLOCK_TEXT = "Моя бывшая\nПоставьте и узнайте, как она там без вас\nВведите VIN";

    String PUT_INTO_GARAGE_BUTTON = "Поставить в Гараж";


    @Name("Кнопка «?»")
    @FindBy(".//div[contains(@class, '_questionIcon')]")
    VertisElement questionButton();

}
