//
//  UserOffersCardSteps.swift
//  UITests
//
//  Created by Dmitry Barillo on 15/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class UserOffersCardSteps {
    lazy var mosRuConnectionPanel: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.mosRuConnectionPanel)

    // MARK: Common

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана карточки пользовательского оффера") { _ -> Void in
            self.cardView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnBackButton() -> Self {
        XCTContext.runActivity(named: "Закрываем экран карточки пользовательского оффера") { _ -> Void in
            let backButton = ElementsProvider.obtainBackButton()
            backButton.yreTap()
        }
        return self
    }

    // MARK: Actions

    func openOfferPreview() {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Как увидят моё объявление другие\"") { _ -> Void in
            self.offerPreviewButton.tap()
        }
    }

    func openSupport() {
        XCTContext.runActivity(named: "Нажимаем на кнопку чата с техподдержкой") { _ -> Void in
            self.openSupportButton.tap()
        }
    }

    @discardableResult
    func scrollToProlongateButton() -> Self {
        XCTContext.runActivity(named: "Скроллим до кнопки \"Продлить размещение\"") { _ -> Void in
            self.screenView.scrollToElement(element: self.prolongateButton, direction: .up)
        }
        return self
    }
    
    @discardableResult
    func activateOptionTurbo() -> PaymentMethodsSteps {
        self.activateProductOption("Турбо", using: self.turboButton)
    }
    
    @discardableResult
    func activateOptionPremium() -> PaymentMethodsSteps {
        self.activateProductOption("Премиум", using: self.premiumButton)
    }
    
    @discardableResult
    func activateOptionRaising() -> PaymentMethodsSteps {
        self.activateProductOption("Поднятие", using: self.raisingButton)
    }
    
    @discardableResult
    func activateOptionPromotion() -> PaymentMethodsSteps {
        self.activateProductOption("Продвижение", using: self.promotionButton)
    }
    
    @discardableResult
    func openTurboScreen() -> UserOfferProductInfoViewSteps {
        self.openOptionScreen("Турбо", using: self.turboView)
    }
    
    @discardableResult
    func openPremiumScreen() -> UserOfferProductInfoViewSteps {
        self.openOptionScreen("Премиум", using: self.premiumView)
    }
    
    @discardableResult
    func openRaisingScreen() -> UserOfferProductInfoViewSteps {
        self.openOptionScreen("Поднятия", using: self.raisingView)
    }

    @discardableResult
    func openPromotionScreen() -> UserOfferProductInfoViewSteps {
        self.openOptionScreen("Продвижение", using: self.promotionView)
    }
    
    @discardableResult
    func scrollToPublishButton() -> Self {
        XCTContext.runActivity(named: "Скроллим до кнопки \"Опубликовать\"") { _ -> Void in
            self.screenView.scrollToElement(element: self.publishButton, direction: .up)
        }
        return self
    }
    
    @discardableResult
    func scrollToUnpublishButton() -> Self {
        XCTContext.runActivity(named: "Скроллим до кнопки \"Снять с публикации\"") { _ -> Void in
            self.screenView.scrollToElement(element: self.unpublishButton, direction: .up)
        }
        return self
    }

    @discardableResult
    func scrollToRemoveButton() -> Self {
        XCTContext.runActivity(named: "Скроллим до кнопки \"Удалить\"") { _ -> Void in
            self.screenView.scrollToElement(element: self.removeButton, direction: .up)
        }
        return self
    }
    
    @discardableResult
    func scrollToPreviewButton() -> Self {
        XCTContext.runActivity(named: "Скроллим до кнопки \"Как увидят моё объявление другие\"") { _ -> Void in
            self.screenView.scroll(to: self.offerPreviewButton,
                                   // We have a Navigation panel at the top and a Edit button at the bottom
                                   adjustInteractionFrame: { $0.insetBy(dx: 0, dy: 90) },
                                   velocity: 0.3,
                                   swipeLimits: 10)
        }
        return self
    }
    
    @discardableResult
    func scrollToOpenSupportButton() -> Self {
        XCTContext.runActivity(named: "Скроллим до кнопки чата с техподдержкой") { _ -> Void in
            self.screenView.scrollToElement(element: self.openSupportButton, direction: .up)
        }
        return self
    }

    @discardableResult
    func tapOnPublishButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Опубликовать\"") { _ -> Void in
            self.publishButton.tap()
        }
        return self
    }

    @discardableResult
    func tapOnActivateButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Активировать\"") { _ -> Void in
            self.activateButton.tap()
        }
        return self
    }

    // MARK: Top Controls

    @discardableResult
    func isPriceEditIconNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие иконки редактирования цены") { _ -> Void in
            self.priceEditIcon.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isPriceEditIconTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность иконки редактирования цены") { _ -> Void in
            self.priceEditIcon.yreEnsureExists()
            self.priceEditButton.yreEnsureEnabled()
            self.priceEditButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isPriceEditButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки редактирования цены") { _ -> Void in
            self.priceEditButton.yreEnsureExists()
            self.priceEditButton.yreEnsureEnabled()
            self.priceEditButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isPriceEditButtonDisabled() -> Self {
        XCTContext.runActivity(named: "Проверяем недоступность кнопки редактирования цены") { _ -> Void in
            self.priceEditButton.yreEnsureExists()
            self.priceEditButton.yreEnsureHittable()
            self.priceEditButton.yreEnsureNotEnabled()
        }
        return self
    }

    // MARK: Bottom Controls

    @discardableResult
    func isProlongateButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки \"Продлить размещение\"") { _ -> Void in
            self.prolongateButton.yreEnsureExists()
            self.prolongateButton.yreEnsureEnabled()
            self.prolongateButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isOpenSupportButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки чата с техподдержкой") { _ -> Void in
            self.openSupportButton.yreEnsureExists()
            self.openSupportButton.yreEnsureHittable()
            self.openSupportButton.yreEnsureEnabled()
        }
        return self
    }

    @discardableResult
    func isProlongationButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"Продлить размещение\"") { _ -> Void in
            self.prolongateButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isPublishButton(tappable: Bool) -> Self {
        let tappableString = (tappable ? "активность" : "недоступность")
        XCTContext.runActivity(named: "Проверяем \(tappableString) кнопки \"Опубликовать\"") { _ -> Void in
            if tappable {
                self.publishButton.yreEnsureExists()
                self.publishButton.yreEnsureEnabled()
                self.publishButton.yreEnsureHittable()
            }
            else {
                self.publishButton.yreEnsureNotExists()
            }
        }
        return self
    }

    @discardableResult
    func isUnpublishButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки \"Снять с публикации\"") { _ -> Void in
            self.unpublishButton.yreEnsureExists()
            self.unpublishButton.yreEnsureEnabled()
            self.unpublishButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isUnpublishButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"Снять с публикации\"") { _ -> Void in
            self.unpublishButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isRemoveButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки \"Удалить\"") { _ -> Void in
            self.removeButton.yreEnsureExists()
            self.removeButton.yreEnsureEnabled()
            self.removeButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isRemoveButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"Удалить\"") { _ -> Void in
            self.removeButton.yreEnsureNotExists()
        }
        return self
    }
    
    @discardableResult
    func isShareButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие нажимаемой кнопки \"Поделиться\"") { _ -> Void in
            self.shareButton.yreEnsureExists()
            self.shareButton.yreEnsureEnabled()
            self.shareButton.yreEnsureHittable()
        }
        return self
    }
    
    @discardableResult
    func isShareButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"Поделиться\"") { _ -> Void in
            self.shareButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isOfferPreviewButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки \"Как увидят моё объявление другие\"") { _ -> Void in
            self.offerPreviewButton.yreEnsureExists()
            self.offerPreviewButton.yreEnsureEnabled()
            self.offerPreviewButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isAddPhotosButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки добавления фото") { _ -> Void in
            self.addPhotosButton.yreEnsureExists()
            self.addPhotosButton.yreEnsureEnabled()
            self.addPhotosButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isAddPhotosButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки добавления фото") { _ -> Void in
            self.addPhotosButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isEditButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки \"Редактировать\"") { _ -> Void in
            self.editButton.yreEnsureExists()
            self.editButton.yreEnsureEnabled()
            self.editButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isEditButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"Редактировать\"") { _ -> Void in
            self.editButton.yreEnsureNotExists()
        }
        return self
    }

    // MARK: Placement

    @discardableResult
    func isActivePlacementView(presented: Bool) -> Self {
        let presentedString = (presented ? "наличие" : "отсутствие")
        XCTContext.runActivity(named: "Проверяем \(presentedString) плашки активного размещения") { _ -> Void in
            if presented {
                self.activePlacementView.yreEnsureExists()
            }
            else {
                self.activePlacementView.yreEnsureNotExists()
            }
        }
        return self
    }

    @discardableResult
    func isInactivePlacementView(presented: Bool) -> Self {
        let presentedString = (presented ? "наличие" : "отсутствие")
        XCTContext.runActivity(named: "Проверяем \(presentedString) плашки неактивного размещения") { _ -> Void in
            if presented {
                self.inactivePlacementView.yreEnsureExists()
            }
            else {
                self.inactivePlacementView.yreEnsureNotExists()
            }
        }
        return self
    }

    @discardableResult
    func isActivateButton(tappable: Bool) -> Self {
        let tappableString = (tappable ? "активность" : "недоступность")
        XCTContext.runActivity(named: "Проверяем \(tappableString) кнопки \"Активировать\"") { _ -> Void in
            if tappable {
                self.activateButton.yreEnsureExists()
                self.activateButton.yreEnsureEnabled()
                self.activateButton.yreEnsureHittable()
            }
            else {
                self.activateButton.yreEnsureNotExists()
            }
        }
        return self
    }

    @discardableResult
    func isSuccessNotificationViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие плашки об успехе операции") { _ -> Void in
            self.notificationView.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSuccessNotificationViewDismissedAfterDelay(_ delay: TimeInterval = Constants.timeout) -> Self {
        XCTContext.runActivity(named: "Проверяем исчезновение плашки об успехе операции") { _ -> Void in
            self.notificationView.yreEnsureNotVisibleWithTimeout(timeout: delay)
        }
        return self
    }

    func errorAlert() -> AnyAlertSteps {
        return AnyAlertSteps(elementType: .alert,
                             alertID: Identifiers.errorAlert)
    }

    // MARK: MosRu Connection

    @discardableResult
    func isMosRuConnectionPanelPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие плашки для подключения mos.ru") { _ -> Void in
            self.mosRuConnectionPanel.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isMosRuConnectionPanelNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие плашки для подключения mos.ru") { _ -> Void in
            self.mosRuConnectionPanel.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = UserOfferCardAccessibilityIdentifiers
    
    private lazy var turboView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.TurboIdentifiers.viewIdentifier
    )
    private lazy var turboButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.TurboIdentifiers.activateButton
    )
    private lazy var premiumView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.ProductsIdentifiers.premiumView
    )
    private lazy var premiumButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.ProductsIdentifiers.premiumPayButton
    )
    private lazy var raisingView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.ProductsIdentifiers.raisingView
    )
    private lazy var raisingButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.ProductsIdentifiers.raisingPayButton
    )
    private lazy var promotionButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.ProductsIdentifiers.promotionPayButton
    )
    private lazy var promotionView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.ProductsIdentifiers.promotionView
    )
    private lazy var cardView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.card
    )
    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.view
    )
    private lazy var prolongateButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.prolongateButton
    )
    private lazy var openSupportButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.openSupportButton
    )
    private lazy var publishButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.publishButton
    )
    private lazy var unpublishButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.unpublishButton
    )
    private lazy var removeButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.removeButton
    )
    private lazy var shareButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.shareButton
    )
    private lazy var offerPreviewButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.offerPreviewButton
    )
    private lazy var addPhotosButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.addPhotosButton
    )
    private lazy var editButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.editButton
    )
    private lazy var priceEditButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.priceEditButton
    )
    private lazy var priceEditIcon: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.priceEditIcon
    )
    private lazy var activePlacementView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.activePlacementView
    )
    private lazy var inactivePlacementView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.inactivePlacementView
    )
    private lazy var activateButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.activateButton
    )
    private lazy var notificationView: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.notificationView
    )
    
    @discardableResult
    private func activateProductOption(_ title: String, using element: XCUIElement) -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку покупки \(title)") { _ -> Void in
            self.screenView.scrollToElement(element: element, direction: .up)
            element
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .yreTap()
        }
        return PaymentMethodsSteps()
    }
    
    @discardableResult
    private func openOptionScreen(_ title: String, using element: XCUIElement) -> UserOfferProductInfoViewSteps {
        XCTContext.runActivity(named: "Открываем экран покупки \(title)") { _ -> Void in
            self.screenView.scrollToElement(element: element, direction: .up)
            element
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .yreTap()
        }
        return UserOfferProductInfoViewSteps()
    }
}
