final class MicrophonePermissionScreen: BaseSteps, UIRootedElementProvider {
    enum Description: String {
        case forBuyer = "Так вы сможете общаться с теми, кто предпочитает звонить через Авто.ру"
        case forSeller = "Так вы не упустите покупателей, которые предпочитают звонить через Авто.ру"
    }

    enum Element {
        case settingsButton
        case cancelButton
        case description(Description)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .settingsButton:
            return "Настройки"

        case .cancelButton:
            return "Отмена"

        case let .description(description):
            return description.rawValue
        }
    }

    static let rootElementID = "microphone_permission"
    static let rootElementName = "Шторка с предложением перейти в настройки для включения микрофона"
}
