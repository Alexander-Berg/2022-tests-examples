package ru.auto.tests.desktop.page.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.element.Footer;
import ru.auto.tests.desktop.element.header.Header;
import ru.auto.tests.desktop.element.poffer.UserVas;
import ru.auto.tests.desktop.element.poffer.beta.AllMarksPopup;
import ru.auto.tests.desktop.element.poffer.beta.BetaBadgesBlock;
import ru.auto.tests.desktop.element.poffer.beta.LeftNavigationMenu;
import ru.auto.tests.desktop.element.poffer.beta.BetaAddressBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaAuctionBanner;
import ru.auto.tests.desktop.element.poffer.beta.BetaComplectationBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaContactsBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaDescriptionBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaMarkBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaModelBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaOptionsCurtain;
import ru.auto.tests.desktop.element.poffer.beta.BetaPhotoBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaPriceBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaPtsBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaStsBlock;
import ru.auto.tests.desktop.element.poffer.beta.BetaSupportFloatingButton;
import ru.auto.tests.desktop.element.poffer.beta.BetaVasBlock;
import ru.auto.tests.desktop.element.poffer.beta.TechSection;
import ru.auto.tests.desktop.page.BasePage;

public interface BetaPofferPage extends BasePage,
        WithSelect,
        WithGeoSuggest,
        WithButton,
        WithCheckbox,
        WithRadioButton {

    @Name("Блок марки при пустой форме")
    @FindBy("//div[@class='OfferWizardSectionMark']")
    VertisElement wizardMarkSection();

    @Name("Блок выбора марки")
    @FindBy("//div[contains(@class, 'MarkField')]")
    BetaMarkBlock markBlock();

    @Name("Попап со всеми марками")
    @FindBy("//div[contains(@class, 'MarkFieldModal')]")
    AllMarksPopup allMarksPopup();

    @Name("Блок выбора модели")
    @FindBy("//div[contains(@class, 'ModelField')]")
    BetaModelBlock modelBlock();

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

    @Name("Блок фото и видео")
    @FindBy("//div[@class='AccordionSection' and @id='section-photos']")
    BetaPhotoBlock photoBlock();

    @Name("Блок ПТС")
    @FindBy("//div[@class='AccordionSection' and @id='section-pts']")
    BetaPtsBlock ptsBlock();

    @Name("Блок описания")
    @FindBy("//div[@class='AccordionSection' and @id='section-description']")
    BetaDescriptionBlock descriptionBlock();

    @Name("Блок комплектации")
    @FindBy("//div[@class='AccordionSection' and @id='section-complectation']")
    BetaComplectationBlock complectationBlock();

    @Name("Блок опций")
    @FindBy("//div[@class='AccordionSection' and @id='section-equipment']")
    BetaComplectationBlock equipmentBlock();

    @Name("Блок контактов")
    @FindBy("//div[@class='AccordionSection' and @id='section-contacts']")
    BetaContactsBlock contactsBlock();

    @Name("Блок адреса")
    @FindBy("//div[@class='AccordionSection' and @id='section-address']")
    BetaAddressBlock addressBlock();

    @Name("Блок цены")
    @FindBy("//div[@class='AccordionSection' and @id='section-price']")
    BetaPriceBlock priceBlock();

    @Name("Блок СТС")
    @FindBy("//div[@class='AccordionSection' and @id='section-sts']")
    BetaStsBlock stsBlock();

    @Name("Баннер аукциона")
    @FindBy("//div[@class='AccordionSection' and @id='section-buyout']")
    BetaAuctionBanner auctionBanner();

    @Name("Стикеры быстрой продажи")
    @FindBy("//div[contains(@class, 'QuickSaleBadges')]")
    BetaBadgesBlock badges();

    @Name("Меню навигации")
    @FindBy("//div[@class='OfferAccordionContents']")
    LeftNavigationMenu leftNavigationMenu();

    @Name("Блок услуг")
    @FindBy("//div[contains(@class, 'VasFormUserPoffer')]")
    BetaVasBlock vasBlock();

    @Name("Блок услуг для частника")
    @FindBy(".//div[contains(@class, 'section_type_vas')] | " +
            ".//div[contains(@class, 'VasForm')]")
    UserVas userVas();

    @Name("Таймер скидки")
    @FindBy("//div[@class='VasFormUserTimer']")
    VertisElement discountTimer();

    @Name("Плавающая кнопка поддержки")
    @FindBy("//div[contains(@class, 'OfferFormFloatBottomRight_visible')]")
    BetaSupportFloatingButton supportFloatingButton();

    @Name("Шторка с выбором опций")
    @FindBy("//div[contains(@class, 'Curtain__container')]")
    BetaOptionsCurtain optionCurtain();

    @Name("Шапка")
    @FindBy("//div[@class='OfferFormHeader']")
    Header header();

    @Name("Футер")
    @FindBy("//div[contains(@class, 'AppOfferForm__footer')]")
    Footer footer();
}
