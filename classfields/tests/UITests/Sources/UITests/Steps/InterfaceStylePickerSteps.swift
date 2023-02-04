import XCTest
import Snapshots

final class InterfaceStylePickerSteps: BaseSteps {
    @discardableResult
    func select(style: AppearanceStyle) -> Self {
        step("Выбираем в пикере тему '\(style.rawValue)'") {
            baseScreen.find(by: style.rawValue).firstMatch.tap()
        }
    }

    @discardableResult
    func checkPicker(style: AppearanceStyle) -> Self {
        step("Проверяем что выбрана тема '\(style.rawValue)'") {
            let screenshot = baseScreen.find(by: "ModalViewControllerHost").element(boundBy: 1).waitAndScreenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: "interface_style_picker_\(style.screenshotId)")
        }
    }

    enum AppearanceStyle: String {
        case light = "Светлая"
        case dark = "Тёмная"
        case auto = "Системная"

        var screenshotId: String {
            switch self {
            case .light: return "light"
            case .dark: return "dark"
            case .auto: return "system"
            }
        }
    }
}
