import Foundation

extension Mocker {
    @discardableResult
    func mock_eventsLog() -> Self {
        server.addHandler("POST /events/log") { _, _ in
            Response.okResponse(fileName: "success")
        }
        return self
    }
}
