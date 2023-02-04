import Foundation
import XCTest
import Snapshots

/// @depends_on AutoRuGarage AutoRu
final class InterfaceStylePickerTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    private var settings: [String: Any] = [:]

    override var appSettings: [String: Any] {
        var settings = self.settings
        settings["interfaceStylePickerAvailable"] = true
        return settings
    }

    func test_switchFromLightToDark() {
        Step("Переключаемся со светлой на темную тему")

        var settings = super.appSettings
        settings["interfaceStyle"] = 1
        self.settings = settings

        self.openPicker()
            .checkPicker(style: .light)
            .select(style: .dark)
            .checkPicker(style: .dark)
    }

    func test_switchFromDarkToLight() {
        Step("Переключаемся с темной на светлую тему")

        var settings = super.appSettings
        settings["interfaceStyle"] = 2
        self.settings = settings

        self.openPicker()
            .checkPicker(style: .dark)
            .select(style: .light)
            .checkPicker(style: .light)
    }

    // MARK: - Private

    private func openPicker() -> InterfaceStylePickerSteps {
        launchMain()
            .container
            .focus(on: .tabBar, ofType: .tabBar) {
                $0.tap(.tab(.favorites))
            }
            .should(provider: .navBar, .exist)
            .focus { $0.tap(.superMenuButton) }
            .should(provider: .superMenuScreen, .exist)
            .focus {
                $0.scroll(to: .themeSettings, direction: .up)
                $0.tap(.themeSettings)
            }
        return InterfaceStylePickerSteps(context: self)
    }
}
