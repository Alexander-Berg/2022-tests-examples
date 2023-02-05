import UIUtils
import XCTest

final class OpinionsPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OpinionsAccessibility

    // MARK: - Properties

    var openContextMenuButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SKUAccessibility.opinionAuthor)
        return elem.buttons.firstMatch
    }

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var allOpinionsGallery: ScrollBoxSnipppetContainerPage<PhotoCell> {
        let elem = cellUniqueElement(withIdentifier: OpinionsAccessibility.allOpinionsGallery)
        return ScrollBoxSnipppetContainerPage(element: elem)
    }

    var showAllPhotosButton: ShowAllPhotosButton {
        let element = cellUniqueElement(withIdentifier: OpinionsAccessibility.showAllPhotosButton)
            .buttons
            .firstMatch
        return ShowAllPhotosButton(element: element)
    }

}

// MARK: - Nested Types

extension OpinionsPage {

    class PhotoCell: PageObject {
        func tap() -> FullScreenGallery {
            element.tap()
            let gallery = XCUIApplication().otherElements[OpinionsGalleryAccessibility.container]
            return FullScreenGallery(element: gallery)
        }
    }

    class ShowAllPhotosButton: PageObject {
        func tap() -> GalleryGrid {
            element.tap()
            let grid = XCUIApplication().otherElements[OpinionsGalleryGridAccessibility.container]
            return GalleryGrid(element: grid)
        }
    }

    class FullScreenGallery: PageObject {
        var closeButton: XCUIElement {
            NavigationBarPage.current.element
                .buttons
                .matching(identifier: OpinionsGalleryAccessibility.closeButton)
                .firstMatch
        }
    }

    class GalleryGrid: PageObject {}

}
