import AutoRuProtoModels
import Snapshots
import SwiftProtobuf
import UIKit
import XCTest

final class GarageAddNewCardScreenTests: GarageCardBaseTests, KeyboardManaging {
    static let govNumberManually = "Y111KA164"

    func test_addNewCardScreenExist() {
        launch()
        openGarageCard()
            .should(.cardPhotos, .exist)
            .focus { snippet in
                snippet.swipe(.left)
            }
            .should(provider: .garageAddNewCardInSwipeScreen, .exist)
    }

    func test_addByGovNumber() {
        launch()
        openGarageCard()
            .should(.cardPhotos, .exist)
            .focus { snippet in
                snippet.swipe(.left)
            }
            .should(provider: .garageAddNewCardInSwipeScreen, .exist)
            .focus {
                $0.tap(.govNumberInput)
                    .wait(for: 1)
                    .do { typeFromKeyboard(Self.govNumberManually.lowercased()) }
                    .tap(.findButton)
            }
            .should(provider: .garageSearchScreen, .exist)
    }

    func test_addByVin() {
        launch()
        openGarageCard()
            .should(.cardPhotos, .exist)
            .focus { snippet in
                snippet.swipe(.left)
            }
            .should(provider: .garageAddNewCardInSwipeScreen, .exist)
            .focus {
                $0.tap(.addByVin)
            }
            .should(provider: .garageSearchScreen, .exist)
    }

    func test_addDreamCar() {
        launch()
        openGarageCard()
            .should(.cardPhotos, .exist)
            .focus { snippet in
                snippet.swipe(.left)
            }
            .should(provider: .garageAddNewCardInSwipeScreen, .exist)
            .focus {
                $0.tap(.addDreamCar)
            }
            .should(provider: .wizardMarkPicker, .exist)
    }

    func test_addExCar() {
        launch()
        openGarageCard()
            .should(.cardPhotos, .exist)
            .focus { snippet in
                snippet.swipe(.left)
            }
            .should(provider: .garageAddNewCardInSwipeScreen, .exist)
            .focus {
                $0.tap(.addDreamCar)
            }
            .should(provider: .wizardMarkPicker, .exist)
    }
}
