import Foundation
final class GarageAddNewCardInSwipeScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "addNewGarageCardScreen"
    static let rootElementName = "Экран добавления карточки в свайпе раздела гаража"

    enum Element: String {
        case govNumberInput = "gov_number_input"
        case addByVin = "garage_add_by_vin_button"
        case addDreamCar = "add_dream_car_button"
        case addExCar = "add_ex_car_button"
        case findButton = "garage_fing_gov_number_button"
    }
}
