import XCTest

final class InsuranceFormScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "insurance_form"
    static let rootElementName = "Форма редактирования страховки"

    enum Element: String {
        case uploadButton = "upload_button"
        case photoPreview = "photo_preview"
        case retakePhoto = "retake_photo"
        case removePhoto = "remove_photo"
        case removeAlertButton = "Удалить"
        case saveButton = "save_button"
        case removeButton = "remove_button"
        case policyEdit = "policy_edit"
        case validFrom = "valid_from"
        case validTill = "valid_till"
        case companyName = "company_name"
        case companyPhone = "company_phone"
        case documentPicker = "document_picker"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
