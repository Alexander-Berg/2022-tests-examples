//
//  OffersScreen.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/19/19.
//
import XCTest
import Snapshots

class OffersScreen: BaseScreen, Scrollable {

    enum WizardOptions: String, CaseIterable {
        case category = "Легковые"
        case mark = "BMW"
        case generation = "3 серия"
        case year = "2014"
        case subgeneration = "с 2011 по 2016, VI (F3x)"
        case body = "Седан"
        case engineType = "Бензин"
        case drive = "Полный"
        case transmission = "Автоматическая"
        case modification = "320i xDrive / 2.0 / AT / Бензин / 184 л.с."
        case color = "Белый"
        case pts = "Оригинал"
        case ownersCount = "Один"
    }

    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var addOfferButton = find(by: "add_offer").firstMatch
    lazy var enterPromocodeButton = find(by: "enter_promocode").firstMatch
    lazy var addOfferNavButton = find(by: "Добавить").firstMatch
    lazy var nextStepButton = find(by: "Далее").firstMatch
    lazy var addPhoto = find(by: "Добавить фото").firstMatch
    lazy var galleryPhoto = findAll(.collectionView).cells.firstMatch
    lazy var doneButton = find(by: "Done").firstMatch
    lazy var continueButton = find(by: "Продолжить").firstMatch
    lazy var skipPanoramaButton = find(by: "Позже").firstMatch
    lazy var skipButton = find(by: "Пропустить").firstMatch
    lazy var emailField = find(by: "Почта").firstMatch
    lazy var lastElementOnScrollView = find(by: "licenseAgreementTextNode").firstMatch
    lazy var profileButton = find(by: "user_profile_button").firstMatch
    lazy var walletButton = find(by: "wallet_button").firstMatch
    lazy var hud = find(by: "ActivityHUD").firstMatch
    lazy var deactivate = find(by: "Снять с продажи").firstMatch
    lazy var soldReason = find(by: "Я продал автомобиль где-то ещё").firstMatch
    lazy var addReviewButton = find(by: "Написать отзыв").firstMatch
    lazy var buyerPhone = find(by: "+7 929 112-55-95").firstMatch
    lazy var nobody = find(by: "Никому").firstMatch
    lazy var another = find(by: "Другой").firstMatch
    lazy var noRemember = find(by: "Не помню").firstMatch
    lazy var send = find(by: "Отправить").firstMatch
    lazy var never = find(by: "Никому").firstMatch
    lazy var userBannedSupportButton = find(by: "user_banned_support_button").firstMatch

    lazy var driveBanner = find(by: "view.drive_banner.container").firstMatch
    lazy var driveBannerButton = find(by: "Поехали").firstMatch
    lazy var driveBannerCloseIcon = find(by: "view.drive_banner.icn.close").firstMatch

    lazy var premiumOfferAssistantActiveBanner = find(by: "SaleSnippetPremiumAssistantActiveBanner").firstMatch
    lazy var premiumOfferAssistantInactiveBanner = find(by: "SaleSnippetPremiumAssistantInactiveBanner").firstMatch
    lazy var premiumOfferAssistantBannerButton = find(by: "SaleSnippetPremiumAssistantBannerButton").firstMatch

    lazy var poiBigPromoTryButton = find(by: "poiBigPromoTryButton").firstMatch
    lazy var panoramaLoadErrorMessage = find(by: "Не удалось загрузить панораму. Попробуйте ещё раз.").firstMatch
    lazy var galleryOverlayCloseButton = find(by: "galleryOverlayCloseButton").firstMatch
    lazy var editOfferNavBarItem = find(by: "editOfferNavBarItem").firstMatch
    lazy var editOfferPOILink = find(by: "Точки на панорамах").firstMatch
    lazy var panoramaPlayerScreenCloseButton = find(by: "panoramaPlayerScreenCloseButton").firstMatch
    lazy var closeAddPanoramaBannerButton = find(by: "closeAddPanoramaBannerButton").firstMatch
    lazy var addPanoramaBannerAnimation = find(by: "addPanoramaBannerAnimation").firstMatch
    lazy var userSaleInfoSnippetTitle = find(by: "userSaleInfoSnippetTitle").firstMatch

    func labelFor(_ option: WizardOptions) -> XCUIElement {
        return find(by: option.rawValue).firstMatch
    }

    enum DeactivationReason: String {
        case soldOnAutoru = "Я продал автомобиль на auto.ru"
        case soldToDealer = "Я продал автомобиль дилеру"
        case littleCalls = "Мало звонков от покупателей"
        case soldSomewhere = "Я продал автомобиль где-то ещё"
        case rethink = "Передумал продавать"
        case other = "Другая причина"
    }

    func deactivationReason(_ reason: DeactivationReason) -> XCUIElement {
        return find(by: reason.rawValue).firstMatch
    }
}
