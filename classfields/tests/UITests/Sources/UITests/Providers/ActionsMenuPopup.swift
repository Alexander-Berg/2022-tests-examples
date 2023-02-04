import XCTest
import Snapshots

final class ActionsMenuPopup: BaseSteps, UIRootedElementProvider {
    static var rootElementID: String = "actions_menu_view"
    static var rootElementName: String = "Меню с действиями"

    enum Element: String {
        case container = "actions_menu_view"
        case writeNote = "Написать заметку"
        case share = "Поделиться"
        case copyLink = "Скопировать ссылку"
        case downloadOffer = "Скачать объявление"
        case complain = "Пожаловаться на объявление"
        case osago = "ОСАГО"
        case kasko = "Каско"
        case chooseFromGallery = "Выбрать из фотогалереи"
        case chooseFile = "Выбрать PDF-файл"

        case dismissButton = "dismiss_modal_button"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
