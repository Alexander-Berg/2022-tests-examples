import Foundation
import AutoRuDataUploadManager

extension DataUploadManagerTests {
    final class RepositoryWrapper: UploadTaskRepositoryType {
        struct Events {
            var requestsSaved: (([DataUploadRequest]) -> Void)?
        }

        let repository: UploadTaskRepositoryType
        var events = Events()

        init(repository: UploadTaskRepositoryType) {
            self.repository = repository
        }

        func getGroup(id: UUID) -> DataUploadGroup? {
            repository.getGroup(id: id)
        }

        func getRequestWithGroup(requestID: UUID) -> (DataUploadRequest, DataUploadGroup)? {
            repository.getRequestWithGroup(requestID: requestID)
        }

        func updateGroupOrCreate<Result>(byName: String, andHandlerID: String, _ update: (inout DataUploadGroup?) -> Result?) -> Result? {
            repository.updateGroupOrCreate(byName: byName, andHandlerID: andHandlerID, update)
        }

        func updateGroupIfExists<Result>(id: UUID, _ update: (inout DataUploadGroup) -> Result?) -> Result? {
            repository.updateGroupIfExists(id: id, update)
        }

        func saveRequests(_ infos: [DataUploadRequest]) {
            repository.saveRequests(infos)
            events.requestsSaved?(infos)
        }

        func getNextRequest(appRun: UUID) -> (DataUploadRequest, DataUploadGroup)? {
            repository.getNextRequest(appRun: appRun)
        }

        func setProcessing(for requestID: UUID, appRun: UUID?) {
            repository.setProcessing(for: requestID, appRun: appRun)
        }

        func removeRequestsForHandler(_ handler: String) {
            repository.removeRequestsForHandler(handler)
        }

        func removeRequest(id: UUID) {
            repository.removeRequest(id: id)
        }

        func removeGroup(id: UUID) {
            repository.removeGroup(id: id)
        }

        func requestIsCancelled(id: UUID) -> Bool {
            repository.requestIsCancelled(id: id)
        }

        func updateStatus(_ status: DataUploadRequest.Status, for requestID: UUID) {
            repository.updateStatus(status, for: requestID)
        }

        func saveURLResponse(_ body: Data?, response: Data?, for requestID: UUID) {
            repository.saveURLResponse(body, response: response, for: requestID)
        }

        func resetRequest(id: UUID) {
            repository.resetRequest(id: id)
        }

        func removeEmptyGroups() {
            repository.removeEmptyGroups()
        }

        func getRequestHierarchies() -> [(requestID: UUID, groupID: UUID, handlerID: String)] {
            repository.getRequestHierarchies()
        }

        func setTaskIdentifier(_ id: Int?, for requestID: UUID) {
            repository.setTaskIdentifier(id, for: requestID)
        }

        func findRequestByTaskIdentifier(_ id: Int) -> (request: DataUploadRequest, groupName: String, handlerID: String)? {
            repository.findRequestByTaskIdentifier(id)
        }

        func findGroup(byName: String, handlerID: String) -> DataUploadGroup? {
            repository.findGroup(byName: byName, handlerID: handlerID)
        }

        func getRequests(inGroup groupID: UUID) -> [DataUploadRequest] {
            repository.getRequests(inGroup: groupID)
        }

        func updateGroupIsTemporaryStatus(_ isTemporary: Bool, appRun: UUID, groupID: UUID) {
            repository.updateGroupIsTemporaryStatus(isTemporary, appRun: appRun, groupID: groupID)
        }

        func getFirstTemporaryGroup() -> DataUploadGroup? {
            repository.getFirstTemporaryGroup()
        }
    }
}
