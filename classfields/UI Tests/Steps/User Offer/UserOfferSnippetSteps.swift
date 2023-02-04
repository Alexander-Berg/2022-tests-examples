//
//  UserOfferSnippetSteps.swift
//  UITests
//
//  Created by Dmitry Barillo on 15/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class UserOfferSnippetSteps {
    init(element: XCUIElement, scrollView: XCUIElement) {
        self.snippetView = element
        self.scrollView = scrollView
    }

    func tap() {
        XCTContext.runActivity(named: "Нажимаем на сниппет в ЛК") { _ -> Void in
            self.snippetView.tap()
        }
    }
    
    func tapOnPublishButton() {
        XCTContext.runActivity(named: "Нажимаем на кнопку публикации") { _ -> Void in
            self.publishButton.tap()
        }
    }

    @discardableResult
    func isViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие сниппета в ЛК") { _ -> Void in
            self.snippetView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isEditOfferButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки редактирования") { _ -> Void in
            self.editOfferButton.yreEnsureExists()
            XCTAssertTrue(self.editOfferButton.isEnabled)
            XCTAssertTrue(self.editOfferButton.isHittable)
        }
        return self
    }

    @discardableResult
    func isRemoveButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки удаления") { _ -> Void in
            self.removeButton.yreEnsureExists()
            XCTAssertTrue(self.removeButton.isEnabled)
            XCTAssertTrue(self.removeButton.isHittable)
        }
        return self
    }

    @discardableResult
    func isPublishButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки публикации") { _ -> Void in
            self.publishButton.yreEnsureExists()
            XCTAssertTrue(self.publishButton.isEnabled)
            XCTAssertTrue(self.publishButton.isHittable)
        }
        return self
    }

    @discardableResult
    func isProlongateButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки продления размещения") { _ -> Void in
            self.prolongateButton.yreEnsureExists()
            XCTAssertTrue(self.prolongateButton.isEnabled)
            XCTAssertTrue(self.prolongateButton.isHittable)
        }
        return self
    }

    @discardableResult
    func isAddPhotoButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки добавления фото") { _ -> Void in
            self.addPhotoButton.yreEnsureExists()
            XCTAssertTrue(self.addPhotoButton.isEnabled)
            XCTAssertTrue(self.addPhotoButton.isHittable)
        }
        return self
    }

    @discardableResult
    func isEditPriceButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность кнопки изменения цены") { _ -> Void in
            self.editPriceButton.yreEnsureExists()
            XCTAssertTrue(self.editPriceButton.isEnabled)
            XCTAssertTrue(self.editPriceButton.isHittable)
        }
        return self
    }

    @discardableResult
    func isPriceEditControlTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем активность контрола изменения цены") { _ -> Void in
            self.priceEditControl.yreEnsureExists()
            XCTAssertTrue(self.priceEditControl.isEnabled)
            XCTAssertTrue(self.priceEditControl.isHittable)
        }
        return self
    }
    
    @discardableResult
    func openCard() -> UserOffersCardSteps {
        XCTContext.runActivity(named: "Открываем карточку") { _ -> Void in    
            let coordinate: XCUICoordinate = self.snippetView.coordinate(withNormalizedOffset: CGVector(dx: 0.0, dy: 0.0))
            coordinate.tap()
        }
        return UserOffersCardSteps.init()
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом ячейки списка в ЛК") { _ -> Void in
            let screenshot = self.snippetView.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    // MARK: - VAS Turbo

    @discardableResult
    func isTurboContainerViewTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что контейнер c " + Consts.turbo + " нажимается") { _ -> Void in
            self.turboContainerView.yreEnsureExists()
            XCTAssertTrue(self.turboContainerView.isEnabled)
            XCTAssertTrue(self.turboContainerView.isHittable)
        }
        return self
    }

    @discardableResult
    func tapTurboContainerView() -> UserOfferProductInfoViewSteps {
        XCTContext.runActivity(named: "Нажимаем на контейнер c " + Consts.turbo) { _ -> Void in
            self.turboContainerView.yreEnsureExists()
                .tap()
        }
        return UserOfferProductInfoViewSteps()
    }

    @discardableResult
    func isTurboActivateButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что нажимается кнопка, ведущая на покупку " + Consts.turbo) { _ -> Void in
            self.turboActivateButton.yreEnsureExists()
            XCTAssertTrue(self.turboActivateButton.isEnabled)
            XCTAssertTrue(self.turboActivateButton.isHittable)
        }
        return self
    }

    @discardableResult
    func tapTurboActivateButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку, ведущую на покупку " + Consts.turbo) { _ -> Void in
            self.turboActivateButton.yreEnsureExists()
                .tap()
        }
        return PaymentMethodsSteps()
    }

    @discardableResult
    func compareTurboViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом продукта Турбо в ЛК") { _ -> Void in
            self.scrollToElement(element: self.turboContainerView)
            self.turboContainerView.yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: - VAS Activated Turbo

    @discardableResult
    func isActivatedTurboContainerViewTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что контейнер c " + Consts.activatedTurbo + " нажимается") { _ -> Void in
            self.activatedTurboContainerView
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureVisible()
        }
        return self
    }

    @discardableResult
    func tapActivatedTurboContainerView() -> UserOfferProductInfoViewSteps {
        XCTContext.runActivity(named: "Нажимаем на контейнер c " + Consts.activatedTurbo) { _ -> Void in
            self.activatedTurboContainerView.yreEnsureExists()
                .tap()
        }
        return UserOfferProductInfoViewSteps()
    }

    @discardableResult
    func isActivatedTurboActivateButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что нажимается кнопка, ведущая на покупку " + Consts.activatedTurbo) { _ -> Void in
            self.activatedTurboActivateButton
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureVisible()
        }
        return self
    }

    @discardableResult
    func tapActivatedTurboActivateButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку, ведущую на покупку " + Consts.activatedTurbo) { _ -> Void in
            self.activatedTurboActivateButton.yreEnsureExists()
                .tap()
        }
        return PaymentMethodsSteps()
    }

    @discardableResult
    func compareActivatedTurboViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом активированного продукта Турбо в ЛК") { _ -> Void in
            self.scrollToElement(element: self.activatedTurboContainerView)
            self.activatedTurboContainerView.yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: - VAS Raising

    @discardableResult
    func isRaisingContainerViewTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что контейнер c " + Consts.raising + " нажимается") { _ -> Void in

            self.scrollToElement(element: self.raisingContainerView)
            
            self.raisingContainerView.yreEnsureExists()
            XCTAssertTrue(self.raisingContainerView.isEnabled)
            XCTAssertTrue(self.raisingContainerView.isHittable)
        }
        return self
    }

    @discardableResult
    func tapRaisingContainerView() -> UserOfferProductInfoViewSteps {
        XCTContext.runActivity(named: "Нажимаем на контейнер c " + Consts.raising) { _ -> Void in
            self.raisingContainerView.yreEnsureExists()
                .tap()
        }
        return UserOfferProductInfoViewSteps()
    }

    @discardableResult
    func isRaisingPayButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что нажимается кнопка, ведущая на покупку " + Consts.raising) { _ -> Void in
            self.raisingPayButton.yreEnsureExists()
            XCTAssertTrue(self.raisingPayButton.isEnabled)
            XCTAssertTrue(self.raisingPayButton.isHittable)
        }
        return self
    }

    @discardableResult
    func tapRaisingActivateButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку, ведущую на покупку " + Consts.raising) { _ -> Void in
            self.raisingPayButton.yreEnsureExists()
                .tap()
        }
        return PaymentMethodsSteps()
    }

    @discardableResult
    func tapRaisingAutoPurchaseSwitch() -> UserOfferVASActivationSteps {
        XCTContext.runActivity(named: "Нажимаем на свитчер автопродления " + Consts.raising) { _ -> Void in
            self.scrollToElement(element: self.raisingAutoPurchaseSwitch)
            self.raisingAutoPurchaseSwitch
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .tap()
        }
        return UserOfferVASActivationSteps()
    }

    @discardableResult
    func compareRaisingViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом продукта Поднятие в ЛК") { _ -> Void in
            self.scrollToElement(element: self.raisingContainerView)
            self.raisingContainerView.yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: - VAS Premium

    @discardableResult
    func isPremiumContainerViewTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что контейнер c " + Consts.premium + " нажимается") { _ -> Void in

            self.scrollToElement(element: self.premiumContainerView)

            self.premiumContainerView.yreEnsureExists()
            XCTAssertTrue(self.premiumContainerView.isEnabled)
            XCTAssertTrue(self.premiumContainerView.isHittable)
        }
        return self
    }

    @discardableResult
    func tapPremiumContainerView() -> UserOfferProductInfoViewSteps {
        XCTContext.runActivity(named: "Нажимаем на контейнер c " + Consts.premium) { _ -> Void in
            self.premiumContainerView.yreEnsureExists()
                .tap()
        }
        return UserOfferProductInfoViewSteps()
    }

    @discardableResult
    func isPremiumPayButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что нажимается кнопка, ведущая на покупку " + Consts.premium) { _ -> Void in
            self.premiumPayButton.yreEnsureExists()
            XCTAssertTrue(self.premiumPayButton.isEnabled)
            XCTAssertTrue(self.premiumPayButton.isHittable)
        }
        return self
    }

    @discardableResult
    func tapPremiumActivateButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку, ведущую на покупку " + Consts.premium) { _ -> Void in
            self.premiumPayButton.yreEnsureExists()
                .tap()
        }
        return PaymentMethodsSteps()
    }

    @discardableResult
    func tapPremiumAutoPurchaseSwitch() -> UserOfferVASActivationSteps {
        XCTContext.runActivity(named: "Нажимаем на свитчер автопродления " + Consts.premium) { _ -> Void in
            self.scrollToElement(element: self.raisingAutoPurchaseSwitch)
            self.premiumAutoPurchaseSwitch
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .tap()
        }
        return UserOfferVASActivationSteps()
    }

    @discardableResult
    func comparePremiumViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом продукта Премиум в ЛК") { _ -> Void in
            self.scrollToElement(element: self.premiumContainerView)
            self.premiumContainerView.yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: - VAS Promotion

    @discardableResult
    func isPromotionContainerViewTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что контейнер c " + Consts.promotion + " нажимается") { _ -> Void in

            self.scrollToElement(element: self.promotionContainerView)

            self.promotionContainerView.yreEnsureExists()
            XCTAssertTrue(self.promotionContainerView.isEnabled)
            XCTAssertTrue(self.promotionContainerView.isHittable)
        }
        return self
    }

    @discardableResult
    func tapPromotionContainerView() -> UserOfferProductInfoViewSteps {
        XCTContext.runActivity(named: "Нажимаем на контейнер c " + Consts.promotion) { _ -> Void in
            self.promotionContainerView.yreEnsureExists()
                .tap()
        }
        return UserOfferProductInfoViewSteps()
    }

    @discardableResult
    func isPromotionPayButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что нажимается кнопка, ведущая на покупку " + Consts.promotion) { _ -> Void in
            self.promotionPayButton.yreEnsureExists()
            XCTAssertTrue(self.promotionPayButton.isEnabled)
            XCTAssertTrue(self.promotionPayButton.isHittable)
        }
        return self
    }

    @discardableResult
    func tapPromotionActivateButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку, ведущую на покупку " + Consts.promotion) { _ -> Void in
            self.promotionPayButton.yreEnsureExists()
                .tap()
        }
        return PaymentMethodsSteps()
    }

    @discardableResult
    func tapPromotionAutoPurchaseSwitch() -> UserOfferVASActivationSteps {
        XCTContext.runActivity(named: "Нажимаем на свитчер автопродления " + Consts.promotion) { _ -> Void in
            self.scrollToElement(element: self.raisingAutoPurchaseSwitch)
            self.promotionAutoPurchaseSwitch
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .tap()
        }
        return UserOfferVASActivationSteps()
    }

    @discardableResult
    func comparePromotionViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом продукта Продвижение в ЛК") { _ -> Void in
            self.scrollToElement(element: self.promotionContainerView)
            self.promotionContainerView.yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: Private

    private enum Consts {
        static let turbo = "VAS-турбо"
        static let activatedTurbo = "Активированный VAS-турбо"
        static let premium = "VAS-премиум"
        static let promotion = "VAS-продвижение"
        static let raising = "VAS-поднятие"
    }

    private struct Identifier {
        static let editOfferButton = "userOffers.list.snippetCell.editOfferButton"
        static let removeButton = "userOffers.list.snippetCell.removeButton"
        static let publishButton = "userOffers.list.snippetCell.publishButton"
        static let prolongateButton = "userOffers.list.snippetCell.prolongateButton"
        static let addPhotoButton = "userOffers.list.snippetCell.addPhotoButton"
        static let editPriceButton = "userOffers.list.snippetCell.editPriceButton"
        static let priceEditControl = "userOffers.list.snippetCell.priceEditControl"

        static let statusLabelIdentifier = "userOffers.list.snippetCell.statusLabel"
        static let problemViewIdentifier = "userOffers.list.snippetCell.problemView.text"

        private init() {}
    }

    private typealias SnippetIdentifiers = UserOfferSnippetAccessibilityIdentifiers
    private typealias TurboProductIdentifiers = UserOfferTurboProductAccessibilityIdentifiers
    private typealias ActivatedTurboProductIdentifiers = UserOfferActivatedTurboProductAccessibilityIdentifiers
    private typealias ProductsIdentifiers = UserOfferProductsAccessibilityIdentifiers

    private let snippetView: XCUIElement
    private let scrollView: XCUIElement

    private lazy var editOfferButton: XCUIElement = self.obtainElement(SnippetIdentifiers.editOfferButtonIdentifier)
    private lazy var removeButton: XCUIElement = self.obtainElement(SnippetIdentifiers.removeButtonIdentifier)
    private lazy var publishButton: XCUIElement = self.obtainElement(SnippetIdentifiers.publishButtonIdentifier)
    private lazy var prolongateButton: XCUIElement = self.obtainElement(SnippetIdentifiers.prolongateButtonIdentifier)
    private lazy var addPhotoButton: XCUIElement = self.obtainElement(SnippetIdentifiers.addPhotoButtonIdentifier)
    private lazy var editPriceButton: XCUIElement = self.obtainElement(SnippetIdentifiers.editPriceButtonIdentifier)
    private lazy var priceEditControl: XCUIElement = self.obtainElement(SnippetIdentifiers.priceEditControlIdentifier)

    private lazy var statusLabel: XCUIElement = self.obtainElement(SnippetIdentifiers.statusLabelIdentifier)
    private lazy var problemView: XCUIElement = self.obtainElement(SnippetIdentifiers.ProblemView.textIdentifier)

    // MARK: - VAS

    private lazy var turboContainerView: XCUIElement = self.obtainElement(TurboProductIdentifiers.viewIdentifier)
    private lazy var turboActivateButton: XCUIElement = self.obtainElement(TurboProductIdentifiers.activateButton)

    private lazy var activatedTurboContainerView: XCUIElement = self.obtainElement(ActivatedTurboProductIdentifiers.viewIdentifier)
    private lazy var activatedTurboActivateButton: XCUIElement = self.obtainElement(ActivatedTurboProductIdentifiers.activateButton)

    private lazy var raisingContainerView: XCUIElement = self.obtainElement(ProductsIdentifiers.raisingView)
    private lazy var raisingPayButton: XCUIElement = self.obtainElement(ProductsIdentifiers.raisingPayButton)
    private lazy var raisingAutoPurchaseSwitch: XCUIElement = self.obtainElement(ProductsIdentifiers.raisingAutoPurchaseSwitch)

    private lazy var premiumContainerView: XCUIElement = self.obtainElement(ProductsIdentifiers.premiumView)
    private lazy var premiumPayButton: XCUIElement = self.obtainElement(ProductsIdentifiers.premiumPayButton)
    private lazy var premiumAutoPurchaseSwitch: XCUIElement = self.obtainElement(ProductsIdentifiers.premiumAutoPurchaseSwitch)

    private lazy var promotionContainerView: XCUIElement = self.obtainElement(ProductsIdentifiers.promotionView)
    private lazy var promotionPayButton: XCUIElement = self.obtainElement(ProductsIdentifiers.promotionPayButton)
    private lazy var promotionAutoPurchaseSwitch: XCUIElement = self.obtainElement(
        ProductsIdentifiers.promotionAutoPurchaseSwitch
    )

    // MARK: - Functions

    private func obtainElement(_ identifier: String) -> XCUIElement {
        return ElementsProvider.obtainElement(identifier: identifier,
                                              type: .any,
                                              in: self.snippetView)
    }

    private func scrollToElement(
        element: XCUIElement,
        velocity: CGFloat = 1.0,
        swipeLimits: UInt = 5
    ) {
        XCTContext.runActivity(named: "Скроллим к элементу") { _ -> Void in
            element.yreEnsureExistsWithTimeout()

            self.scrollView.scroll(
                to: element,
                velocity: velocity,
                swipeLimits: swipeLimits
            )
        }
    }
}
