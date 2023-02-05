import UIUtils
import XCTest

final class OutletMapInfoPage: PageObject {

    static var current: OutletMapInfoPage {
        let elem = XCUIApplication().otherElements[OutletMapInfoAccessibility.root]
        return OutletMapInfoPage(element: elem)
    }

    var headerTitle: XCUIElement {
        XCUIApplication().staticTexts[PopupEmdeddingAccessibility.navigationHeaderTitle]
    }

    var legalText: XCUIElement {
        cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.legalText)
            .staticTexts
            .firstMatch
    }

    var continueButton: XCUIElement {
        XCUIApplication().otherElements[OutletMapInfoAccessibility.doneButton]
            .buttons
            .element
    }

    var closeButton: XCUIElement {
        element.buttons
            .matching(identifier: OutletMapInfoAccessibility.closeButton)
            .firstMatch
    }

    var showOnYandexMapsButton: XCUIElement {
        XCUIApplication().otherElements[OutletMapInfoAccessibility.showOnYandexMapsButton]
            .buttons
            .element
    }

    var extendButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.extendButton)
        return elem.firstMatch
    }

    var storagePeriod: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.storagePeriod)
        return elem.textViews.firstMatch
    }

    var picturesContainer: OutletMapInfoPicturesContainerPage {
        let elem = cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.picturesContainer)
        return OutletMapInfoPicturesContainerPage(element: elem)
    }

    var name: XCUIElement {
        cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.name)
            .staticTexts
            .firstMatch
    }

    var address: XCUIElement {
        cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.address)
            .staticTexts
            .firstMatch
    }

    var scheduleDay: XCUIElement {
        cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.schedule)
            .staticTexts
            .element(boundBy: 0)
    }

    var scheduleTime: XCUIElement {
        cellUniqueElement(withIdentifier: OutletMapInfoAccessibility.schedule)
            .staticTexts
            .element(boundBy: 1)
    }
}

class OutletMapInfoPicturesContainerPage: PageObject, UniformCollectionViewPage {
    typealias CellPage = OutletPhotoPage
    typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility

    class OutletPhotoPage: PageObject {
        func tap() -> OpenedGallery {
            element.tap()
            let gallery = XCUIApplication().otherElements[GalleryCollectionViewCellAccessibility.openGallery]
            XCTAssertTrue(gallery.waitForExistence(timeout: XCTestCase.defaultTimeOut))
            return OpenedGallery(element: gallery)
        }

        class OpenedGallery: PageObject {
            var image: XCUIElement {
                element.images.firstMatch
            }

            var closeButton: XCUIElement {
                element
                    .buttons
                    .matching(identifier: GalleryCollectionViewCellAccessibility.galleryCloseButton)
                    .firstMatch
            }

            var pageControl: XCUIElement {
                element
                    .any
                    .matching(identifier: GalleryCollectionViewCellAccessibility.pageIndicatorInOpenGallery)
                    .firstMatch
            }
        }
    }

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - EntryPoint

protocol OutletMapInfoEntryPoint: PageObject {
    func tap() -> OutletMapInfoPage
}

extension OutletMapInfoEntryPoint {

    func tap() -> OutletMapInfoPage {
        element.tap()
        let elem = XCUIApplication().otherElements[OutletMapInfoAccessibility.root]
        XCTAssertTrue(elem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return OutletMapInfoPage(element: elem)
    }
}

// MARK: - CollectionViewPage

extension OutletMapInfoPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OutletMapInfoCollectionViewCellsAccessibility

    var collectionView: XCUIElement { element.collectionViews.firstMatch }
}
