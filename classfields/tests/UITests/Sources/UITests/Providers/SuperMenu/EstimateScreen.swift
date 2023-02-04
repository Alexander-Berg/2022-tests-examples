//
//  EstimateFormScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 2/3/22.
//

import XCTest

final class EstimateFormScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "EstimateFormViewController"
    static let rootElementName = "Оценка автомобиля"

    enum Element: String {
        case submitButton = "Оценить автомобиль"
    }
}

final class EstimationResultScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "EstimationResultViewController"
    static let rootElementName = "Экран результата оценки стоимости"

    enum Element: String {
        case addGarageButton = "Добавить в гараж"
    }
}
