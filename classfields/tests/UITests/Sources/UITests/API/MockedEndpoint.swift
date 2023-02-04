import Foundation
import SwiftProtobuf
import XCTest

struct MockedEndpoint<
    QueryParameter: EndpointQueryParameter,
    RequestMessage: SwiftProtobuf.Message,
    ResponseMessage: SwiftProtobuf.Message
>: CustomStringConvertible {
    let responseCode: HTTPResponseStatus
    let method: HTTPMethod
    let path: String
    let parameters: EndpointQueryParametersMatching<QueryParameter>?
    let mock: MockSource<RequestMessage, ResponseMessage>

    var description: String {
        "[\(responseCode.rawValue)] [\(method.rawValue)] \(parameters?.getStubServerFullURL(for: path) ?? path)"
    }

    private var stubServerHandlerMethod: String {
        "\(method.rawValue.uppercased()) \((parameters?.getStubServerFullURL(for: path) ?? path).lowercased())"
    }

    func use(with stubServer: StubServer) {
        stubServer.addHandler(stubServerHandlerMethod) { req, _ in
            let info = MockSource<RequestMessage, ResponseMessage>.RequestEndpointInfo(
                method: method,
                path: path,
                request: req
            )

            do {
                return try mock.makeResponse(withInfo: info, statusCode: responseCode.rawValue)
            } catch {
                XCTFail("Invalid mock model \(error)")
                return nil
            }
        }
    }
}
