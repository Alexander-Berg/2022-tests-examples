import XCTest

class WebViewPage: PageObject {

    class WebviewNavBar: NavigationBarPage {
        /// Крестик в правом верхнем углу
        override var closeButton: XCUIElement {
            element
                .buttons
                .matching(identifier: WebviewAccessibility.closeButton)
                .firstMatch
        }
    }

    var navigationBar: WebviewNavBar {
        WebviewNavBar(element: NavigationBarPage.current.element)
    }

    static var current: WebViewPage {
        let el = XCUIApplication().otherElements[WebviewAccessibility.webview]
        return WebViewPage(element: el)
    }
}
