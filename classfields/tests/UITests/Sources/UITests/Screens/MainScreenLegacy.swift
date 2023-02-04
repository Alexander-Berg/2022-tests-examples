//
//  MainScreen.swift
//  UITests
//
//  Created by Victor Orlovsky on 20/03/2019.
//

import XCTest
import Snapshots

public enum MainTab: String, CaseIterable {
    case transport = "ТРАНСПОРТ"
    case reports = "ПРОАВТО"
    case credits = "КРЕДИТЫ"
    case insurance = "ОСАГО"
    case reviews = "ОТЗЫВЫ"
    case journal = "ЖУРНАЛ"
}

public enum LowTab: String, CaseIterable {
    case search = "Поиск"
    case favorites = "Избранное"
    case offers = "Объявления"
    case offers_attentions = "Разместить"
    case messages = "Сообщения"
    case garage = "Гараж"
}

enum MordaBanner {
    enum ReportsBannerKind {
        case loading, buy, bought
    }

    case carReports
    case reportsBundle(ReportsBannerKind)
    case newAuto

    var elementId: String {
        switch self {
        case .carReports: return "large_marketing_preset_car_reports"
        case let .reportsBundle(kind):
            switch kind {
            case .loading: return "carReports_placeholder"
            case .buy, .bought: fatalError()
            }
        case .newAuto:
            return "small_marketing_preset_newAuto"
        }
    }
}

class MainScreen: BaseScreen, Scrollable {
    lazy var staticTexts = findAll(.staticText).firstMatch
    lazy var buttons = findAll(.button)
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    func mainTab(_ item: MainTab) -> XCUIElement {
        return findAll(.staticText)[item.rawValue]
    }

    func tabBarItem(kind: LowTab) -> XCUIElement {
        return find(by: "tabbar_item_\(kind.rawValue)").firstMatch
    }

    lazy var quickSearchButton: XCUIElement = find(by: "main_tab.navbar.quick_search").firstMatch

    lazy var parametersButton: XCUIElement = scrollableElement.staticTexts["Параметры"]

    lazy var creditBanner = find(by: "creditBaner").firstMatch

    lazy var tabsScrollView = find(by: "main_header_tabs_scroll").firstMatch

    func lastElementScrollView(_ item: MainTab) -> XCUIElement {
        switch item {
        case .transport:
            return find(by: "space_24.0_section_header_1").firstMatch
        case .reports:
            return find(by: "space_24.0_history_row_6").firstMatch
        default:
            preconditionFailure("not implemented")
        }
    }

    func banner(_ kind: MordaBanner) -> XCUIElement {
        if case .newAuto = kind {
            return bannerByPrefix(kind.elementId)
        } else {
            return find(by: kind.elementId).firstMatch
        }
    }

    func bannerByPrefix(_ prefix: String) -> XCUIElement {
        return app.collectionViews
            .firstMatch
            .cells
            .withIdentifierPrefix(prefix)
            .firstMatch
    }

    func watchedOffersHistory() -> XCUIElement {
        return find(by: "history_offers").firstMatch
    }

    func carReportsListButton() -> XCUIElement {
        return find(by: MainTab.reports.rawValue).firstMatch
    }

    func creditsTab() -> XCUIElement {
        return find(by: MainTab.credits.rawValue).firstMatch
    }

    func insuranceTab() -> XCUIElement {
        return find(by: MainTab.insurance.rawValue).firstMatch
    }

    func reviewsTab() -> XCUIElement {
        return find(by: MainTab.reviews.rawValue).firstMatch
    }

    func journalTab() -> XCUIElement {
        return find(by: MainTab.journal.rawValue).firstMatch
    }
}

class EmptyFavoritesScreen: BaseScreen {
    public lazy var loginButton = findAll(.staticText)["Войти"]
}
