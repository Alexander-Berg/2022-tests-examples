import FormKit
import UIUtils
import XCTest

final class EmptyCartPage: PageObject {

    static var current: EmptyCartPage {
        let item = XCUIApplication().otherElements[EmptyCartAccessibility.root]
        return EmptyCartPage(element: item)
    }

    var collection: Collection {
        let item = element
            .collectionViews.matching(identifier: EmptyCartAccessibility.collectionView)
            .firstMatch
        return Collection(element: item)
    }

    class Collection: PageObject {
        typealias CarouselCell = LegacyScrollBoxWidgetPage<SnippetPage>

        var recommendationsCell: CarouselCell {
            let item = element
                .cells.matching(identifier: EmptyCartAccessibility.recommendationsCell)
                .firstMatch
            return CarouselCell(element: item)
        }
    }
}
