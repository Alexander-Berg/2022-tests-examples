//
//  SuperMenuScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 1/31/22.
//

import XCTest

final class SuperMenuScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "SuperMenuViewController"
    static let rootElementName = "Супер-меню"

    enum Element {
        case auth
        case logInTitle
        case wallet
        case safeDeal
        case credit
        case reports
        case insurance
        case catalog
        case youtube
        case recallCompanies
        case myReviews
        case journal
        case price
        case notifications
        case themeSettings
        case help
        case licenseAgreement
        case confidentialPolitics
        case about
        case header
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .auth:
            return "superMenuAuthCell"
        case .logInTitle:
            return "Войти"
        case .wallet:
            return "superMenuWalletCell"
        case .safeDeal:
            return "Безопасная сделка"
        case .credit:
            return "Заявка на кредит"
        case .reports:
            return "superMenuPromoCell"
        case .insurance:
            return "Купить полис ОСАГО"
        case .catalog:
            return "Каталог автомобилей"
        case .youtube:
            return "Авто.ру на YouTube"
        case .recallCompanies:
            return "Отзывные кампании"
        case .myReviews:
            return "Мои отзывы"
        case .journal:
            return "Журнал Авто.ру"
        case .price:
            return "Стоимость автомобиля"
        case .notifications:
            return "SuperMenuNotificationCell"
        case .themeSettings:
            return "SuperMenuThemeCell"
        case .help:
            return "SuperMenuHelpCell"
        case .licenseAgreement:
            return "SuperMenuLicenseAgreementCell"
        case .confidentialPolitics:
            return "SuperMenuConfidentialPoliticsCell"
        case .about:
            return "SuperMenuAboutCell"
        case .header:
            return "super_menu_header_cell"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .auth:
            return "Авторизация/Профиль"
        case .logInTitle:
            return "заголовок Войти"
        case .wallet:
            return "Кошелёк"
        case .safeDeal:
            return "Безопасная сделка"
        case .credit:
            return "Заявка на кредит"
        case .reports:
            return "Отчёты ПроАвто"
        case .insurance:
            return "Купить полис ОСАГО"
        case .catalog:
            return "Каталог автомобилей"
        case .youtube:
            return "Авто.ру на YouTube"
        case .recallCompanies:
            return "Отзывные кампании"
        case .myReviews:
            return "Мои отзывы"
        case .journal:
            return "Журнал Авто.ру"
        case .price:
            return "Стоимость автомобиля"
        case .notifications:
            return "Уведомления"
        case .themeSettings:
            return "Настройки темы"
        case .help:
            return "Помощь"
        case .licenseAgreement:
            return "Лицензионное соглашение"
        case .confidentialPolitics:
            return "Политика конфиденциальности"
        case .about:
            return "О приложении"
        case .header:
            return "Супер-меню header"
        }
    }
}

final class SuperMenuHeaderCell: BaseSteps, UIElementProvider {
    typealias Element = Void
    static let rootElementID = "super_menu_header_cell"
    static let rootElementName = "Супер-меню header"
}

final class SuperMenuPromoCell: BaseSteps, UIElementProvider {
    typealias Element = Void
    static let rootElementID = "superMenuPromoCell"
    static let rootElementName = "Супер-меню promo"
}
