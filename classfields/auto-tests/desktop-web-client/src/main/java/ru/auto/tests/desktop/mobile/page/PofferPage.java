package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithBillingPopup;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.AddOfferNavigateModal;
import ru.auto.tests.desktop.mobile.element.poffer.BannerContinueInApp;
import ru.auto.tests.desktop.mobile.element.poffer.ContactsBlock;
import ru.auto.tests.desktop.mobile.element.poffer.MarkBlock;
import ru.auto.tests.desktop.mobile.element.poffer.ModelBlock;
import ru.auto.tests.desktop.mobile.element.poffer.PhotoBlock;
import ru.auto.tests.desktop.mobile.element.poffer.PriceBlock;
import ru.auto.tests.desktop.mobile.element.poffer.StsBlock;
import ru.auto.tests.desktop.mobile.element.poffer.SubmitBlock;
import ru.auto.tests.desktop.mobile.element.poffer.TechSection;

public interface PofferPage extends BasePage, WithBillingPopup, WithRadioButton {

    String KOMTRANS = "Комтранс";
    String MOTO = "Мото";

    @Name("Блок выбора марки")
    @FindBy("//div[@id = 'tech.mark']")
    MarkBlock markBlock();

    @Name("Блок выбора модели")
    @FindBy("//div[@id = 'tech.model']")
    ModelBlock modelBlock();

    @Name("Блок характеристик, год")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='year']")
    TechSection yearBlock();

    @Name("Блок характеристик, тип кузова")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='body_type']")
    TechSection bodyTypeBlock();

    @Name("Блок характеристик, поколение")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='super_gen']")
    TechSection generationBlock();

    @Name("Блок характеристик, тип двигателя")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='engine_type']")
    TechSection engineTypeBlock();

    @Name("Блок характеристик, тип коробки передач")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='gear_type']")
    TechSection gearTypeBlock();

    @Name("Блок характеристик, тип привода")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='transmission']")
    TechSection transmissionBlock();

    @Name("Блок характеристик, технических характеристик")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='tech_param']")
    TechSection techParamBlock();

    @Name("Блок пробега")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='mileage']")
    TechSection mileageBlock();

    @Name("Блок необычностей")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='extra']")
    TechSection extraBlock();

    @Name("Блок цвета")
    @FindBy("//div[contains(@class, 'TechAccordion__section') and @id='color']")
    TechSection colorBlock();

    @Name("Тип ПТС «{{ ptsType }}»")
    @FindBy("//div[@id='pts.pts_status']//button[.='{{ ptsType }}']")
    VertisElement ptsType(@Param("ptsType") String ptsType);

    @Name("Кол-во владельцев «{{ owners }}»")
    @FindBy("//div[@id='pts.owners_number']//button[.='{{ owners }}']")
    VertisElement ownersCount(@Param("owners") String owners);

    @Name("Поле ввода описания")
    @FindBy("//textarea[@name='description.description']")
    VertisElement description();

    @Name("Блок фото")
    @FindBy("//div[@class = 'OfferBlockPhoto']")
    PhotoBlock photoBlock();

    @Name("Блок контактов")
    @FindBy("//div[contains(@class, 'OfferFormBlock')][./h2[.='Контакты']]")
    ContactsBlock contactsBlock();

    @Name("Блок «Госномер и VIN»")
    @FindBy("//div[./h3[.='Госномер и VIN']]")
    StsBlock stsBlock();

    @Name("Блок цены")
    @FindBy("//div[@class='OfferBlockPrice']")
    PriceBlock priceBlock();

    @Name("Блок размещения")
    @FindBy("//div[@class = 'OfferFormSubmitBlock']")
    SubmitBlock submitBlock();

    @Name("Заголовок с тех. данными")
    @FindBy("//div[@class = 'OfferTechHeader OfferBlockTech__techHeader']")
    VertisElement techHeader();

    @Name("Баннер «{{ text }}»")
    @FindBy("//div[@class = 'MobilePofferBanner'][contains(., '{{ text }}')]")
    BannerContinueInApp banner(@Param("text") String text);

}
