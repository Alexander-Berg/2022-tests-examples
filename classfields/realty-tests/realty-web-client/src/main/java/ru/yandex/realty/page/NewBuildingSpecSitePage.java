package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.newbuildingsite.SiteCardAbout;

public interface NewBuildingSpecSitePage extends NewBuildingSitePage {

    @Override
    SiteCardAbout siteCardAbout();

    @Name("Карточка информации")
    @FindBy("//div[contains(@class, 'SiteCardSecondPackageHeader__container')]")
    SiteCardAbout siteCardSecondPackageHeader();

    @Name("Картинка галереи")
    @FindBy("//button[contains(@class,'SiteCardSecondPackageHeader__galleryBtn')]")
    AtlasWebElement photoShowButton();
}
