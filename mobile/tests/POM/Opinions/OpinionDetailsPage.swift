import UIUtils
import XCTest

final class OpinionDetailsPage: PageObject, UniformCollectionViewPage {

    typealias AccessibilityIdentifierProvider = CommentsCollectionViewCellsAccessibility
    typealias CellPage = CommentCell

    // MARK: - Properties

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: CommentsAccessibility.opinionView).firstMatch
        return .init(element: element)
    }

    var openContextMenuButton: XCUIElement {
        cellUniqueElement(withIdentifier: SKUAccessibility.opinionAuthor)
            .buttons
            .firstMatch
    }

    var openContextMenuButtonInComments: XCUIElement {
        cellUniqueElement(withIdentifier: CommentsAccessibility.commentDateCell)
            .buttons
            .firstMatch
    }
}

// MARK: - Nested types

extension OpinionDetailsPage {

    class CommentCell: PageObject {
        var text: XCUIElement {
            element.descendants(matching: .textView).firstMatch
        }
    }

}
