import XCTest

final class LiveAnnouncementPage: PageObject {
    static var current: LiveAnnouncementPage {
        let element = XCUIApplication().any.matching(identifier: LiveAnnouncementAccessibility.root).firstMatch
        return LiveAnnouncementPage(element: element)
    }

    /// Шапка
    var navigationBar: NavigationBar {
        NavigationBar(element: NavigationBarPage.current.element)
    }

    var skuScrollView: XCUIElement {
        let collectionViewElement = element
            .otherElements
            .matching(identifier: LiveAnnouncementAccessibility.skuScrollView)
            .collectionViews
            .firstMatch
        return collectionViewElement
    }
}

// MARK: - Nested types

extension LiveAnnouncementPage {

    final class NavigationBar: NavigationBarPage {
        override var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: LiveAnnouncementAccessibility.dateLabel)
                .firstMatch
        }

        override var closeButton: XCUIElement {
            element
                .buttons
                .matching(identifier: LiveAnnouncementAccessibility.closeButton)
                .firstMatch
        }
    }
}
