import XCTest

final class OrdersPage: PageObject {

    // MARK: - Properties

    static var current: OrdersPage {
        let element = XCUIApplication().any.matching(identifier: OrdersAccessibility.root).firstMatch
        return .init(element: element)
    }

    var pushSubscribe: PushSubscribe {
        let el = element.otherElements.matching(identifier: PushSubscribeAccessibility.root).firstMatch
        return PushSubscribe(element: el)
    }

    // MARK: - Public

    func header(orderId: String) -> HeaderPage {
        let el = element.otherElements.matching(identifier: OrdersAccessibility.header(orderId: orderId)).firstMatch
        return HeaderPage(element: el)
    }

    func footer(orderId: String) -> FooterPage {
        let el = element.otherElements.matching(identifier: OrdersAccessibility.footer(orderId: orderId)).firstMatch
        return FooterPage(element: el)
    }

}

// MARK: - Nested types

extension OrdersPage {

    final class HeaderPage: PageObject {
        var status: PageObject {
            let el = element.staticTexts.matching(identifier: OrdersAccessibility.headerStatus).firstMatch
            return PageObject(element: el)
        }

        var stillNoButton: XCUIElement {
            element.buttons.matching(identifier: OrdersAccessibility.stillNoButton).firstMatch
        }

        var alreadyGotItButton: XCUIElement {
            element.buttons.matching(identifier: OrdersAccessibility.alreadyGotItButton).firstMatch
        }
    }

    final class FooterPage: PageObject {
        var detailButton: DetailButton {
            let el = element.buttons.matching(identifier: OrdersAccessibility.footerDetailButton).firstMatch
            return DetailButton(element: el)
        }

        var payButton: PayButton {
            let el = element.buttons.matching(identifier: OrdersAccessibility.footerPayButton).firstMatch
            return .init(element: el)
        }

        var consultationButton: XCUIElement {
            element.buttons.matching(identifier: OrdersAccessibility.consultationButton).firstMatch
        }
    }

    final class DetailButton: PageObject, OrderDetailsEntryPoint {}

    final class PayButton: PageObject {
        func tap() -> OrderEditPaymentPage {
            element.tap()
            let elem = XCUIApplication().otherElements[OrderEditPaymentAccessibility.root]
            return .init(element: elem)
        }
    }
}
