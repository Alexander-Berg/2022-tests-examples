//
//  YandexRentBannerViewSteps.swift
//  UI Tests
//
//  Created by Ella Meltcina on 9/17/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class YandexRentBannerViewSteps {
    @discardableResult
    func isBannerPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие баннера Яндекс.Аренда") { _ -> Void in
            self.bannerView.yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    
    @discardableResult
    func isBannerNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие баннера Яндекс.Аренда") { _ -> Void in
            self.bannerView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func tapOnAccountButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Личный кабинет") { _ -> Void in
            self.accountButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }
    
    @discardableResult
    func tapOnOwnerButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Сдать") { _ -> Void in
            self.ownerButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }
    
    @discardableResult
    func tapOnTenantButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Снять") { _ -> Void in
            self.tenantButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func compareCurrentState(with state: State) -> Self {
        XCTContext.runActivity(
            named: "Проверяем, что текущее состояние баннера Яндекс.Аренда - \"\(state.rawValue)\""
        ) { _ -> Void in
            switch state {
                case .promo:
                    self.ownerButton.yreEnsureExistsWithTimeout()
                    self.tenantButton.yreEnsureExistsWithTimeout()
                case .account:
                    self.accountButton.yreEnsureExistsWithTimeout()
            }
        }
        return self
    }
    
    private lazy var bannerView = ElementsProvider.obtainElement(
        identifier: InAppServicesAccessibilityIdentifiers.BannerView.view
    )
    
    private lazy var accountButton = ElementsProvider.obtainElement(
        identifier: InAppServicesAccessibilityIdentifiers.BannerView.accountButton,
        type: .button,
        in: self.bannerView
    )
    
    private lazy var ownerButton = ElementsProvider.obtainElement(
        identifier: InAppServicesAccessibilityIdentifiers.BannerView.ownerButton,
        type: .button,
        in: self.bannerView
    )
    
    private lazy var tenantButton = ElementsProvider.obtainElement(
        identifier: InAppServicesAccessibilityIdentifiers.BannerView.tenantButton,
        type: .button,
        in: self.bannerView
    )
}

extension YandexRentBannerViewSteps {
    enum State: String {
        case promo = "Промо"
        case account = "Личный кабинет"
    }
}
