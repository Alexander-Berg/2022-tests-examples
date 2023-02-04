import XCTest

typealias DealerCardScreen_ = DealerCardSteps

extension DealerCardScreen_: UIRootedElementProvider {
    static let rootElementID = "DealerCardViewController"
    static let rootElementName = "Экран выдачи по конкретному дилеру из карточки оффера"
}
