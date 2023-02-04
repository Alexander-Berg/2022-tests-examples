import Foundation
import AutoRuDataUploadManager

extension DataUploadManagerTests {
    final class Handler: DataUploadHandler {
        struct Group: DataUploadHandlerGroup, Equatable, Codable {
            var isTemporary = false
            var name: String
        }

        struct Request: Equatable, Codable {
            var info = "some useful info"
        }

        struct ParsedURLResponse: Equatable, Codable {
            var payload = "Hello world"
        }

        struct Methods {
            var writeFile: ((Request, WriteFileOperationContext) async throws -> Void)?
            var makeURLRequest: ((Request, MakeURLRequestOperationContext) async throws -> URLRequest)?
            var parseURLResponse: ((Data, URLResponse?) throws -> ParsedURLResponse)?
            var uploadingCompleted: ((Request, Group, ParsedURLResponse, CompleteUploadingOperationContext) async throws -> Void)?
            var shouldRemoveRequest: ((Error, DataUploadManager.Stage) -> Bool)?
        }

        static let id: String = "Handler"

        func mergeGroup(_ group: inout Group, with prevGroup: Group) {
        }

        var fileContent: Data = {
            let string = (0..<100).map(\.description).joined()
            return Data(base64Encoded: string) ?? Data()
        }()

        let uploadURL = URL(string: "http://example.com/upload")!

        var methods = Methods()

        func writeFile(_ request: Request, context: WriteFileOperationContext) async throws {
            if let writeFile = methods.writeFile {
                try await writeFile(request, context)
                return
            }

            try! fileContent.write(to: context.fileURL)
        }

        func makeURLRequest(_ request: Request, context: MakeURLRequestOperationContext) async throws -> URLRequest {
            if let makeURLRequest = methods.makeURLRequest {
                return try await makeURLRequest(request, context)
            }

            return URLRequest(url: uploadURL)
        }

        func parseURLResponse(body: Data, urlResponse: URLResponse?) throws -> ParsedURLResponse {
            if let parseURLResponse = methods.parseURLResponse {
                return try parseURLResponse(body, urlResponse)
            }

            return try JSONDecoder().decode(ParsedURLResponse.self, from: body)
        }

        func getGroupStatus(_ group: Group, createdAtCurrentAppRun: Bool) -> DataUploadHandlerGroupStatus {
            if group.isTemporary {
                return .temporary
            }

            return .valid
        }

        func uploadingCompleted(_ request: Request, group: Group, response: ParsedURLResponse, context: CompleteUploadingOperationContext) async throws {
            if let uploadingCompleted = methods.uploadingCompleted {
                return try await uploadingCompleted(request, group, response, context)
            }
        }

        func shouldRemoveRequest(after error: Error, stage: DataUploadManager.Stage) -> Bool {
            methods.shouldRemoveRequest?(error, stage) ?? false
        }
    }
}
