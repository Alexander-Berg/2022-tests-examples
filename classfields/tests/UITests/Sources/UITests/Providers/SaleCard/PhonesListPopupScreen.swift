final class PhonesListPopupScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID: String = "phone_list_popup"
    static let rootElementName: String = "Popup выбора телефона"

    enum Element {
        case phoneRow(number: String)
        case addPhoneButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .phoneRow(let number):
            return number
        case .addPhoneButton:
            return "add_phone_button"
        }
    }
}
