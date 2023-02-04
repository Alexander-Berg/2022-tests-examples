import Foundation
import Snapshots
import XCTest

private final class CreditPromoScreen: BaseScreen {
    lazy var promo = find(by: "creditPromo").firstMatch
    lazy var newApplicationButton = find(by: "Заполнить заявку").firstMatch
    lazy var continueApplicationButton = find(by: "Дозаполнить заявку").firstMatch
}

final class CreditPromoSteps: BaseSteps {
    fileprivate func onCreditPromoScreen() -> CreditPromoScreen {
        return baseScreen.on(screen: CreditPromoScreen.self)
    }

    func tapNewApplicationButton() -> PreliminaryCreditSteps {
        onCreditPromoScreen()
            .newApplicationButton
            .tap()
        return PreliminaryCreditSteps(context: context)
    }

    func tapContinueApplicationButton() -> PreliminaryCreditSteps {
        onCreditPromoScreen()
            .continueApplicationButton
            .tap()
        return PreliminaryCreditSteps(context: context)
    }

    @discardableResult
    func notExist() -> Self {
        onCreditPromoScreen()
            .promo
            .shouldNotExist()
        return self
    }

    @discardableResult
    func exist() -> Self {
        onCreditPromoScreen()
            .promo
            .shouldExist()
        return self
    }
}
