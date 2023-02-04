package ru.auto.tests.desktop.element.compare;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface AddModelPopup extends VertisElement, WithButton, WithInput {

    @Name("Заголовок")
    @FindBy(".//p[contains(@class, 'FormToAddModel__title')]")
    VertisElement title();

    @Name("Хлебные крошки")
    @FindBy(".//p[contains(@class, 'FormToAddModel__nameplate')]")
    VertisElement breadcrumbs();

    @Name("Хлебная крошка «{{ text }}»")
    @FindBy(".//span[@class = 'FormToAddModel__breadcrumb' and .= '{{ text }}']")
    VertisElement bredcrumb(@Param("text") String Text);

    @Name("Кнопка закрытия")
    @FindBy(".//div[contains(@class,'modal__close')]")
    VertisElement close();

    @Name("Список  марок/моделей")
    @FindBy(".//li[@class = 'FormToAddModel__item']")
    ElementsCollection<VertisElement> marksOrModelsList();

    @Step("Получаем марку/модель с индексом {i}")
    default VertisElement getListItem(int i) {
        return marksOrModelsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Марка/модель «{{ text }}» в списке")
    @FindBy(".//li[@class = 'FormToAddModel__item' and .= '{{ text }}']")
    VertisElement markOrModel(@Param("text") String Text);

    @Name("Поколение «{{ text }}»")
    @FindBy(".//div[@class = 'FormToAddModel__photoItem' and .//span[.= '{{ text }}']]")
    VertisElement generation(@Param("text") String text);

    @Name("Кузов «{{ text }}»")
    @FindBy(".//div[@class = 'FormToAddModel__photoItem' and .//p[.= '{{ text }}']]")
    VertisElement body(@Param("text") String text);

    @Step("Кнопка «Добавить»")
    @FindBy(".//span[.='Добавить']")
    VertisElement submitButton();
}