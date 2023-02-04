import XCTest
@testable import AutoRuRateCall
import AutoRuProtoModels
import Snapshots

class RateCallQualityModuleTests: XCTestCase {
    struct Result: Equatable {
        var star: Int
        var reasons: Set<String>
    }

    private var module: RateCallQualityModule!

    private var rate: [Result] = []

    override func setUp() {
        super.setUp()

        rate = []

        module = RateCallQualityModule(onRated: { [weak self] star, reasons in
            self?.rate.append(Result(star: star, reasons: reasons))
        })
    }

    func test3StarsSnapshot() {
        module.viewModel.starCount = 3
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 300, height: 100))
    }

    func testReasonsSnapshot() {
        module.viewModel.starSelected(3)
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 400))
    }

    func testReasonsSnapshotWithSelectedReason() {
        module.viewModel.starSelected(3)
        module.viewModel.selectedReasons = ["Помехи"]
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 400))
    }

    func testShouldEmit5StarsAfter5StarTap() {
        module.viewModel.starSelected(5)
        XCTAssertEqual(rate, [.init(star: 5, reasons: [])])
    }

    func testShouldEmit3StarsWithReason() {
        module.viewModel.starSelected(3)
        module.viewModel.selectedReasons = ["Помехи"]
        module.viewModel.confirmed()
        XCTAssertEqual(rate, [.init(star: 3, reasons: ["Помехи"])])
    }
}
