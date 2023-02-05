import BeruHealthMetrics
import MarketProtocols
import MarketUnitTestHelpers
import OHHTTPStubs
import PromiseKit
import XCTest

@testable import BeruServices
@testable import Metrics

class ExperimentsServiceTests: NetworkingTestCase {

    let suppportedExperiments = Set(["EXP_CONTROL", "EXP_OFF", "EXP_EXP_EXP"])

    var service: ExperimentsServiceImpl!
    var fapiService: ExperimentsServiceImpl!
    var startupIdentifiersServiceMock: StartupIdentifiersServiceMock!

    override func setUp() {
        super.setUp()

        startupIdentifiersServiceMock = StartupIdentifiersServiceMock()
        service = ExperimentsServiceImpl(
            apiClient: DependencyProvider().apiClient,
            startupIdentifiersService: startupIdentifiersServiceMock,
            isFAPIExperimentsEnabled: false
        )
        fapiService = ExperimentsServiceImpl(
            apiClient: DependencyProvider().apiClient,
            startupIdentifiersService: startupIdentifiersServiceMock,
            isFAPIExperimentsEnabled: true
        )
        MetricRecorder.isRecording = true
    }

    override func tearDown() {
        service = nil
        fapiService = nil
        startupIdentifiersServiceMock = nil
        service = nil
        MetricRecorder.isRecording = true
        MetricRecorder.clear()

        super.tearDown()
    }

    // MARK: - CAPI

    func test_shouldSendProperRequestAndReturnExperiments_whenRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "startup",
            responseFileName: "startup_response",
            testBlock: isMethodPOST() && verifyJsonBody(isBodyValid(_:))
        )

        startupIdentifiersServiceMock.obtainStartupIdentifiersReturnValue = .value([:])

        // when
        let result = service.obtainExperiments(supportedExperiments: suppportedExperiments, forcedExperiments: [])
            .expect(in: self, timeout: 5)

        // then
        let experiments = try result.get()
        XCTAssertFalse(experiments.isEmpty)
    }

    func test_shouldReturnError_whenRequestFailed() {
        // given
        stubError(requestPartName: "startup", code: 415)

        startupIdentifiersServiceMock.obtainStartupIdentifiersReturnValue = .value([:])

        // when
        let result = service.obtainExperiments(supportedExperiments: [], forcedExperiments: [])
            .expect(in: self, timeout: 5)

        // then
        XCTAssertThrowsError(try result.get())
    }

    // MARK: - FAPI

    func test_shouldSendProperFAPIRequestAndReturnExperiments_whenRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "resolveAppExperiments",
            responseFileName: "resolveAppExperiments_response",
            testBlock: isMethodPOST() && verifyJsonBody(isFAPIBodyValid(_:))
        )

        startupIdentifiersServiceMock.obtainStartupIdentifiersReturnValue = .value([:])

        // when
        let result = fapiService.obtainExperiments(
            supportedExperiments: suppportedExperiments,
            forcedExperiments: []
        ).expect(in: self)

        // then
        let experiments = try result.get()
        XCTAssertFalse(experiments.isEmpty)
    }

    func test_shouldReturnError_whenFAPIRequestFailed() {
        // given
        stubError(requestPartName: "resolveAppExperiments", code: 415)

        startupIdentifiersServiceMock.obtainStartupIdentifiersReturnValue = .value([:])

        // when
        let result = service.obtainExperiments(supportedExperiments: [], forcedExperiments: [])
            .expect(in: self, timeout: 5)

        // then
        XCTAssertThrowsError(try result.get())
    }

    func test_shouldSendHealthEvent_whenRequestCompleted() {
        // given
        let expectedEventParams: [AnyHashable: AnyHashable] = [
            "name": "GET_EXPERIMENTS",
            "portion": "CORE"
        ]

        stub(
            requestPartName: "startup",
            responseFileName: "startup_response",
            testBlock: isMethodPOST() && verifyJsonBody(isBodyValid(_:))
        )

        startupIdentifiersServiceMock.obtainStartupIdentifiersReturnValue = .value([:])

        // when
        _ = service.obtainExperiments(supportedExperiments: suppportedExperiments, forcedExperiments: [])
            .expect(in: self)

        // then
        wait {
            MetricRecorder.events(from: .health).with(params: expectedEventParams).isNotEmpty
        }
    }

    func test_shouldReturnError_whenStartupIdentifiersRequestFailed() throws {
        // given
        let expectedError = NSError(domain: NSURLErrorDomain, code: -1_011, userInfo: nil)
        startupIdentifiersServiceMock.obtainStartupIdentifiersReturnValue = Promise(error: expectedError)

        // when
        let result = service.obtainExperiments(supportedExperiments: suppportedExperiments, forcedExperiments: [])
            .expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as NSError, expectedError)
        }
    }

    // MARK: - Helpers

    private func isBodyValid(_ body: [AnyHashable: Any]) -> Bool {
        guard let experiments = body["supportedExperiments"] as? [String] else {
            return false
        }
        return suppportedExperiments.elementsEqual(experiments)
    }

    private func isFAPIBodyValid(_ body: [AnyHashable: Any]) -> Bool {
        guard
            let params = (body["params"] as? [[AnyHashable: Any]])?.first,
            let experiments = params["supportedExperiments"] as? [String]
        else {
            return false
        }

        let lowercasedExperiments = suppportedExperiments.map { $0.lowercased() }
        return lowercasedExperiments.elementsEqual(experiments)
    }
}

// MARK: - Mocks

extension ExperimentsServiceTests {
    class StartupIdentifiersServiceMock: StartupIdentifiersService {
        var obtainStartupIdentifiersReturnValue: Promise<[AnyHashable: Any]>!

        func obtainStartupIdentifiers() -> Promise<[AnyHashable: Any]> {
            obtainStartupIdentifiersReturnValue
        }
    }
}
