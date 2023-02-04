//
//  UserOfferPaymentMethodsTests.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 14.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable force_unwrapping

import XCTest
@testable import YREUserOfferPaymentModule
@testable import YREPaymentComponents
@testable import YREModel
@testable import YREModelObjc

final class UserOfferPaymentMethodsTests: XCTestCase {
    func testPaymentMethodsLayoutForPackageTurbo() {
        let purchase = UserOfferPurchase.makePurchase(type: .packageTurbo)
        let cart = UserOfferPaymentCart.makeCart(type: .packageTurbo)
        let viewController = self.makeViewController(cart: cart, purchase: purchase)
        self.assertSnapshot(viewController.view)
    }

    func testPaymentMethodsLayoutForPremium() {
        let purchase = UserOfferPurchase.makePurchase(type: .premium)
        let cart = UserOfferPaymentCart.makeCart(type: .premium)
        let viewController = self.makeViewController(cart: cart, purchase: purchase)
        self.assertSnapshot(viewController.view)
    }

    func testPaymentMethodsLayoutForRaising() {
        let purchase = UserOfferPurchase.makePurchase(type: .raising)
        let cart = UserOfferPaymentCart.makeCart(type: .raising)
        let viewController = self.makeViewController(cart: cart, purchase: purchase)
        self.assertSnapshot(viewController.view)
    }

    func testPaymentMethodsLayoutForPromotion() {
        let purchase = UserOfferPurchase.makePurchase(type: .promotion)
        let cart = UserOfferPaymentCart.makeCart(type: .promotion)
        let viewController = self.makeViewController(cart: cart, purchase: purchase)
        self.assertSnapshot(viewController.view)
    }

    private let generator = UserOfferPaymentMethodsViewModelGenerator.self

    private func makeViewController(cart: UserOfferPaymentCart, purchase: UserOfferPurchase) -> UIViewController {
        let generator = self.generator

        guard let headerViewType = generator.makeHeaderViewType(cart: cart, purchase: purchase) else {
            assertionFailure("Couldn't create header view model")
            return UIViewController()
        }
        let paymentMethodsPairs = generator.makePaymentMethods(
            plainMethods: [
                YREPaymentMethod.bankCard,
                YREPaymentMethod.sberbank,
                YREPaymentMethod.yooMoney,
            ],
            wallet: nil
        ).sorted { $0.0 < $1.0 }
        let paymentMethods = paymentMethodsPairs.map { $0.0 }
        let tableViewModel = generator.makeTableViewModel(paymentMethods: paymentMethods)
        let footerViewModel = generator.makeFooterViewModel(purchase: purchase)

        let viewController = PaymentMethodsViewController(
            headerViewType: headerViewType,
            tableViewModel: tableViewModel,
            footerViewModel: footerViewModel
        )
        return viewController
    }
}

extension UserOfferPurchase {
    fileprivate static func makePurchase(type: UserOfferProductType) -> UserOfferPurchase {
        UserOfferPurchase(
            identifier: "cb68dcb3f47943ffbd1595d6024a93c2",
            purchaseStatus: .new,
            price: UserOfferProductAvailablePrice.stubPrice,
            vasProducts: [UserOfferProduct.makeProduct(type: type)],
            placementProduct: nil,
            willTurnOnRenewal: false
        )
    }
}

extension UserOfferPaymentCart {
    fileprivate static func makeCart(type: UserOfferProductType) -> UserOfferPaymentCart {
        UserOfferPaymentCart(
            userOfferID: "3089311723042339329",
            kind: .singleProductActivation(UserOfferProduct.makeProduct(type: type)),
            mode: .activation
        )
    }
}

extension YREPaymentMethod {
    fileprivate static var bankCard: YREPaymentMethod {
        YREPaymentMethod(
            identifier: "bank_card",
            name: "Банковская карта",
            methodType: PaymentMethodType.bankCard,
            paymentSystemType: PaymentSystemType.yooKassaV3,
            rawPayload: [:],
            bankCard: nil,
            needsEmail: true
        )
    }

    fileprivate static var sberbank: YREPaymentMethod {
        YREPaymentMethod(
            identifier: "sberbank",
            name: "SberPay",
            methodType: PaymentMethodType.sberbank,
            paymentSystemType: PaymentSystemType.yooKassaV3,
            rawPayload: [:],
            bankCard: nil,
            needsEmail: true
        )
    }

    fileprivate static var yooMoney: YREPaymentMethod {
        YREPaymentMethod(
            identifier: "yoo_money",
            name: "ЮMoney",
            methodType: PaymentMethodType.yooMoney,
            paymentSystemType: PaymentSystemType.yooKassaV3,
            rawPayload: [:],
            bankCard: nil,
            needsEmail: true
        )
    }
}

extension UserOfferProduct {
    fileprivate static func makeProduct(type: UserOfferProductType) -> UserOfferProduct {
        UserOfferProduct(
            type: type,
            productDescription: UserOfferProductDescription.stubProduct,
            availablePrice: UserOfferProductAvailablePrice.stubPrice,
            status: .inactive,
            renewalInfo: nil,
            endTime: nil,
            secondsUntilExpiry: nil,
            pendingActivationCount: 0,
            pendingPaymentCount: 0
        )!
    }
}

extension UserOfferProductDescription {
    fileprivate static var stubProduct: UserOfferProductDescription {
        UserOfferProductDescription(
            durationLength: 7,
            durationType: .days,
            descriptionText: "Описание",
            callsCount: 3,
            viewsCount: 7
        )!
    }
}

extension UserOfferProductAvailablePrice {
    fileprivate static var stubPrice: UserOfferProductAvailablePrice {
        UserOfferProductAvailablePrice(
            basePrice: 69900,
            effectivePrice: 69900,
            moneyModifiers: []
        )
    }
}
