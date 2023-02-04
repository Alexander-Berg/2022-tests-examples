import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

class GarageCardBaseTests: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    static let garageCard: (vin: String, id: String) = ("XTAF5015LE0773148", "1955418404")

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        mocker.mockGarageCard()
        mocker.startMock()
    }

    func openGarageCard() -> GarageCardSteps {
        return mainSteps
            .openTab(.garage)
            .as(GarageCardSteps.self)
            .shouldSeeCard()
    }
}
