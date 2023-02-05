import UIUtils
import XCTest

class ComparisonPage: PageObject {

    class ComparisonModelCellPage: PageObject {

        class Title: PageObject {
            func tap() -> SKUPage {
                element.tap()
                return SKUPage.current
            }
        }

        class Photo: PageObject {
            func tap() -> SKUPage {
                element.tap()
                return SKUPage.current
            }
        }

        var title: Title {
            let elem = element
                .staticTexts
                .matching(identifier: ComparisonAccessibility.Model.title)
                .firstMatch
            return Title(element: elem)
        }

        var photo: Photo {
            let elem = element
                .images
                .matching(identifier: ComparisonAccessibility.Model.photo)
                .firstMatch
            return Photo(element: elem)
        }

        var outOfStockImage: XCUIElement {
            element.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock).firstMatch
        }

        var price: PageObject {
            let elem = element
                .staticTexts
                .matching(identifier: ComparisonAccessibility.Model.price)
                .firstMatch

            return PageObject(element: elem)
        }

        var delete: PageObject {
            let elem = element
                .buttons
                .matching(identifier: ComparisonAccessibility.Model.delete)
                .firstMatch

            return PageObject(element: elem)
        }

        func tap() -> SKUPage {
            element.tap()
            return SKUPage.current
        }
    }

    class ComparisonOpinionCellPage: PageObject {
        var rating: PageObject {
            let elem = element
                .staticTexts
                .matching(identifier: ComparisonAccessibility.Opinion.rating)
                .firstMatch

            return PageObject(element: elem)
        }

        var count: PageObject {
            let elem = element
                .staticTexts
                .matching(identifier: ComparisonAccessibility.Opinion.count)
                .firstMatch

            return PageObject(element: elem)
        }
    }

    class CollectionView: PageObject, CollectionViewPage {
        typealias AccessibilityIdentifierProvider = ComparisonCollectionViewCellsAccessibility

        var collectionView: XCUIElement {
            element
        }

        func modelCell(with index: Int) -> ComparisonModelCellPage {
            let element = cellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            return ComparisonModelCellPage(element: element)
        }

        func opinionCell(with index: Int) -> ComparisonOpinionCellPage {
            let element = cellUniqueElement(withIdentifier: ComparisonAccessibility.Opinion.root)
            return ComparisonOpinionCellPage(element: element)
        }
    }

    var collectionView: CollectionView {
        let elem = element
            .collectionViews
            .matching(identifier: ComparisonAccessibility.collectionView)
            .firstMatch
        return CollectionView(element: elem)
    }

    var goToCategoryButton: XCUIElement {
        element.otherElements.matching(identifier: ComparisonAccessibility.emptyViewGoToCategory).element.buttons
            .firstMatch
    }

    var allCharacteristics: PageObject {
        let elem = element
            .cells
            .matching(identifier: ComparisonAccessibility.allCharacteristics)
            .firstMatch

        return PageObject(element: elem)
    }

    var deleteListButton: PageObject {
        let elem = element
            .buttons
            .matching(identifier: ComparisonAccessibility.deleteListButton)
            .firstMatch

        return PageObject(element: elem)
    }

    var deleteToast: XCUIElement {
        XCUIApplication()
            .otherElements
            .matching(identifier: ComparisonAccessibility.deleteToast)
            .firstMatch
    }

}
