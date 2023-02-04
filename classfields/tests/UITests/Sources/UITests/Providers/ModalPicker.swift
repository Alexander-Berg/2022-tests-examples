final class ModalPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "modal_picker_view_container"
    static let rootElementName = "Модальный пикер"

    enum Element {
        case item(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .item(let value):
            return value
        }
    }
}
