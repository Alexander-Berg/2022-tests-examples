import XCTest
@testable import AutoRuRateCall
import AutoRuProtoModels
import Snapshots
import Combine

class SaleComplaintModuleTests: XCTestCase {
    var module: SaleComplaintModule!
    private var complaintServiceMock = ComplaintServiceMock()
    private var cancellables: Set<AnyCancellable> = []
    private var moduleResults: [Void] = []

    override func setUp() {
        super.setUp()

        complaintServiceMock = ComplaintServiceMock()

        module = SaleComplaintModule(
            offer: Auto_Api_Offer(),
            complaintService: complaintServiceMock
        )

        cancellables = []

        module.completed.sink(receiveValue: { [weak self] result in
            self?.moduleResults.append(result)
        }).store(in: &cancellables)
    }

    func testComplaintReasonsSnapshot() {
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 400))
    }

    func testInputAnotherComplaintSnapshot() {
        module.viewModel.selectedReason = .another
        module.viewModel.text = "Lorem, ipsum dolor sit amet consectetur adipisicing elit. Sit rem fugiat nulla hic tempore nam accusantium cumque eius praesentium sequi?"
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 400))
    }

    func testEmptyInputAnotherComplaintSnapshot() {
        module.viewModel.selectedReason = .another
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 400))
    }

    func testShouldSendComplaintAfterButtonTap() {
        module.viewModel.selectedReason = .priceError

        XCTAssertEqual(complaintServiceMock.input, [.init(reason: .priceError, text: nil)], "Должна произойти отправка .priceError")
    }

    func testShouldSendAnotherComplaintWithTextAfterButtonTap() {
        module.viewModel.selectedReason = .another
        let text = "Lorem, ipsum dolor sit amet consectetur adipisicing elit."
        module.viewModel.text = text
        module.viewModel.sendAnotherComplaint()

        XCTAssertEqual(complaintServiceMock.input, [.init(reason: .another, text: text)], "Должна произойти отправка .another")
    }

    func testShouldEmitResultAfterSelectingComplaint() {
        module.viewModel.selectedReason = .priceError

        XCTAssert(!moduleResults.isEmpty)
    }
}
