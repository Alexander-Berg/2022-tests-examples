//
//  SaleCardListSteps.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/17/19.
//

import XCTest
import Snapshots

class SaleCardListSteps: BaseSteps {
    enum OfferPosition {
        case title
        case body
        case gallery
        case footer
    }

    enum StockOfferPosition {
        case title
        case body
        case request
    }

    func onSaleCardListScreen() -> SaleCardListScreen {
        return baseScreen.on(screen: SaleCardListScreen.self)
    }

    @discardableResult
    func addToFavorites() -> SaleCardListSteps {
        onSaleCardListScreen().addToFavoritesButton.tap()
        return self
    }

    @discardableResult
    func confirmAddToFavorites() -> Self {
        if onSaleCardListScreen().confirmAlertText.exists {
            onSaleCardListScreen().confirmAlertOK.firstMatch.tap()
        }
        return self
    }

    func tapBack() -> BaseSteps {
        onSaleCardListScreen().backButton.tap()
        return BaseSteps(context: context)
    }

    @discardableResult
    func scrollToStockOffer(with offerId: String, position: StockOfferPosition = .title, maxSwipes: Int = 50) -> Self {
        let element: XCUIElement
        switch position {
        case .title:
            element = onSaleCardListScreen().stockOfferTitle(for: offerId)
        case .body:
            element = onSaleCardListScreen().stockOfferBody(for: offerId)
        case .request:
            element = onSaleCardListScreen().stockOfferRequest(for: offerId)
        }
        onSaleCardListScreen().scrollTo(element: element, maxSwipes: maxSwipes)
        return self
    }

    @discardableResult
    func scrollToOffer(with offerId: String, position: OfferPosition = .title, maxSwipes: Int = 50) -> Self {
        let element: XCUIElement
        switch position {
        case .title:
            element = onSaleCardListScreen().offersCardTitle(for: offerId)
        case .body:
            element = onSaleCardListScreen().offerBody(for: offerId)
        case .gallery:
            element = onSaleCardListScreen().find(by: "offer_image_\(offerId)").firstMatch
        case .footer:
            element = onSaleCardListScreen().offerFooter(for: offerId)
        }
        onSaleCardListScreen().scrollTo(element: element, maxSwipes: maxSwipes, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 150, right: 0))
        return self
    }

    @discardableResult
    func scroll(to: String, maxSwipes: Int = 50) -> Self {
        let element: XCUIElement = onSaleCardListScreen().find(by: to).firstMatch
        onSaleCardListScreen().scrollTo(element: element, maxSwipes: maxSwipes)
        return self
    }

    func scrollSnippetGallery(offerID: String, elementToFind: String, scrollForcefully: Bool = false) -> Self {
        scrollToOffer(with: offerID)
        let element: XCUIElement = onSaleCardListScreen().find(by: elementToFind).firstMatch
        let gallery = onSaleCardListScreen().find(by: "offer_image_\(offerID)").firstMatch

        if scrollForcefully {
            let rightOfCentre = gallery.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5))
            let leftOfCentre = gallery.coordinate(withNormalizedOffset: CGVector(dx: 0.1, dy: 0.5))
            rightOfCentre.press(forDuration: 0.1, thenDragTo: leftOfCentre)
        } else {
            gallery.scrollTo(
                element: element,
                swipeDirection: .left,
                maxSwipes: 10
            )
        }

        return self
    }

    func tapBestPriceBanner(snapshotId: String = #function) -> GetBestPriceSteps {
        let screen = onSaleCardListScreen()
        screen.scrollTo(
            element: screen.bestPriceBanner,
            maxSwipes: 12,
            windowInsets: UIEdgeInsets(top: 100, left: 0, bottom: 100, right: 0)
        )
        validateSnapshot(of: screen.bestPriceBanner, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16), snapshotId: snapshotId)
        screen.bestPriceBannerButton.tap()
        return GetBestPriceSteps(context: context)
    }

    func openFilters() -> FiltersSteps {
        onSaleCardListScreen().filterButton.tap()
        return self.as(FiltersSteps.self)
    }

    @discardableResult
    func tapAutoRuOnlyBadge() -> AutoRuOnlyModalSteps<SaleCardListSteps> {
        onSaleCardListScreen().offerAutoRuBadge().tap()

        return AutoRuOnlyModalSteps(context: context, source: self)
    }

    @discardableResult
    func tapReportBadge() -> SaleCardSteps {
        onSaleCardListScreen().reportBadge().tap()

        return SaleCardSteps(context: context)
    }

    @discardableResult
    func likeCarOffer(withId id: String) -> Self {
        onSaleCardListScreen().offerFavButton(forId: id).tap()
        return self
    }

    @discardableResult
    func openCarOffer(with offerID: String) -> SaleCardSteps {
        let title = onSaleCardListScreen().offersCardTitle(for: offerID)
        title.staticTexts.firstMatch.tap()

        return SaleCardSteps(context: context)
    }

    @discardableResult
    func openStockCardOffer(offersTitle: String) -> StockCardSteps {
        scrollTo(offersTitle)
        _ = tap(offersTitle)
        return StockCardSteps(context: context)
    }

    @discardableResult
    func scrollTo(_ selector: String, maxSwipes: Int = 14) -> Self {
        onSaleCardListScreen().scrollTo(element: onSaleCardListScreen().find(by: selector).firstMatch, maxSwipes: maxSwipes, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0))
        return self
    }

    @discardableResult
    func swipeUp() -> Self {
        onSaleCardListScreen().swipe(.up)
        return self
    }

    @discardableResult
    func swipeDown() -> Self {
        onSaleCardListScreen().swipe(.down)
        return self
    }

    func tapOnCreditBanner() -> PreliminaryCreditSteps {
        onSaleCardListScreen().find(by: "credit_baner").firstMatch.tap()
        return PreliminaryCreditSteps(context: context)
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16), file: "SaleListTests")
            return self
        }
    }

    func checkFavoriteIconOnSnippetIsSelected(offerId: String) -> Self {
        return validateSnapShot(accessibilityId: "fav_offer_\(offerId)", snapshotId: "favorite_icon_on_snippet_selected", message: "Сравниваем иконку избранного на сниппете, когда оффер в избранном")
    }

    func tapOnCreditPrice() -> SaleCardSteps {
        onSaleCardListScreen().find(by: "carCreditPrice").firstMatch.tap()
        return SaleCardSteps(context: context)
    }

    func tapSortButton() -> SaleListSortingModalSteps<SaleCardListSteps> {
        onSaleCardListScreen().find(by: "ic sort").firstMatch.tap()
        return SaleListSortingModalSteps(context: context, source: self)
    }

    @discardableResult
    func tapConditionSegment(index: Int) -> Self {
        onSaleCardListScreen().segmentedControlAt(index: index).tap()
        return self
    }

    func checkHasSelected(sorting: String) {
        let label = onSaleCardListScreen()
            .sortingInfoContainer.staticTexts
            .allElementsBoundByIndex[1]
            .label

        XCTAssertEqual(label, sorting)
    }

    func checkContains(sorting: String) {
        let labels = onSaleCardListScreen()
            .sortingInfoContainer.staticTexts
            .allElementsBoundByIndex.map(\.label)

        XCTAssertTrue(labels.contains(where: { $0 == sorting }))
    }

    @discardableResult
    func scrollToStockOffer(with offerID: String, maxSwipes: Int = 50) -> Self {
        Step("Скроллим к стоковому сниппету '\(offerID)'") {
            let element = self.onSaleCardListScreen().stockOfferTitle(for: offerID)
            self.onSaleCardListScreen().scrollTo(element: element, maxSwipes: maxSwipes)
        }

        return self
    }

    @discardableResult
    func tapOnStockOffer(with offerID: String) -> StockCardSteps {
        Step("Тапаем на стоковый оффер '\(offerID)'") {
            self.onSaleCardListScreen().stockOfferTitle(for: offerID).tap()
        }

        return StockCardSteps(context: context)
    }

    @discardableResult
    func checkSnippetBodyHasLabel(offer: String, text: String) -> SaleCardListSteps {
        let body = onSaleCardListScreen().offerBody(for: offer)
        body.staticTexts[text].shouldExist()
        return self
    }

    @discardableResult
    func checkIsVisible() -> Self {
        step("Проверяем видимость листинга") {
            onSaleCardListScreen().find(by: "SaleListViewController").firstMatch.shouldExist()
        }
    }

    @discardableResult
    func validateEmptyResultsItem() -> Self {
        step("Проверяем скриншот блока \"Ничего не найдено\"") {
            validateSnapshot(of: "empty_result")
        }
    }

    @discardableResult
    func swipeGalleryLeft(offer id: String) -> Self {
        step("Свайпаем галерею влево") {
            onSaleCardListScreen().find(by: "offer_image_\(id)").firstMatch.swipeLeft()
        }
    }

    @discardableResult
    func tapGalleryReportButton() -> Self {
        step("Тапаем по кнопке отчета в галерее") {
            onSaleCardListScreen().find(by: "view.snippet.show-report-btn").firstMatch.tap()
        }
    }

    @discardableResult
    func checkHasNoGallery() -> Self {
        step("Проверяем, что галереи нет на экране") {
            onSaleCardListScreen().find(by: "view.snippet.show-report-btn").firstMatch.shouldNotExist()
        }
    }

    @discardableResult
    func checkStockSnippetBodySnapshot(offerId: String, identifier: String) -> Self {
        step("Проверяем скриншот тела сниппета с id = '\(identifier)'") {
            let body = onSaleCardListScreen().find(by: "stock_offer_\(offerId)").firstMatch
            onSaleCardListScreen().scrollTo(element: body, windowInsets: .init(top: 0, left: 0, bottom: 80, right: 0))

            Snapshot.compareWithSnapshot(image: body.waitAndScreenshot().image, identifier: identifier)
        }
    }

    @discardableResult
    func tapOnStockSnippetAuctionLink() -> DealerCardSteps {
        step("Тапаем по ссылке на дилерский листинг со стокового сниппета") {
            onSaleCardListScreen().find(by: "dealer_auction_link").firstMatch.tap()
        }
        .as(DealerCardSteps.self)
    }

    @discardableResult
    func checkSegmentedControl() -> Self {
        step("Проверяем, что видим переключатель Все / Новые / С пробегом") {
            app.staticTexts["Все"].shouldExist()
            app.staticTexts["Новые"].shouldExist()
            app.staticTexts["С пробегом"].shouldExist()
        }
    }
}
