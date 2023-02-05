import UIUtils
import XCTest

final class PickupPointsMapPage: PageObject {
    class PickupNavigationBar: NavigationBarPage {
        var filterButton: XCUIElement {
            element
                .buttons
                .matching(identifier: PickupPointsMapAccessibility.filterButton)
                .firstMatch
        }

        var switchPageButton: XCUIElement {
            element
                .buttons
                .matching(identifier: PickupPointsMapAccessibility.switchPageButton)
                .firstMatch
        }
    }

    class MapControlViewPage: PageObject {
        class OutletAnnotationInfoView: PageObject {
            var chooseButton: XCUIElement {
                let elem = element.buttons
                    .matching(identifier: OutletAnnotationInfoViewAccessibility.chooseButton)
                    .firstMatch
                return elem
            }
        }

        var outletAnnotationInfoView: OutletAnnotationInfoView {
            let elem = element.otherElements
                .firstMatch
                .otherElements
                .matching(identifier: OutletAnnotationInfoViewAccessibility.root)
                .firstMatch
            return OutletAnnotationInfoView(element: elem)
        }
    }

    class CollectionView: PageObject, UniformCollectionViewPage {
        typealias AccessibilityIdentifierProvider = ResultCollectionViewCellsAccessibility
        typealias CellPage = PickupPointsMapPage

        class SuggestItem: PageObject {
            var titleLabel: XCUIElement {
                element.staticTexts.matching(identifier: PickupPointsMapAccessibility.suggestItemLabel).firstMatch
            }

            var subtitleLabel: XCUIElement {
                element.staticTexts.matching(identifier: PickupPointsMapAccessibility.suggestItemSubtitleLabel)
                    .firstMatch
            }
        }

        var collectionView: XCUIElement {
            element
        }

        func suggestItem(at index: Int) -> SuggestItem {
            let el = element.cells.element(boundBy: index)

            return SuggestItem(element: el)
        }
    }

    var navigationBar: PickupNavigationBar {
        PickupNavigationBar(element: NavigationBarPage.current.element)
    }

    var searchBarTextField: XCUIElement {
        element
            .textFields
            .matching(identifier: PickupPointsMapAccessibility.searchBarTextField)
            .firstMatch
    }

    var searchBarPlaceholderLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PickupPointsMapAccessibility.searchBarPlaceholderLabel)
            .firstMatch
    }

    var searchBarClearButton: XCUIElement {
        element
            .buttons
            .matching(identifier: PickupPointsMapAccessibility.searchBarTextFieldClearButton)
            .firstMatch
    }

    var searchBarErrorLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PickupPointsMapAccessibility.searchBarErrorLabel)
            .firstMatch
    }

    func mapViewWithZoom(zoom: Int) -> XCUIElement {
        XCUIApplication()
            .otherElements
            .matching(identifier: "\(PickupPointsMapAccessibility.mapView)-\(zoom)")
            .firstMatch
    }

    var suggestCollectionView: CollectionView {
        let elem = element
            .collectionViews.matching(identifier: PickupPointsMapAccessibility.addressSuggestCollectionView)
            .firstMatch
        return CollectionView(element: elem)
    }

    var mapControlView: MapControlViewPage {
        let elem = XCUIApplication().otherElements
            .matching(identifier: MapControlViewAccessibility.root)
            .firstMatch
        return MapControlViewPage(element: elem)
    }
}
