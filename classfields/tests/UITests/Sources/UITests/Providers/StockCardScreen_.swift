import XCTest
import Snapshots

typealias StockCardScreen_ = StockCardSteps

extension StockCardScreen_: UIRootedElementProvider {
    enum Element {
        case compareButton(compare: Bool)
        case saveSearchButton(saved: Bool)
        case offerCount(Int)
        case sorting
        case relatedCars
        case relatedCar(String)
        case offerOfTheDay
        case callButton
        case geoRadiusBubbles
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .compareButton(compare):
            return compare ? "compare_red" : "compare_gray"
        case let .saveSearchButton(saved):
            return saved ? "icn-saved-search" : "icn-save-search"
        case let .offerCount(count):
            return "\(count) предложения"
        case .sorting:
            return "По актуальности"
        case .relatedCars:
            return "related_header"
        case let .relatedCar(car):
            return car
        case .offerOfTheDay:
            return "Предложения дня"
        case .callButton:
            return "Позвонить"
        case .geoRadiusBubbles:
            return "geoRadiusBubbles"
        }
    }

    static var rootElementID = "stock_card_view_controller"
    static var rootElementName = "Групповая карточка"
}
