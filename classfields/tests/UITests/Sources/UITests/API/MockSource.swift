import XCTest
import SwiftProtobuf

struct MockSource<Request: SwiftProtobuf.Message, Response: SwiftProtobuf.Message> {
    typealias Mutation = (inout Response) -> Void
    typealias DynamicInit = (RequestEndpointInfo, Request) throws -> Response

    struct RequestEndpointInfo {
        let method: HTTPMethod
        let path: String
        let request: UITests.Request
    }

    private let factory: DynamicInit
    private var headers: [String: String] = [:]

    static func file(_ file: String, mutation: Mutation? = nil) -> MockSource<Request, Response> {
        return .init { _, _ in
            var model = Response(mockFile: file)
            mutation?(&model)
            return model
        }
    }

    static func model(_ model: Response = Response(), mutation: Mutation? = nil) -> MockSource<Request, Response> {
        return .init { _, _ in
            var model = model
            mutation?(&model)
            return model
        }
    }

    static func `dynamic`(_ init: @escaping DynamicInit) -> MockSource<Request, Response> {
        return .init(`init`)
    }

    private init(_ mock: @escaping DynamicInit) {
        self.factory = mock
    }

    func makeResponse(withInfo info: RequestEndpointInfo, statusCode: String) throws -> UITests.Response {
        var encodingOptions = JSONEncodingOptions()
        encodingOptions.preserveProtoFieldNames = true

        var decodingOptions = JSONDecodingOptions()
        decodingOptions.ignoreUnknownFields = true

        let request: Request

        if let body = info.request.messageBody {
            request = (try? Request(jsonUTF8Data: body, options: decodingOptions)) ?? Request()
        } else {
            request = Request()
        }

        let model = try factory(info, request)

        let data = try model.jsonUTF8Data(options: encodingOptions)

        let response = UITests.Response.responseWithStatus(body: data, status: "HTTP/1.1 \(statusCode)")

        for (header, value) in headers {
            response.headers[header] = value
        }

        return response
    }
}

extension MockSource {
    func withHeader(_ header: String, value: String) -> Self {
        var copy = self
        copy.headers[header] = value
        return copy
    }
}
