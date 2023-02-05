import MarketYandexBank
import XCTest

final class YandexBankPage: PageObject {

    static var current: Self {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: YandexBankAccessibility.root)
            .firstMatch
        return Self(element: elem)
    }
}
