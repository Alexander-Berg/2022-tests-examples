//
//  OffersSteps.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/19/19.
//

import XCTest
import Snapshots

class OffersSteps: BaseSteps {
    func onOffersScreen() -> OffersScreen {
        return baseScreen.on(screen: OffersScreen.self)
    }

    func tapProfile() -> UserProfileSteps {
        onOffersScreen().hud.shouldNotExist(timeout: 10)
        onOffersScreen().profileButton.shouldExist(timeout: 10)

        onOffersScreen().profileButton.tap()
        return UserProfileSteps(context: context)
    }

    func tapProfileIfExists() -> UserProfileSteps {
        let button = onOffersScreen().profileButton

        // костыль для случая, когда при открытии таба показываем экран авторизации
        if button.exists {
            Thread.sleep(forTimeInterval: 0.5)
            if button.exists {
                button.tap()
            }
        }
        return UserProfileSteps(context: context)
    }

    func tapWallet() -> WalletSteps {
        onOffersScreen().hud.shouldNotExist(timeout: 10)
        onOffersScreen().walletButton.shouldExist(timeout: 10)

        onOffersScreen().walletButton.tap()
        return WalletSteps(context: context)
    }

    func tapAddOffer() -> Self {
        onOffersScreen().addOfferButton.tap()
        return self
    }

    func scrollToEnterPromocode() -> Self {
        XCTAssert(onOffersScreen().scrollTo(element: onOffersScreen().enterPromocodeButton, maxSwipes: 5))
        return self
    }

    func tapEnterPromocode() -> Self {
        onOffersScreen().enterPromocodeButton.tap()
        return self
    }

    func tapAddOfferInNavigation() -> Self {
        onOffersScreen().addOfferNavButton.tap()
        return self
    }

    func tapToCarsCategory() -> OfferEditSteps {
        onOffersScreen().labelFor(.category).tap()
        return OfferEditSteps(context: context)
    }

    func tapToCarsCategoryForWizard() -> WizardSteps {
        onOffersScreen().labelFor(.category).tap()
        return WizardSteps(context: context)
    }

    func enterNumber() -> Self {
        app.keys["1"].tap()
        app.keys["2"].tap()
        app.keys["3"].tap()
        return self
    }

    func enterText() -> Self {
        if app.keys["W"].exists {
            app.keys["C"].tap()
            app.keys["a"].tap()
            app.keys["r"].tap()
        } else {
            app.keys["К"].tap()
            app.keys["а"].tap()
            app.keys["р"].tap()
        }
        return self
    }

    func tapNextStep() -> Self {
        onOffersScreen().nextStepButton.tap()
        return self
    }

    func tapSkip() -> Self {
        onOffersScreen().skipButton.tap()
        return self
    }

    func tapSkipPanoramaPromo() -> Self {
        onOffersScreen().skipPanoramaButton.tap()
        return self
    }

    func enterEmail() -> Self {
        onOffersScreen().emailField.tap()
        wait(for: 1)
        app.keys["M"].tap()
        app.keys["@"].tap()
        app.keys["a"].tap()
        app.keys["i"].tap()
        app.keys["."].tap()
        app.keys["l"].tap()
        return self
    }

    func tapContinueStep() -> Self {
        onOffersScreen().continueButton.tap()
        return self
    }

    func scrollOnce() -> Self {
        onOffersScreen().scrollableElement.gentleSwipe(.up)
        return self
    }

    func chooseFirstPhoto() -> Self {
        if onOffersScreen().addPhoto.exists {
            onOffersScreen().addPhoto.tap()
            wait(for: 2)
            onOffersScreen().galleryPhoto.tap()
            wait(for: 2)
            onOffersScreen().doneButton.tap()
        }
        return self
    }

    func scrollToEnd() -> Self {
        onOffersScreen().scrollTo(element: onOffersScreen().lastElementOnScrollView)
        return self
    }

    func tapAddReview() -> Self {
        onOffersScreen().addReviewButton.tap()
        return self
    }

    @discardableResult
    func tapDeactivate() -> Self {
        onOffersScreen().deactivate.tap()
        return self
    }

    func tapActivate(_ identifier: String = "Активировать") -> Self {
        onOffersScreen().find(by: identifier).firstMatch.tap()
        return self
    }

    @discardableResult
    func tapDone() -> Self {
        let screen = onOffersScreen()
        let element = screen.find(by: "Готово").firstMatch
        screen.scrollTo(element: element)
        element.tap()
        return self
    }

    func selectReason(_ reason: OffersScreen.DeactivationReason) -> Self {
        onOffersScreen().deactivationReason(reason).tap()
        return self
    }

    func selectBuyerPhone() -> Self {
        onOffersScreen().buyerPhone.tap()
        return self
    }

    @discardableResult
    func exist(_ selector: String) -> Self {
        return XCTContext.runActivity(named: "Cheking existence of \(selector)") { _ in
            let element = onOffersScreen().find(by: selector).firstMatch
            element.shouldExist(timeout: 10)
            return self
        }
    }

    @discardableResult
    func notExist(_ selector: String) -> Self {
        return XCTContext.runActivity(named: "Cheking NOT existence of \(selector)") { _ in
            let element = onOffersScreen().find(by: selector).firstMatch
            element.shouldNotExist(timeout: 10)
            return self
        }
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String = #function) -> Self {
        let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.shouldExist(timeout: 10, message: "").screenshot().image
        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 1.0)
        return self
    }

    @discardableResult
    func validateSnippetSnapShot(offerID: String, snapshotId: String = #function) -> Self {
        let screen = onOffersScreen()
        let screenshot = Snapshot.screenshotCollectionView(
            fromCell: screen.find(by: "offer_\(offerID)_details").firstMatch,
            toCell: screen.find(by: "offer_\(offerID)_archiveSnippetActions").firstMatch,
            windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 16, right: 0),
            timeout: 10)
        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 1.0)
        return self
    }

    func scrollToResellerWarning() -> Self {
        let screen = onOffersScreen()
        let element = screen.find(by: "reseller_warning_cell").firstMatch
        screen.scrollTo(element: element, maxSwipes: 5, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 80, right: 0))
        return self
    }

    func tapSupportOnResselerWarning() -> ChatSteps {
        onOffersScreen()
            .find(by: "reseller_warning_cell")
            .firstMatch
            .coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
            .withOffset(CGVector(dx: 50, dy: 68))
            .press(forDuration: 0.2)
        return ChatSteps(context: context)
    }

    func tapNobody() -> Self {
        onOffersScreen().nobody.tap()
        return self
    }

    func tapAnother() -> Self {
        onOffersScreen().another.tap()
        return self
    }

    func tapNoRemember() -> Self {
        onOffersScreen().noRemember.tap()
        return self
    }

    func typeText(_ text: String) -> Self {
        app.typeText(text)
        return self
    }

    func tapClosePopUp() -> Self {
        onOffersScreen().find(by: "dismiss_modal_button").firstMatch.tap()
        return self
    }

    func tapAvito() -> Self {
        onOffersScreen().find(by: "Авито").firstMatch.tap()
        return self
    }

    func scrollToVasTrapRejectButton() -> Self {
        onOffersScreen().scrollTo(element: onOffersScreen().find(by: "Всё равно снять с публикации").firstMatch)
        return self
    }

    func tapRejectVasTrap() -> Self {
        onOffersScreen().find(by: "Всё равно снять с публикации").firstMatch.tap()
        return self
    }

    func tapSend() -> Self {
        onOffersScreen().send.tap()
        return self
    }

    func openOffer(offerId: String) -> SaleCardSteps {
        onOffersScreen().find(by: "offer_\(offerId)_details").staticTexts.firstMatch.tap()
        return SaleCardSteps(context: context)
    }

    func scrollToDriveBanner() -> Self {
        let screen = onOffersScreen()
        screen.scrollTo(element: screen.driveBanner)
        return self
    }

    func tapUserBannedSupportButton() -> ChatSteps {
        onOffersScreen().userBannedSupportButton.tap()
        return ChatSteps(context: context)
    }

    func scrollToPremiumAssistantBanner() -> Self {
        let scroller = onOffersScreen().scrollableElement
        scroller.swipeUp()
        scroller.swipeUp()
        return self
    }

    func scrollToTop() -> Self {
        let scroller = onOffersScreen().scrollableElement
        scroller.swipeDown()
        scroller.swipeDown()
        return self
    }

    @discardableResult
    func validatePremiumAssistantBannerActiveScreenshoot() -> Self {
        validateSnapShot(accessibilityId: onOffersScreen().premiumOfferAssistantActiveBanner.identifier)
        return self
    }

    @discardableResult
    func validatePremiumAssistantBannerInactiveScreenshoot() -> Self {
        validateSnapShot(accessibilityId: onOffersScreen().premiumOfferAssistantInactiveBanner.identifier)
        return self
    }

    func tapPremiumAssistantBannerButton() -> ChatSteps {
        onOffersScreen().premiumOfferAssistantBannerButton.tap()
        return ChatSteps(context: context)
    }

    @discardableResult
    func tapOnScoreBadge() -> ScoreImprovementPopupSteps {
        step("Тапаем на бейдж со скором (улучшить скор)") {
            self.onOffersScreen().findContainedText(by: "Качество объявления").firstMatch.tap()
        }
        .as(ScoreImprovementPopupSteps.self)
    }

    @discardableResult
    func validateNoEnterPromocodeButton() -> Self {
        step("Проверяем что кнопки ввода промокода нет") {
            XCTAssertFalse(onOffersScreen().scrollTo(element: onOffersScreen().enterPromocodeButton, maxSwipes: 3))
        }
    }

    @discardableResult
    func closeLoginScreenIfNeeded() -> Self {
        let closeButton = onOffersScreen().find(by: "closeButton").firstMatch
        if closeButton.exists {
            closeButton.tap()
        }
        return self
    }

    @discardableResult
    func tapTryBigPOIPromo() -> Self {
        step("Пытаемся нажать кнопку в промо.") {
            onOffersScreen().poiBigPromoTryButton.tap()
        }
    }

    @discardableResult
    func checkPanoramaLoadError() -> Self {
        step("Проверяем, что дошли до панорамы.") {
            onOffersScreen().panoramaLoadErrorMessage.shouldExist(timeout: 20, message: "Нет ошибки загрузки панорамы.")
        }
    }

    @discardableResult
    func closeGallery() -> Self {
        step("Закрываем галерею.") {
            onOffersScreen().galleryOverlayCloseButton.tap()
        }
    }

    @discardableResult
    func editOffer() -> Self {
        step("Открываем редактирование оффера.") {
            onOffersScreen().editOfferNavBarItem.tap()
        }
    }

    @discardableResult
    func tapPOILink() -> Self {
        step("Открываем панораму спецссылкой.") {
            onOffersScreen().editOfferPOILink.tap()
        }
    }

    @discardableResult
    func checkPanoramaPlayerCloseButton() -> Self {
        step("Проверяем, что дошли до экрана панорамы.") {
            onOffersScreen().panoramaPlayerScreenCloseButton.shouldExist(timeout: 10, message: "Нет экрана панорамы.")
        }
    }

    @discardableResult
    func checkAddPanoramaBannerOpened() -> Self {
        step("Проверяем, что баннер добавления панорамы показывается развёрнуто.") {
            onOffersScreen().find(by: "Добавьте\(String.nbsp)панораму. Вы получите до\(String.nbsp)2,5\(String.nbsp)раз больше\(String.nbsp)звонков").firstMatch.shouldExist()
        }
    }

    @discardableResult
    func checkAddPanoramaBannerClosed() -> Self {
        step("Проверяем, что баннер добавления панорамы показывается свёрнуто.") {
            onOffersScreen().addPanoramaBannerAnimation.shouldExist()
            onOffersScreen().find(by: "Добавьте\(String.nbsp)панораму. Вы получите до\(String.nbsp)2,5\(String.nbsp)раз больше\(String.nbsp)звонков").firstMatch.shouldNotExist()
        }
    }

    @discardableResult
    func tapCloseAddPanoramaBanner() -> Self {
        step("Нажимаем кнопку закрытия баннера добавления панорамы.") {
            onOffersScreen().closeAddPanoramaBannerButton.tap()
        }
    }

    @discardableResult
    func checkDraftAppear() -> Self {
        step("Проверяем, что драфт в списке объявлений появился.") {
            onOffersScreen().find(by: "ЧЕРНОВИК").firstMatch.shouldExist()
        }
    }

    @discardableResult
    func checkShortTitle() -> Self {
        step("Проверяем, что драфт содержит заголовок только с актуальными полями.") {
            XCTAssertTrue(onOffersScreen().userSaleInfoSnippetTitle.label == "BMW")
        }
    }

    @discardableResult
    func checkDeleteDraftMenu() -> Self {
        step("Проверяем, что меню удаления содержит верный заголовок.") {
            onOffersScreen().find(by: "Удалить").firstMatch.tap()
            onOffersScreen().find(by: "Удалить черновик?").firstMatch.shouldExist()
            onOffersScreen().find(by: "Отмена").firstMatch.tap()
        }
    }

    @discardableResult
    func checkEditDraftButton() -> Self {
        step("Проверяем, что работает переход в редактирование.") {
            onOffersScreen().find(by: "Редактировать").firstMatch.tap()
            onOffersScreen().find(by: "Объявление").firstMatch.shouldExist()
        }
    }
}

final class ScoreImprovementPopupSteps: BaseSteps {
    enum ImproveAction: String {
        case longDescription = "Добавьте описание автомобиля"
        case provenOwner = "Подтвердите владение автомобилем"
        case mosRu = "Привяжите аккаунт mos.ru"
    }

    @discardableResult
    func tapOnAction(_ action: ImproveAction) -> Self {
        step("Тапаем на пункт '\(action.rawValue)'") {
            self.baseScreen.findStaticText(by: action.rawValue).tap()
        }
    }
}
