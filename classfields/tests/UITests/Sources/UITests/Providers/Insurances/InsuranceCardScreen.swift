import XCTest

final class InsuranceCardScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "insurance_card"
    static let rootElementName = "Карточка страховки"

    enum Element {
        case uploadButton
        case photoPreview
        case retakePhoto
        case removePhoto
        case removePhotoAlertButton
        case edit
        case close
        case documentPicker
        case field(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .uploadButton:
            return "upload_button"
        case .photoPreview:
            return "photo_preview"
        case .retakePhoto:
            return "retake_photo"
        case .removePhoto:
            return "remove_photo"
        case .removePhotoAlertButton:
            return "Удалить"
        case .edit:
            return "edit"
        case .close:
            return "nav_close_button"
        case .documentPicker:
            return "document_picker"
        case let .field(value):
            return value
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
