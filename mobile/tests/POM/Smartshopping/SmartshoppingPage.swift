import UIUtils
import XCTest

final class SmartshoppingPage: PageObject, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = SmartshoppingCollectionViewCellsAccessibility

    // MARK: - Properties

    static let rootIdentifier = SmartshoppingAccessibility.container

    /// Вся страничка SmartshoppingViewController
    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    var howToGetButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.howToGetButton)

        return elem.buttons.firstMatch
    }

    var howToUseButton: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.howToUseButton)

        return elem.buttons.firstMatch
    }

    var tokenCard: SmartbonusTokenCardSnippetPage {
        let tokenCard = carouselView.collectionView
            .otherElements
            .matching(identifier: SmartbonusTokenCardSnippetPage.rootIdentifier)
            .firstMatch
        return SmartbonusTokenCardSnippetPage(element: tokenCard)
    }

    var singleCouponCard: SmartbonusSnippetPage {
        let couponCard = carouselView.collectionView
            .otherElements
            .matching(identifier: SmartbonusSnippetPage.rootIdentifier)
            .firstMatch
        return SmartbonusSnippetPage(element: couponCard)
    }

    var infoSectionTitle: XCUIElement {
        let elem = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.infoSectionTitleTextView)

        return elem.textViews.firstMatch
    }

    class CarouselView: PageObject, UniformCollectionViewPage {
        typealias CellPage = SmartbonusSnippetPage
        typealias AccessibilityIdentifierProvider = CarouselCellsAccessibility

        var collectionView: XCUIElement {
            element
        }

        var isBonusExists: Bool {
            element.cells.count > 1
        }
    }

    class SmartBonusBannerView: PageObject {
        var loginAndStashButton: XCUIElement {
            element.buttons
                .matching(identifier: SmartBonusBannerAccessibility.button)
                .firstMatch
        }
    }

    var smartBonusBannerView: SmartBonusBannerView {
        let elem = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.smartBonusBannerView)

        return SmartBonusBannerView(element: elem)
    }

    /// Карусель купонов
    var carouselView: CarouselView {
        let elem = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.carouselCollectionView)

        return CarouselView(element: elem)
    }

    func scrollToBonus(at index: Int) -> SmartbonusSnippetPage {
        let snippet = allCellUniqueElement(withIdentifier: SmartshoppingAccessibility.couponSnippetView)[index]
            .otherElements
            .firstMatch

        collectionView.swipe(to: .down, untilVisible: snippet)
        collectionView.ybm_swipe(toFullyReveal: snippet)

        return SmartbonusSnippetPage(element: snippet)
    }

    func infoSectionDetailText(at index: Int) -> XCUIElement {
        allCellUniqueElement(withIdentifier: SmartshoppingAccessibility.infoSectionDetailTextView)[index].textViews
            .firstMatch
    }
}
