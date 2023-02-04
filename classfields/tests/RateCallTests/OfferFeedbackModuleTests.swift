import XCTest
import AutoRuProtoModels
@testable import AutoRuRateCall
import Snapshots
import Combine

class OfferFeedbackModuleTests: XCTestCase {

    private var module: OfferFeedbackModule!
    private var complaintServiceMock = ComplaintServiceMock()
    private var cancellables: Set<AnyCancellable> = []
    private var moduleResults: [OfferFeedbackModule.Result] = []

    override func setUp() {
        super.setUp()

        cancellables = []
        complaintServiceMock = ComplaintServiceMock()
        module = makeModule(offer: Auto_Api_Offer())
    }

    func testSnapshotNotAvailableForCheckup() {
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 450))
    }

    func testSnapshotAvailableForCheckup() {
        let offer = Auto_Api_Offer.with { $0.tags.append("available_for_checkup") }
        module = makeModule(offer: offer)
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 450))
    }

    func testSnapshotCommercial() {
        let offer = Auto_Api_Offer.with { $0.sellerType = .commercial }
        module = makeModule(offer: offer)
        Snapshot.compareWithSnapshot(module.viewController, size: CGSize(width: 400, height: 450))
    }

    func testShouldOpenCompaintIfClaimButtonTapped() {
        module.buttonTap(.claim)
        XCTAssertEqual(moduleResults, [.openComplaint])
    }

    func testShouldOpenChatIfCheckupButtonTapped() {
        module.buttonTap(.checkup)
        XCTAssertEqual(moduleResults, [.openChat])
    }

    func testShouldSendCompaintIfSoldButtonTapped() {
        module.buttonTap(.sold)
        XCTAssertEqual(complaintServiceMock.input, [.init(reason: .sold)])
    }

    func testShouldSendComplaintIfNoAnswerButtonTapped() {
        module.buttonTap(.noAnswer)
        XCTAssertEqual(complaintServiceMock.input, [.init(reason: .noAnswer)])
    }

    private func makeModule(offer: Auto_Api_Offer) -> OfferFeedbackModule {
        let module = OfferFeedbackModule(
            offer: offer,
            complaintService: complaintServiceMock
        )

        module.result.sink(receiveValue: { [weak self] result in
            self?.moduleResults.append(result)
        }).store(in: &cancellables)

        return module
    }
}
