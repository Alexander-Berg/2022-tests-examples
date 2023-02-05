import MarketRegionSelectFeature
import XCTest

final class RegionSelectPage: PageObject {

    // MARK: - Properties

    static var current: RegionSelectPage {
        let element = XCUIApplication().otherElements[RegionSelectAccessibility.root]
        return RegionSelectPage(element: element)
    }

    /// Кнопка подтверждения региона
    var doneChoosingButton: XCUIElement {
        XCUIApplication().buttons
            .matching(identifier: RegionSelectAccessibility.doneChoosingRegionButton).firstMatch
    }

    /// Поле ввода региона
    var regionInput: XCUIElement {
        element.textFields.matching(identifier: RegionSelectAccessibility.regionInput).firstMatch
    }

    var regionInputPlaceholder: XCUIElement {
        element.staticTexts.matching(identifier: RegionSelectAccessibility.regionInputPlaceholder).firstMatch
    }

    var regionInputClearButton: XCUIElement {
        element.buttons.matching(identifier: RegionSelectAccessibility.regionInputClearButton).firstMatch
    }

    /// Информация при отсутствии доставки в даный регион
    var noDeliveryTitle: XCUIElement {
        cellUniqueElement(withIdentifier: RegionSelectAccessibility.noDeliveryTitle).textViews.firstMatch
    }

    var noDeliverySubtitle: XCUIElement {
        cellUniqueElement(withIdentifier: RegionSelectAccessibility.noDeliverySubtitle).textViews.firstMatch
    }

    /// Саджесты поиска регионов
    var geoSuggest: [XCUIElement] {
        allCellUniqueElement(withIdentifier: RegionSelectAccessibility.geoSuggest)
    }

    /// Саджесты ближайших регионов
    var nearestRegionSuggest: [XCUIElement] {
        allCellUniqueElement(withIdentifier: RegionSelectAccessibility.nearestRegionSuggest)
    }

    /// Саджест автоопрделения
    var autodetectSuggest: AutodetectSuggest {
        let element = cellUniqueElement(withIdentifier: RegionSelectAccessibility.autodetectSuggest).firstMatch
        return AutodetectSuggest(element: element)
    }

}

// MARK: - CollectionViewPage

extension RegionSelectPage: CollectionViewPage {
    typealias AccessibilityIdentifierProvider = RegionSelectCollectionViewCellAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}

// MARK: - Nested types

extension RegionSelectPage {
    class AutodetectSuggest: PageObject {
        var label: String {
            element.staticTexts.firstMatch.label
        }
    }
}
