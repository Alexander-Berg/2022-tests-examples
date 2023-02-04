//
//  SaleCardScreen.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/17/19.
//

import XCTest
import Snapshots

class SaleCardScreen: BaseScreen, Scrollable, NavigationControllerContent {
    public lazy var headerView = find(by: "header").firstMatch
    lazy var priceHistoryButton = find(by: "sale_card.header.btn.price_hist").firstMatch
    lazy var bookingBanner = find(by: "sale_card.header.booking_banner").firstMatch
    lazy var bookButton = find(by: "book_btn").firstMatch

    public lazy var characteristicTitle = find(by: "header").firstMatch
    public lazy var specialOffer = findAll(.staticText)["Уникальные условия"]
    public lazy var lastElementOnScrollView = findAll(.staticText)["320i 2.0 AT (184 л.с.)"]
    public lazy var carsCatalogTitle = findAll(.staticText)["Каталог автомобилей"]
    public lazy var likeButton = findAll(.staticText)["Избранное"]
    public lazy var creditOfferButton = find(by: "Оформить заявку").firstMatch
    public lazy var gallery = find(by: "images").firstMatch
    public lazy var reportButton = find(by: "Смотреть полный отчёт").firstMatch
    public lazy var showFullReportButton = find(by: "Смотреть полный отчёт").firstMatch

    public lazy var compareButton = find(by: "action_buttons_compare").firstMatch

    public lazy var snackbar = find(by: "snackbar").firstMatch
    public lazy var snackbarTitle = find(by: "snackbar_title").firstMatch
    public lazy var snackbarButton = find(by: "snackbar_button").firstMatch

    lazy var newCarRequestBanner = find(by: "view.new_car_request.container").firstMatch
    lazy var newCarRequestButton = find(by: "Получить лучшую цену").firstMatch

    lazy var driveBanner = find(by: "view.drive_banner.container").firstMatch
    lazy var driveBannerButton = find(by: "Поехали").firstMatch
    lazy var driveBannerCloseIcon = find(by: "view.drive_banner.icn.close").firstMatch

    lazy var sameButNewSection = find(by: "same-mark-model-but-new-offers").firstMatch

    lazy var tutorialHeader = find(by: "view.section_header_Проверьте перед покупкой").firstMatch
    lazy var complectationHeader = find(by: "complectation_header").firstMatch
    lazy var complectationComparisonLink = find(by: "complectation_comparison_link").firstMatch

    lazy var approvedSellerBadge = find(by: "proven_owner_inactive").firstMatch
    lazy var advantagesBadge = find(by: "advantages").firstMatch
    lazy var approvedSellerDialogButton = find(by: "Пройти проверку").firstMatch
    lazy var scoreBadge = find(by: "Качественное объявление").firstMatch

    lazy var reportPreviewTitle = find(by: "preview_title_cell").firstMatch
    lazy var dealerSubscriptionButton = find(by: "dealer_subscription").firstMatch
    lazy var dealerListingButton = find(by: "dealer_listing").firstMatch

    lazy var characteristics = find(by: "CharacteristicBlock").firstMatch

    lazy var chatButton = find(by: "chatButton").firstMatch
    lazy var phoneButton = find(by: "phoneButton").firstMatch

    lazy var reportBuySingleButton = find(by: "report_purchase_button").firstMatch

    lazy var premiumOfferAssistantActiveBanner = find(by: "SaleSnippetPremiumAssistantActiveBanner").firstMatch
    lazy var premiumOfferAssistantInactiveBanner = find(by: "SaleSnippetPremiumAssistantInactiveBanner").firstMatch
    lazy var premiumOfferAssistantBannerButton = find(by: "SaleSnippetPremiumAssistantBannerButton").firstMatch

    lazy var closeAddPanoramaBannerButton = find(by: "closeAddPanoramaBannerButton").firstMatch
    lazy var addPanoramaBannerAnimation = find(by: "addPanoramaBannerAnimation").firstMatch

    var scrollableElement: XCUIElement {
        return findAll(.collectionView).firstMatch
    }

    var greatDealBadge: XCUIElement {
        return app.staticTexts.containing(NSPredicate(format: "label IN %@", ["Хорошая цена", "Отличная цена"])).firstMatch
    }

    func reportBuyBundleButton(size: Int) -> XCUIElement {
        return find(by: "purchaseReportsPackageButtonId_\(size)").firstMatch
    }
}
