import XCTest
import GRDB
@testable import AutoRuDataUploadManager
import Dispatch
import Foundation

final class DataUploadManagerTests: XCTestCase {

    struct TestError: Error, Equatable {
        let stage: DataUploadManager.Stage
    }

    var target: DataUploadManager!
    var handler: Handler!
    var observer: Observer!
    var repository: UploadTaskRepository!
    var repositoryWrapper: RepositoryWrapper!
    static let groupName = "groupName"
    let observerQueue = DispatchQueue(label: "observerQueue", qos: .userInitiated)

    override func setUp() async throws {
        target = DataUploadManager()
        handler = Handler()
        observer = Observer()

        let databaseQueue = DatabaseQueue()
        try! databaseQueue.write { db in
            try DataUploadGroup.createTable(in: db)
            try DataUploadRequest.createTable(in: db)
        }
        repository = UploadTaskRepository(dbWriter: databaseQueue)

        repositoryWrapper = RepositoryWrapper(repository: repository)

        target.register(handler)
        await target.setUp(with: repositoryWrapper, urlProtocol: MockURLProtocol.self)
    }

    func testNotifyObserverAboutCurrentPendingStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { $0.status = .pending },
            getExpectedEvents: { groupID, requestID in
                [.didAddUploads([.init(request: Handler.Request(), id: requestID)], groupID: groupID)]
            })
    }

    func testNotifyObserverAboutCurrentFileWrittenStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { $0.status = .fileWritten },
            getExpectedEvents: { groupID, requestID in
                [ObserverEvent.didStartPreparing(Handler.Request(), requestID: requestID, groupID: groupID)]
            })
    }

    func testNotifyObserverAboutCurrentFileWriteFailedStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { $0.status = .fileWriteFailed },
            getExpectedEvents: { groupID, requestID in
                [ObserverEvent.fail(
                    .init(
                        request: Handler.Request(),
                        error: nil,
                        stage: .writeToFile,
                        requestID: requestID,
                        groupID: groupID,
                        isRemoved: false
                    )
                )]
            })
    }

    func testNotifyObserverAboutCurrentMakeURLRequestFailedStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { $0.status = .makeURLRequestFailed },
            getExpectedEvents: { groupID, requestID in
                [ObserverEvent.fail(
                    .init(
                        request: Handler.Request(),
                        error: nil,
                        stage: .makeURLRequest,
                        requestID: requestID,
                        groupID: groupID,
                        isRemoved: false
                    )
                )]
            })
    }

    func testNotifyObserverAboutCurrentFileUploadedStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { request in
                request.status = .fileUploaded
                request.urlResponseBody = try! JSONEncoder().encode(Handler.ParsedURLResponse())
            },
            getExpectedEvents: { groupID, requestID in
                [ObserverEvent.didUpload(
                    .init(
                        request: Handler.Request(),
                        requestID: requestID,
                        groupID: groupID,
                        response: Handler.ParsedURLResponse()
                    )
                )]
            })
    }

    func testNotifyObserverAboutCurrentFileUploadingFailedStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { $0.status = .fileUploadingFailed },
            getExpectedEvents: { groupID, requestID in
                [ObserverEvent.fail(
                    .init(
                        request: Handler.Request(),
                        error: nil,
                        stage: .urlRequestProcessing,
                        requestID: requestID,
                        groupID: groupID,
                        isRemoved: false
                    )
                )]
            })
    }

    func testNotifyObserverAboutCurrentCompletionFailedStatus() {
        validateEventsAfterAddingObserver(
            updateRequest: { $0.status = .completionFailed },
            getExpectedEvents: { groupID, requestID in
                [ObserverEvent.fail(
                    .init(
                        request: Handler.Request(),
                        error: nil,
                        stage: .uploadingCompleted,
                        requestID: requestID,
                        groupID: groupID,
                        isRemoved: false
                    )
                )]
            })
    }

    func testNotifyObserverDuringUploading() {
        let group = Handler.Group(name: Self.groupName)
        let request = Handler.Request()

        var requestID: UUID?
        var groupID: UUID?

        let expectation = XCTestExpectation()

        observer.recordedEventsChanged = { events in
            guard events.count == 6 else { return }
            guard let requestID = requestID, let groupID = groupID else {
                XCTFail("requestID and groupID should not be nil")
                return
            }

            XCTAssertEqual(events[0], .groupChanged(group, groupID: groupID))
            XCTAssertEqual(events[1], .didAddUploads([.init(request: request, id: requestID)], groupID: groupID))
            XCTAssertEqual(events[2], .didStartPreparing(request, requestID: requestID, groupID: groupID))
            XCTAssertEqual(events[3], .didStartUploading(request, requestID: requestID, groupID: groupID))
            let didUploadEvent = ObserverEvent.DidUploadEvent(
                request: request,
                requestID: requestID,
                groupID: groupID,
                response: Handler.ParsedURLResponse()
            )
            XCTAssertEqual(events[4], .didUpload(didUploadEvent))
            XCTAssertEqual(events[5], .didComplete(request, requestID: requestID, groupID: groupID))

            XCTAssertEqual(self.repository.getRequests(inGroup: groupID).count, 0)

            expectation.fulfill()
        }

        addObserver(startWithCurrentStatuses: false)

        MockURLProtocol.requestHandler = { request in
            let data = try! JSONEncoder().encode(Handler.ParsedURLResponse())
            return (URLResponse(), data)
        }

        repositoryWrapper.events.requestsSaved = { requests in
            requestID = requests.first?.id
            groupID = requests.first?.groupID
        }

        target.upload(handlerType: Handler.self, group: group, requests: [request])

        wait(for: [expectation], timeout: 3)
    }

    func testWriteToFileErrorHandling() {
        let error = TestError(stage: .writeToFile)
        handler.methods.writeFile = { _, operation in
            throw error
        }

        validateError(error)
    }

    func testMakeURLRequestErrorHandling() {
        let error = TestError(stage: .makeURLRequest)
        handler.methods.makeURLRequest = { _, operation in
            throw error
        }

        validateError(error)
    }

    func testExecuteURLRequestErrorHandling() {
        let error = TestError(stage: .urlRequestProcessing)
        MockURLProtocol.requestHandler = { _ in
            throw error
        }

        validateError(error)
    }

    func testParseURLResponseErrorHandling() {
        let error = TestError(stage: .parseURLResponse)
        handler.methods.parseURLResponse = { _, _ in
            throw error
        }

        validateError(error)
    }

    func testUploadingCompletedErrorHandling() {
        let error = TestError(stage: .uploadingCompleted)
        handler.methods.uploadingCompleted = { _, _, _, operation in
            throw error
        }

        validateError(error)
    }

    func testGroupTemporaryState() {
        let group = Handler.Group(isTemporary: true, name: Self.groupName)
        let request = Handler.Request()

        var requestID: UUID?
        var groupID: UUID?

        let didNotCompleteExpectation = XCTestExpectation(description: "didComplete should not be called")
        didNotCompleteExpectation.isInverted = true

        observer.recordedEventsChanged = { events in
            guard let requestID = requestID, let groupID = groupID else {
                return
            }

            let didCompleteEvent = ObserverEvent.didComplete(request, requestID: requestID, groupID: groupID)

            if events.contains(didCompleteEvent) {
                didNotCompleteExpectation.fulfill()
            }
        }

        addObserver(startWithCurrentStatuses: false)

        MockURLProtocol.requestHandler = { request in
            let data = try! JSONEncoder().encode(Handler.ParsedURLResponse())
            return (URLResponse(), data)
        }

        repositoryWrapper.events.requestsSaved = { requests in
            requestID = requests.first?.id
            groupID = requests.first?.groupID
        }

        target.upload(handlerType: Handler.self, group: group, requests: [request])

        wait(for: [didNotCompleteExpectation], timeout: 3)

        let didCompleteExpectation = XCTestExpectation(description: "didComplete should be called")

        observer.recordedEvents.removeAll()
        observer.recordedEventsChanged = { events in
            guard events.count == 2 else { return }

            XCTAssertEqual(events[0], .groupChanged(Handler.Group(isTemporary: false, name: Self.groupName), groupID: groupID!))
            XCTAssertEqual(events[1], .didComplete(request, requestID: requestID!, groupID: groupID!))

            didCompleteExpectation.fulfill()
        }

        Task { [groupID] in
            let _ = await target.updateGroup(handlerType: Handler.self, groupID: groupID!) { group in
                group.isTemporary = false
                return true
            }
        }

        wait(for: [didCompleteExpectation], timeout: 3)
    }

    private func validateEventsAfterAddingObserver(
        updateRequest: (inout DataUploadRequest) -> Void,
        getExpectedEvents: (_ groupID: UUID, _ requestID: UUID) -> [ObserverEvent]
    ) {
        let group = makeDataUploadGroup()

        let request = makeDataUploadRequest {
            $0.groupID = group.id
            updateRequest(&$0)
        }

        var expectedEvents = getExpectedEvents(group.id, request.id)
        expectedEvents.insert(.groupChanged(Handler.Group(name: Self.groupName), groupID: group.id), at: 0)

        repository.updateGroupOrCreate(byName: group.name, andHandlerID: group.handlerID) {
            $0 = group
        }
        repository.saveRequests([request])

        let expectation = XCTestExpectation()

        observer.recordedEventsChanged = { events in
            if events == expectedEvents {
                expectation.fulfill()
            }
        }

        addObserver(startWithCurrentStatuses: true)

        wait(for: [expectation], timeout: 3)
    }

    private func addObserver(startWithCurrentStatuses: Bool) {
        target.addObserver(
            observer,
            groupName: Self.groupName,
            queue: observerQueue,
            startWithCurrentStatuses: startWithCurrentStatuses
        )
    }

    private func makeDataUploadGroup(
        group: Handler.Group = .init(name: groupName),
        _ mutate: (inout DataUploadGroup) -> Void = { _ in }
    ) -> DataUploadGroup {
        var group = DataUploadGroup(
            id: UUID(),
            name: Self.groupName,
            handlerID: Handler.id,
            appRun: UUID(),
            groupData: try! handler.encode(group: group),
            isTemporary: false,
            appRunWhenIsTemporaryUpdated: UUID()
        )

        mutate(&group)

        return group
    }

    private func makeDataUploadRequest(
        request: Handler.Request = .init(),
        _ mutate: (inout DataUploadRequest) -> Void = { _ in }
    ) -> DataUploadRequest {
        var request = DataUploadRequest(
            id: UUID(),
            groupID: UUID(),
            requestData: try! handler.encode(request: request),
            created: Date(),
            order: 0
        )

        mutate(&request)

        return request
    }

    private func validateError(_ expectedError: TestError) {
        let group = Handler.Group(name: Self.groupName)
        let request = Handler.Request()

        var requestID: UUID?
        var groupID: UUID?

        let observerExpectation = XCTestExpectation()
        let handlerExpectation = XCTestExpectation()

        observer.recordedEventsChanged = { events in
            guard let requestID = requestID, let groupID = groupID else {
                return
            }

            let failEvent = ObserverEvent.FailEvent(
                request: request,
                error: expectedError,
                stage: expectedError.stage,
                requestID: requestID,
                groupID: groupID,
                isRemoved: true
            )

            if events.contains(.fail(failEvent)) {
                observerExpectation.fulfill()
            }
        }

        addObserver(startWithCurrentStatuses: false)

        if MockURLProtocol.requestHandler == nil {
            MockURLProtocol.requestHandler = { request in
                let data = try! JSONEncoder().encode(Handler.ParsedURLResponse())
                return (URLResponse(), data)
            }
        }

        repositoryWrapper.events.requestsSaved = { requests in
            requestID = requests.first?.id
            groupID = requests.first?.groupID
        }

        handler.methods.shouldRemoveRequest = { error, stage in
            guard let error = error as? TestError else {
                XCTFail("wrong error type.")
                return true
            }

            XCTAssertEqual(error, expectedError)

            handlerExpectation.fulfill()
            return true
        }

        target.upload(handlerType: Handler.self, group: group, requests: [request])

        wait(for: [observerExpectation, handlerExpectation], timeout: 3)
    }
}
