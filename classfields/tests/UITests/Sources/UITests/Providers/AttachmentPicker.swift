import XCTest

final class AttachmentPicker: BaseSteps, UIRootedElementProvider {
    enum Element {
        case photo
        case location
        case systemImage(Int)
        case send
        case camera
    }

    static let rootElementID = "AttachmentsPickerViewController"
    static let rootElementName = "Пикер аттачей"

    func identifier(of element: Element) -> String {
        switch element {
        case .photo:
            return "Все фотографии"
        case .location:
            return "Отправить местоположение"
        case let .systemImage(index):
            return "image_cell_\(index)"
        case .send:
            return "send_photos"
        case .camera:
            return "camera_cell"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}

final class PHPicker: BaseSteps, UIRootedElementProvider {
    enum Element {}

    static let rootElementID = "PHPickerViewController"
    static let rootElementName = "Пикер фоток"
}
