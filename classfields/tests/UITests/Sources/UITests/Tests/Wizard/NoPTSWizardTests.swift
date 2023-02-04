import XCTest

final class NoPTSWizardTests: BaseTest, WizardTraversable {
    private static let draftID = "1640897598478615376-2c0470c2"
    
    override func setUp() {
        super.setUp()
        setupServer()
    }
    
    func test_NoPtsOptionSelect() {
        openUserSaleList()
            .step("Открываем пикер категорий") { $0
                .should(provider: .userSaleListScreen, .exist)
                .focus { screen in
                    screen.tap(.placeFreeLabel)
                }
            }
            .step("Открываем визард для авто") { $0
                .should(provider: .categoryPicker, .exist)
                .focus { screen in
                    screen.tap(.auto)
                }
            }
            .should(provider: .wizardScreen, .exist)
            .focus { wizard in
                wizard
                    .step("Проходим визард до шага с ПТС") {
                        traverseWizard(for: Self.draftID, from: wizard, to: .pts)
                    }
                    .should(provider: .wizardPTSPicker, .exist)
                    .focus { screen in
                        screen.focus(on: .wizardItem("Нет ПТС")) { item in
                            item.tap()
                        }
                    }
            }
            .step("Показался пикер панорамы, минув пикер с выбором кол-ва владельцев") { $0
                .should(provider: .wizardDescriptionPanoramaPicker, .exist)
            }
    }
    
    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .startMock()
        
        setupMocksForWizard(draftID: Self.draftID)
    }
    
    private func openUserSaleList() -> UserSaleListScreen {
        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/my"),
            overrideAppSettings: [:]
        )
        return launch(on: .userSaleListScreen, options: options)
    }
}
