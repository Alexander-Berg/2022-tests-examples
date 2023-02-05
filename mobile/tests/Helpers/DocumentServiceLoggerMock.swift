import MarketFlexCore

final class DocumentServiceLoggerMock: DocumentServiceLogger {

    var loggedRequestErrors: [Error] = []

    var loggedDocumentDecodingErrors: [Error] = []

    func logRequestError(_ error: Error) {
        loggedRequestErrors.append(error)
    }

    func logDocumentDecodingError(_ error: Error) {
        loggedDocumentDecodingErrors.append(error)
    }
}
