import MarketUI
import UIUtils
import XCTest

final class ErrorPage: PageObject {

    final class RefreshButton: PageObject {}

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BarrierViewAccessibility.title)
            .firstMatch
    }

    var refreshButton: RefreshButton {
        let elem = element
            .buttons
            .matching(identifier: BarrierViewAccessibility.actionButton)
            .firstMatch
        return RefreshButton(element: elem)
    }

    func refresh() {
        refreshButton.element.tap()
    }

    var goOnMainButton: XCUIElement {
        element
            .buttons
            .matching(identifier: BarrierViewAccessibility.extraButton)
            .firstMatch
    }

    func goToMain() -> MordaPage {
        goOnMainButton.tap()

        let morda = XCUIApplication().otherElements[MordaAccessibility.container]
        _ = morda.waitForExistence(timeout: 10)

        return MordaPage(element: morda)
    }
}

extension PageObject {

    var errorPage: ErrorPage {
        let elem = element
            .otherElements
            .matching(identifier: BarrierViewAccessibility.root)
            .firstMatch
        return ErrorPage(element: elem)
    }
}
