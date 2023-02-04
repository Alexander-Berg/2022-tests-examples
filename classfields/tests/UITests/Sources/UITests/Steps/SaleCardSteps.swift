//
//  SaleCardSteps.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/17/19.
//

import XCTest
import Snapshots

final class SaleCardSteps: BaseSteps {
    func onSaleCardScreen() -> SaleCardScreen {
        return baseScreen.on(screen: SaleCardScreen.self)
    }

    func tapBack() -> SaleCardListSteps {
        onSaleCardScreen().backButton.tap()
        return SaleCardListSteps(context: context)
    }

    @discardableResult
    func checkIsFavoriteButtonSelected() -> Self {
        return validateSnapShot(accessibilityId: "action_buttons_favorite", snapshotId: "action_button_favorite_selected", message: "Сравнимваем снапшот для кнопки избранного, когда оффер в избранном")
    }

    @discardableResult
    func checkIsFavoriteButtonNotSelected() -> Self {
        return validateSnapShot(accessibilityId: "action_buttons_favorite", snapshotId: "action_button_favorite_not_selected", message: "Сравнимваем снапшот для кнопки избранного, когда оффера нет в избранном")
    }

    func scrollToSpecialOffer() -> Self {
        onSaleCardScreen().scrollTo(element: onSaleCardScreen().lastElementOnScrollView)
        return self
    }

    func scrollToCarsCatalor() -> Self {
        onSaleCardScreen().scrollTo(element: onSaleCardScreen().carsCatalogTitle)
        return self
    }

    func scrollToCharacteristics() -> Self {
        onSaleCardScreen().scrollableElement.scrollTo(element: onSaleCardScreen().characteristics, swipeDirection: .up, windowInsets: .zero)
        return self
    }

    @discardableResult
    func findVIN(_ vin: String) -> Self {
        onSaleCardScreen().find(by: vin).firstMatch.shouldExist()
        return self
    }

    func copyVIN(_ vin: String) -> Self {
        onSaleCardScreen().find(by: vin).firstMatch.tap(withNumberOfTaps: 2, numberOfTouches: 1)
        return self
    }

    @discardableResult
    func findLicensePlate(_ number: String) -> Self {
        onSaleCardScreen().find(by: number).firstMatch.shouldExist()
        return self
    }

    func scrollToReportPreview() -> SaleCardReportPreviewSteps {
        onSaleCardScreen().scrollableElement.scrollTo(element: onSaleCardScreen().reportPreviewTitle, swipeDirection: .up, windowInsets: .init(top: 0, left: 0, bottom: 250, right: 0))

        return SaleCardReportPreviewSteps(context: context)
    }

    @discardableResult
    func validateReportPreviewExtist() -> Self {
        onSaleCardScreen().reportPreviewTitle.shouldExist()
        return self
    }

    @discardableResult
    func scrollToReportBuySingleButton(windowInsets: UIEdgeInsets = .zero) -> Self {
        onSaleCardScreen().scrollableElement.scrollTo(
            element: onSaleCardScreen().reportBuySingleButton,
            swipeDirection: .up,
            windowInsets: windowInsets
        )
        return self
    }

    func scrollToReportBuyBundleButton(size: Int) -> Self {
        onSaleCardScreen().scrollableElement.scrollTo(
            element: onSaleCardScreen().reportBuyBundleButton(size: size),
            swipeDirection: .up
        )
        return self
    }

    @discardableResult
    func scrollToDealerSubsriptionButton() -> Self {
        Step("Скроллим до кнопки подписки на дилера") {
            onSaleCardScreen().scrollTo(
                element: onSaleCardScreen().dealerSubscriptionButton,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 120, right: 0),
                swipeDirection: .up
            )
        }
        return self
    }

    @discardableResult
    func tapDealerSubsriptionButton() -> Self {
        Step("Жмак в кнопку подписки на дилера") {
            onSaleCardScreen().dealerSubscriptionButton.tap()
        }
        return self
    }

    @discardableResult
    func validateDealerSubscriptionButton(hasSubscription: Bool) -> Self {
        Step("Проверяем, что кнопка подписки на дилера в состоянии \(hasSubscription ? "'есть подписка'" : "'нет подписки'")") {
            onSaleCardScreen().dealerSubscriptionButton.shouldExist()
            if hasSubscription {
                onSaleCardScreen().findStaticText(by: "Вы подписаны на дилера").shouldExist()
            } else {
                onSaleCardScreen().findStaticText(by: "Подписаться на объявления").shouldExist()
            }
        }
        return self
    }

    @discardableResult
    func scrollToDealerListingButton() -> Self {
        Step("Скроллим до кнопки листинга дилера") {
            onSaleCardScreen().scrollTo(element: onSaleCardScreen().dealerListingButton, swipeDirection: .up)
        }
        return self
    }

    @discardableResult
    func tapDealerListingButton() -> DealerCardSteps {
        Step("Жмак в кнопку листинга дилера") {
            onSaleCardScreen().dealerListingButton.tap()
        }
        return DealerCardSteps(context: context)
    }

    @discardableResult
    func validateDealerListingButtonTitle(count: Int) -> Self {
        Step("Проверяем текст на кнопке листинга дилера") {
            onSaleCardScreen().dealerListingButton.shouldExist()
            onSaleCardScreen().findStaticText(by: "\(count) авто в наличии").shouldExist()
        }
        return self
    }

    func tapReportBuySingleButton() -> PaymentOptionsSteps<SaleCardSteps> {
        Step("Нажимаем на кнопку покупки 1 отчета") {
            onSaleCardScreen().reportBuySingleButton.tap()
        }
        return PaymentOptionsSteps(context: context, source: self)
    }

    @discardableResult
    func checkReportBuySingleButtonNotExist() -> Self {
        step("Проверяем, что кнопка покупки одного отчета не отображается") {
            onSaleCardScreen().reportBuySingleButton.shouldNotExist()
        }
    }

    func tapReportBuyBundleButton(size: Int) -> Self {
        onSaleCardScreen().reportBuyBundleButton(size: size).tap()
        return self
    }

    func scrollToDriveBanner() -> Self {
        let screen = onSaleCardScreen()
        screen.scrollTo(
            element: screen.driveBanner,
            maxSwipes: 12
        )

        return self
    }

    func scrollToSameButNewOffers() -> Self {
        let screen = onSaleCardScreen()
        screen.scrollTo(
            element: screen.sameButNewSection,
            maxSwipes: 14
        )
        return self
    }

    func snapshotSameButNewOffers() -> UIImage {
        return scrollToSameButNewOffers()
            .onSaleCardScreen().sameButNewSection
            .waitAndScreenshot().image
    }

    func tapSameMarkModelOffer(at index: Int) -> Self {
        let screen = onSaleCardScreen()
        screen.find(by: "same-mm-offer_\(index)").firstMatch.tap()
        return self
    }

    func tapSameMarkModelShowAll() -> StockCardSteps {
        let screen = onSaleCardScreen()
        screen.find(by: "btn.same-mm-show-all").firstMatch.tap()
        return StockCardSteps(context: context)
    }

    func snapshotHeader() -> UIImage {
        return onSaleCardScreen().find(by: "header").firstMatch.waitAndScreenshot().image
    }

    func scrollToTutorial() -> Self {
        let screen = onSaleCardScreen()
        screen.scrollTo(
            element: screen.tutorialHeader,
            maxSwipes: 12
        )

        return self
    }

    func scrollToCompectation() -> Self {
        Step("Скролл до блока с названием комплектации") {
            let screen = onSaleCardScreen()
            screen.scrollTo(
                element: screen.complectationHeader,
                maxSwipes: 12,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 64, right: 0)
            )
        }

        return self
    }

    func tapOnCompectationComparisonLink() -> ComplectationComparisonSteps {
        Step("Тап по ссылке на сравнение комплектаций") {
            onSaleCardScreen().complectationComparisonLink.tap()
        }

        return ComplectationComparisonSteps(context: self.context)
    }

    @discardableResult
    override func exist(selector: String) -> Self {
        return XCTContext.runActivity(named: "Cheking existion of \(selector)") { _ in
            let element = onSaleCardScreen().find(by: selector).firstMatch
            element.shouldExist(timeout: Const.timeout)
            return self
        }
    }

    @discardableResult
    override func notExist(selector: String) -> Self {
        return XCTContext.runActivity(named: "Cheking not existion of \(selector)") { _ in
            let element = onSaleCardScreen().find(by: selector).firstMatch
            element.shouldNotExist(timeout: Const.timeout)
            return self
        }
    }

    @discardableResult
    func likeTap() -> Self {
        Step("Тапаем на кнопку Избранного") {
            onSaleCardScreen().likeButton.tap()
        }
        return self
    }

    @discardableResult
    func tapOnGreatDealBadge() -> OfferPriceSteps {
        step("Тапаем на бейдж грейт-дила") {
            self.onSaleCardScreen().greatDealBadge.tap()
        }
        .as(OfferPriceSteps.self)
    }

    func dealerNameTap() -> Self {
        onSaleCardScreen().find(by: "DealerInfoLayout").firstMatch.tap()
        return self
    }

    @discardableResult
    func dealerOfferCountTap() -> DealerCardSteps {
        onSaleCardScreen().find(by: "dealerOffersCount").firstMatch.tap()
        return DealerCardSteps(context: context)
    }

    @discardableResult
    func scrollTo(_ selector: String, maxSwipes: Int = 10, windowInsets: UIEdgeInsets = .zero, useLongSwipes: Bool = false, longSwipeAdjustment: Double = 0.4) -> Self {
        let element: XCUIElement = onSaleCardScreen().find(by: selector).firstMatch
        onSaleCardScreen().scrollTo(element: element, maxSwipes: maxSwipes, windowInsets: windowInsets, useLongSwipes: useLongSwipes, longSwipeAdjustment: longSwipeAdjustment)
        return self
    }

    @discardableResult
    func notExist(_ selector: String, maxSwipes: Int = 5, windowInsets: UIEdgeInsets = .zero) -> Self {
        let element: XCUIElement = onSaleCardScreen().find(by: selector).firstMatch
        let exist = onSaleCardScreen().scrollTo(element: element, maxSwipes: maxSwipes, windowInsets: windowInsets)
        XCTAssertFalse(exist)
        return self
    }

    @discardableResult
    func swipeUp() -> Self {
        onSaleCardScreen().swipe(.up)
        return self
    }

    @discardableResult
    func swipeDown() -> Self {
        onSaleCardScreen().swipe(.down)
        return self
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "", onFail: ((SaleCardSteps) -> Void)? = nil) -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let element = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch
            element.shouldExist()
            let screenshot = element.screenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16), file: "SaleCardTests")
            return self
        }
    }

    func tapCreditOfferButton() -> PreliminaryCreditSteps {
        onSaleCardScreen().creditOfferButton.tap()
        return PreliminaryCreditSteps(context: context)
    }

    func openPreliminarySteps() -> PreliminaryCreditSteps {
        return PreliminaryCreditSteps(context: context)
    }

    func tapOnSelectCarForCredit() -> Self {
        onSaleCardScreen().find(by: "select_auto_button").firstMatch.tap()
        return self
    }

    func continueSharkCredit() -> SharkFullFormSteps {
        let element = onSaleCardScreen().find(by: "Продолжить заполнение").firstMatch
        onSaleCardScreen().scrollTo(element: element, maxSwipes: 10, windowInsets: .init(top: 0, left: 0, bottom: 64, right: 0))
        element.tap()
        return SharkFullFormSteps(context: context)
    }

    @discardableResult
    func tapOnConfirm() -> Self {
        let element = onSaleCardScreen().find(by: "Понятно").firstMatch
        element.tap()
        return self
    }

    @discardableResult
    func checkHeaderHasOnly(labels: [String]) -> Self {
        Step("Провярем что из лейблов на хедере карточки только `\(labels)`") {
            let headerLabels = onSaleCardScreen().headerView.staticTexts
            XCTAssertEqual(labels.count, headerLabels.count)
            labels.forEach { headerLabels[$0].shouldExist() }
        }
        return self
    }

    func openGallery() -> Self {
        onSaleCardScreen().gallery.tap()
        return self
    }

    func openReport() -> ReportCreditSteps {
        onSaleCardScreen().scrollTo(element: onSaleCardScreen().reportButton)
        onSaleCardScreen().reportButton.tap()
        return ReportCreditSteps(context: context)
    }

    func tapOnApprovedSellerBadge() -> Self {
        onSaleCardScreen().approvedSellerBadge.tap()
        return self
    }

    func tapOnAdvantages() -> Self {
        onSaleCardScreen().advantagesBadge.tap()
        return self
    }

    @discardableResult
    func tapApprovedSellerDialogButton() -> Self {
        onSaleCardScreen().approvedSellerDialogButton.tap()
        return self
    }

    func tapOnBookedBanner() -> BookingStatusSteps {
        onSaleCardScreen().bookingBanner.tap()
        return self.as(BookingStatusSteps.self)
    }

    @discardableResult
    func tapPhone() -> SaleCardSteps {
        onSaleCardScreen().phoneButton.tap()
        return self
    }

    @discardableResult
    func tapChat() -> ChatSteps {
        onSaleCardScreen().chatButton.tap()
        return self.as(ChatSteps.self)
    }

    func tapChatPreset(_ preset: String) -> ChatSteps {
        return tap(preset)
            .tap("Отправить")
            .as(ChatSteps.self)
    }

    @discardableResult
    func tapBookButton() -> OfferBookingSteps {
        onSaleCardScreen().bookButton.tap()
        return self.as(OfferBookingSteps.self)
    }

    @discardableResult
    func booked(byMe: Bool) -> Self {
        let bookedTitle: String = byMe ? "Забронирован вами" : "Автомобиль забронирован"
        let title = app.staticTexts[bookedTitle].firstMatch
        title.shouldExist()
        return self
    }

	func tapOnPriceHistory() -> OfferPriceSteps {
        step("Тапаем на историю изменеия цены") {
            self.onSaleCardScreen().priceHistoryButton.tap()
        }
        .as(OfferPriceSteps.self)
    }

    @discardableResult
    func openBestPriceRequestForm() -> GetBestPriceSteps {
        onSaleCardScreen().scrollTo(element: onSaleCardScreen().newCarRequestBanner)
        onSaleCardScreen().newCarRequestBanner.tap()
        return GetBestPriceSteps(context: context)
    }

    @discardableResult
    func checkAdvantagesWithScore() -> Self {
        step("Проверяем, что в блоке преимуществ есть скор и он на второй позиции (после собственника)") {
            self.onSaleCardScreen().scrollTo(
                element: self.onSaleCardScreen().advantagesBadge,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0)
            )
            let screenshot = self.onSaleCardScreen().advantagesBadge.waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: "advantages_with_score")
        }
    }

    @discardableResult
    func tapOnScoreBadge() -> OfferAdvantageSteps<SaleCardScreen> {
        Step("Тапаем на бейдж со скором в преимуществах") {
            self.onSaleCardScreen().scoreBadge.tap()
        }

        return OfferAdvantageSteps<SaleCardScreen>(context: context, source: onSaleCardScreen())
    }

    @discardableResult
    func shouldNotSeeScoreBadge() -> Self {
        step("Проверяем, что бейджа со скором нет в преимуществах") {
            self.onSaleCardScreen().scoreBadge.shouldNotExist()
        }
    }

    @discardableResult
    func scrollAndTapOnScore() -> HealthScorePopupSteps {
        step("Скроллим и тапаем на иконку health скора в превью отчета") {
            let element = self.onSaleCardScreen().find(by: "pro_auto_preview_badge").firstMatch
            self.onSaleCardScreen().scrollTo(element: element).tap()
        }
        .as(HealthScorePopupSteps.self)
    }

    func checkScreenLoaded() -> Self {
        step("Проверяем, что карточка загрузилась") {
            self.onSaleCardScreen().headerView.shouldExist(timeout: 10, message: "Экран оффера не загрузился")
        }
    }

    func scrollToPremiumAssistantBanner() -> Self {
        onSaleCardScreen().scrollableElement.swipeUp()
        onSaleCardScreen().premiumOfferAssistantActiveBanner.shouldExist()
        return self
    }

    @discardableResult
    func chackPremiumAssistantInactiveBannerExists() -> Self {
        onSaleCardScreen().premiumOfferAssistantInactiveBanner.shouldExist()
        return self
    }

    func tapPremiumAssistantBannerButton() -> ChatSteps {
        onSaleCardScreen().premiumOfferAssistantBannerButton.tap()
        return ChatSteps(context: context)
    }

    @discardableResult
    func checkOfferBanned() -> Self {
        onSaleCardScreen().find(by: "Объявление было заблокировано или удалено").firstMatch.shouldExist()
        return self
    }

    @discardableResult
    func checkOfferNotBanned() -> Self {
        wait(for: 1)
        onSaleCardScreen()
            .find(by: "Объявление было заблокировано или удалено")
            .firstMatch
            .shouldNotExist()
        return self
    }

    @discardableResult
    func scrollAndTapOnShowReportButton() -> CarReportPreviewSteps {
        step("Скроллим до кнопки открытия отчета и тапаем") {
            let element = self.onSaleCardScreen().showFullReportButton
            self.onSaleCardScreen().scrollTo(element: element).tap()
        }
        .as(CarReportPreviewSteps.self)
    }

    @discardableResult
    func scrollToShowReportButton() -> Self {
        step("Скролим до кнопки открытия отчета") {
            let element = self.onSaleCardScreen().showFullReportButton
            self.onSaleCardScreen().scrollTo(element: element)
        }
        return self
    }

    @discardableResult
    func checkShowFullReportbuttonIsDisplayed() -> Self {
        step("Проверяем, что на карточке отображается кнопка [Смотреть полный отчет]") {
            onSaleCardScreen().showFullReportButton.shouldExist()
        }
        return self
    }

    @discardableResult
    func tapActivate() -> Self {
        step("Нажимаем активацию оффера") {
            onSaleCardScreen().find(by: "offer_main_button").firstMatch.tap()
        }
    }

    @discardableResult
    func tapOnCompareButton() -> Self {
        step("Тапаем на кнопку добавления в сравнение") {
            self.onSaleCardScreen().compareButton.tap()
        }
    }

    @discardableResult
    func checkCompareButton(isOn: Bool) -> Self {
        step("Проверяем статус кнопки 'Сравнить': ожидается \(isOn ? "'активно'" : "'неактивно'")") {
            self.onSaleCardScreen().scrollTo(
                element: self.onSaleCardScreen().compareButton,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 64, right: 0),
                swipeDirection: .down
            )
            let screenshot = self.onSaleCardScreen().compareButton.waitAndScreenshot()

            Snapshot.compareWithSnapshot(
                image: screenshot.image,
                identifier: SnapshotIdentifier(
                    suite: "SaleCardTests",
                    identifier: isOn ? "compare_button_active" : "compare_button_inactive"
                )
            )
        }
    }

    @discardableResult
    func tapOnBackButton() -> Self {
        step("Тапаем на кнопку Назад") {
            self.onSaleCardScreen().backButton.tap()
        }
    }

    @discardableResult
    func checkComparisonSnackbar(isInComparison: Bool) -> Self {
        step("Проверяем снекбар после \(isInComparison ? "добавления" : "удаления") оффера") {
            if isInComparison {
                XCTAssertEqual(self.onSaleCardScreen().snackbarTitle.label, "Добавлено к сравнению")
                XCTAssertEqual(self.onSaleCardScreen().snackbarButton.label, "Перейти")
            } else {
                XCTAssertEqual(self.onSaleCardScreen().snackbarTitle.label, "Удалено из сравнения")
                XCTAssertEqual(self.onSaleCardScreen().snackbarButton.label, "Вернуть")
            }
        }
    }

    @discardableResult
    func tapOnSnackbarButton() -> Self {
        step("Тапаем на кнопку в снекбаре") {
            self.onSaleCardScreen().snackbarButton.tap()
        }
    }

    @discardableResult
    func checkAddPanoramaBannerOpened() -> Self {
        step("Проверяем, что баннер добавления панорамы показывается развёрнуто.") {
            onSaleCardScreen().find(by: "Добавьте\(String.nbsp)панораму\(String.nbsp)— получите\(String.nbsp)×2,5\(String.nbsp)звонков").firstMatch.shouldExist()
        }
    }

    @discardableResult
    func checkAddPanoramaBannerClosed() -> Self {
        step("Проверяем, что баннер добавления панорамы показывается свёрнуто.") {
            onSaleCardScreen().addPanoramaBannerAnimation.shouldExist()
            onSaleCardScreen().find(by: "Добавьте\(String.nbsp)панораму\(String.nbsp)— получите\(String.nbsp)×2,5\(String.nbsp)звонков").firstMatch.shouldNotExist()
        }
    }

    @discardableResult
    func tapCloseAddPanoramaBanner() -> Self {
        step("Нажимаем кнопку закрытия баннера добавления панорамы.") {
            onSaleCardScreen().closeAddPanoramaBannerButton.tap()
        }
    }
}

final class BookingStatusSteps: BaseSteps {
    @discardableResult
    func hasTitle(_ title: String) -> Self {
        let labels = app.descendants(matching: .staticText)
        labels[title].shouldExist()

        return self
    }

    @discardableResult
    func hasSubtitle(_ subtitle: String) -> Self {
        let predicate = NSPredicate(format: "label LIKE %@", subtitle)
        let label = app.descendants(matching: .staticText).matching(predicate)
        label.firstMatch.shouldExist()

        return self
    }
}
