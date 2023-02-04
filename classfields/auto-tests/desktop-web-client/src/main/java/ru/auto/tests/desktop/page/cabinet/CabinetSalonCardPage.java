package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.element.cabinet.card.AboutBlock;
import ru.auto.tests.desktop.element.cabinet.card.MarkBlock;
import ru.auto.tests.desktop.element.cabinet.card.Phone;
import ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CabinetSalonCardPage extends BasePage, WithNotifier, WithInput, WithSelect, WithButton, WithRadioButton {

    String ADD_REQUISITES = "Добавить реквизиты";
    String PHYSICAL = "Физлицо";
    String LEGAL_PERSON = "Юрлицо";
    String SITE = "Сайт";

    @Name("Блок «О салоне»")
    @FindBy(".//div[@class = 'CardAbout']")
    AboutBlock aboutBlock();

    @Name("Блок реквизитов юр.лица")
    @FindBy(".//div[contains(@class, 'CardDetailsPhysical')]")
    RequisitesBlock requisitesPhysicalBlock();

    @Name("Блок реквизитов юр.лица")
    @FindBy(".//div[contains(@class, 'CardDetailsJuridical')]")
    RequisitesBlock requisitesLegalPersonBlock();

    @Name("Список реквизитов")
    @FindBy(".//div[contains(@class, 'CardDetails__section')]")
    ElementsCollection<RequisitesBlock> requisitesList();

    @Name("Марка «{{ text }}»")
    @FindBy("//div[contains(@class, 'CardFileUploaderPreview__preview_bordered') and .//span[.='{{ text }}']]")
    MarkBlock markBlock(@Param("text") String Text);

    @Name("Инпут адреса")
    @FindBy("(//div[@class = 'CardMap']//input)[2]")
    VertisElement inputAddress();

    @Name("Список телефонов")
    @FindBy(".//div[@class = 'CardPhone']")
    ElementsCollection<Phone> phonesList();

    @Step("Получаем телефон с индексом {i}")
    default Phone getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }
}

