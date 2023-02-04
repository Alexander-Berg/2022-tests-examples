//
//  RegionPickerScreen.swift
//  UITests
//
//  Created by Igor Shamrin on 02.12.2021.
//

final class RegionPickerScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "region_picker_view"
    static let rootElementName = "Экран выбора региона"

    enum Element {
        case pickerItem(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .pickerItem(let title):
            return title
        }
    }
}
