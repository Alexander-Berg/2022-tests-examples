import AutoMate
import UIUtils
import XCTest

class UnlockCoinPopupPage: PageObject, PopupPage, CollectionViewPage {

    typealias AccessibilityIdentifierProvider = UnlockCoinPopupCollectionViewCellsAccessibility

    // MARK: - Properties

    static let rootIdentifier = UnlockCoinPopupAccessibility.root

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

    // Заголовок купонов
    var title: XCUIElement {
        let el = cellUniqueElement(withIdentifier: UnlockCoinPopupAccessibility.title)
        return el.textViews.firstMatch
    }

    /// Кнопка "Продолжить"
    var continueButton: XCUIElement {
        let el = cellUniqueElement(withIdentifier: UnlockCoinPopupAccessibility.continueButton)
        return el.buttons.firstMatch
    }

    /// Крестик для закрытия попапа
    var closeButton: XCUIElement {
        element.buttons.matching(identifier: UnlockCoinPopupAccessibility.closeButton).firstMatch
    }

    class CarouselView: PageObject, UniformCollectionViewPage {
        typealias CellPage = SmartbonusSnippetPage
        typealias AccessibilityIdentifierProvider = CarouselCellsAccessibility

        var collectionView: XCUIElement {
            element
        }
    }

    var pageController: XCUIElement {
        let el = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.pageController)
        return el.otherElements.firstMatch
    }

    /// Карусель купонов
    var carouselView: CarouselView {
        let elem = cellUniqueElement(withIdentifier: SmartshoppingAccessibility.carouselCollectionView)

        return CarouselView(element: elem)
    }

    func scrollToBonus(at index: Int, direction: SwipeDirection = .right) -> SmartbonusSnippetPage {
        let bonusSnippet = carouselView.cellPage(at: IndexPath(item: index, section: 0))

        carouselView.collectionView.swipe(to: direction, untilVisible: bonusSnippet.element)
        carouselView.collectionView.ybm_swipe(toFullyReveal: bonusSnippet.element)

        return bonusSnippet
    }

}
