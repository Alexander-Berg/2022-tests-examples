import Foundation
import AutoRuUtils
import AutoRuDataUploadManager

extension DataUploadManagerTests {
    enum ObserverEvent: Equatable {
        struct RequestWithID: Equatable {
            let request: Handler.Request
            let id: UUID
        }

        struct FailEvent: Equatable {
            var request: Handler.Request
            // swiftlint:disable redundant_optional_initialization
            @EqualityOverride(areEqual: { $0?.localizedDescription == $1?.localizedDescription })
            var error: Error? = nil
            // swiftlint:enable redundant_optional_initialization
            var stage: DataUploadManager.Stage
            var requestID: UUID
            var groupID: UUID
            var isRemoved: Bool
        }

        struct DidUploadEvent: Equatable {
            var request: Handler.Request
            var requestID: UUID
            var groupID: UUID
            var response: Handler.ParsedURLResponse
        }

        struct DidMakeProgressEvent: Equatable {
            var request: Handler.Request
            var requestID: UUID
            var groupID: UUID
            var bytesSend: Int64
            var totalBytes: Int64
        }

        case groupChanged(Handler.Group, groupID: UUID)
        case didStartPreparing(Handler.Request, requestID: UUID, groupID: UUID)
        case didAddUploads([RequestWithID], groupID: UUID)
        case didStartUploading(Handler.Request, requestID: UUID, groupID: UUID)
        case didMakeProgress(DidMakeProgressEvent)
        case didUpload(DidUploadEvent)
        case fail(FailEvent)
        case didComplete(Handler.Request, requestID: UUID, groupID: UUID)
    }

    final class Observer: DataUploadObserver {
        typealias Handler = DataUploadManagerTests.Handler
        typealias Request = Handler.Request

        var recordedEvents: [ObserverEvent] = []
        var recordedEventsChanged: (([ObserverEvent]) -> Void)?

        func dataUploadManagerGroupChanged(_ group: Handler.Group, context: DataUploadContext) {
            record(.groupChanged(group, groupID: context.groupID))
        }

        func dataUploadManagerDidAddUploads(requests: [(request: Request, id: UUID)], context: DataUploadContext) {
            record(.didAddUploads(requests.map { .init(request: $0.request, id: $0.id) }, groupID: context.groupID))
        }

        func dataUploadManagerDidStartPreparing(request: Request, requestID: UUID, context: DataUploadContext) {
            record(.didStartPreparing(request, requestID: requestID, groupID: context.groupID))
        }

        func dataUploadManagerDidStartUploading(request: Request, requestID: UUID, context: DataUploadContext) {
            record(.didStartUploading(request, requestID: requestID, groupID: context.groupID))
        }

        func dataUploadManagerDidMakeProgress(request: Request, requestID: UUID, bytesSend: Int64, totalBytes: Int64, context: DataUploadContext) {
            record(.didMakeProgress(.init(request: request, requestID: requestID, groupID: context.groupID, bytesSend: bytesSend, totalBytes: totalBytes)))
        }

        func dataUploadManagerDidUpload(request: Request, requestID: UUID, response: Handler.ParsedURLResponse, context: DataUploadContext) {
            record(.didUpload(.init(request: request, requestID: requestID, groupID: context.groupID, response: response)))
        }

        func dataUploadManagerRequest(
            _ request: Request,
            failedWith error: Error?,
            stage: DataUploadManager.Stage,
            requestID: UUID,
            isRemoved: Bool,
            context: DataUploadContext
        ) {
            record(
                .fail(
                    .init(
                        request: request,
                        error: error,
                        stage: stage,
                        requestID: requestID,
                        groupID: context.groupID,
                        isRemoved: isRemoved
                    )
                )
            )
        }

        func dataUploadManagerDidComplete(request: Request, requestID: UUID, context: DataUploadContext) {
            record(.didComplete(request, requestID: requestID, groupID: context.groupID))
        }

        private func record(_ event: ObserverEvent) {
            recordedEvents.append(event)
            recordedEventsChanged?(recordedEvents)
        }
    }
}
