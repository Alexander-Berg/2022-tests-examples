package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;

public interface DocumentsPage extends BasePage, Button, SelectionBlock, InputField, Link {

    String BUY_DOGOVOR = "Договор купли-продажи недвижимого имущества";
    String BUY_AKT = "Передаточный акт к договору купли-продажи";
    String BUY_RASPISKA = "Расписка о получении денежных средств";
    String RENT_DOGOVOR = "Договор найма жилого помещения";
    String RENT_AKT = "Акт приема-передачи квартиры";
    String RENT_SPISOK = "Перечень передаваемого имущества";
    String RENT_RASPISKA = "Расписка о получении денежных средств";
    String RENT_RASTORZHENIE = "Соглашение о расторжении договора найма жилого помещения";
    String RENT_AKT_VOZVRAT = "Акт возврата квартиры";

    @Name("Контент страницы документы")
    @FindBy("//div[@class='PageContent']")
    AtlasWebElement pageContent();
}