//
//  GarageAddCar.swift
//  UITests
//
//  Created by Igor Shamrin on 16.11.2021.
//

final class AddCarScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "garage_add_car_view"
    static let rootElementName = "Экран добавления в гараж"

    enum Element {
        case addDreamCarButton
        case govNumberView
        case continueButton
        case addByVin
        case addExCarButton
        case disclaimer
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .addDreamCarButton: return "add_dream_car_button"
        case .govNumberView: return "app.views.gov_number"
        case .continueButton: return "garage_add_my_car_continue_button"
        case .addByVin: return "garage_add_by_vin_button"
        case .addExCarButton: return "add_ex_car_button"
        case .disclaimer: return "add_garage_card_disclaimer"
        }
    }
}
