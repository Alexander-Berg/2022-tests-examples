import MarketFlexCore

// swiftlint:disable implicitly_unwrapped_optional

final class DocumentServiceErrorMock: DocumentService {

    var error: Error!

    func obtainDocument(_ request: DocumentRequest) async throws -> Document {
        throw error
    }
}
