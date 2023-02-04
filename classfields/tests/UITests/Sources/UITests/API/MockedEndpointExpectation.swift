import XCTest
import SwiftProtobuf

enum ExpectationCheckerVerdict {
    case ok
    case skip
    case fail(reason: String?)

    static func okIf(_ condition: Bool) -> ExpectationCheckerVerdict {
        condition ? .ok : .skip
    }
}

struct MockedEndpointExpectation<
    QueryParameter: EndpointQueryParameter,
    RequestMessage: SwiftProtobuf.Message
> {
    let method: HTTPMethod
    let path: String
    let parameters: EndpointQueryParametersMatching<QueryParameter>?
    let checker: ((RequestMessage, Int) -> ExpectationCheckerVerdict)?
    let isInverted: Bool

    var description: String {
        "[\(method.rawValue)] \(parameters?.getStubServerFullURL(for: path) ?? path)"
    }

    func make(with stubServer: StubServer) -> XCTestExpectation {
        let expectation = XCTestExpectation(description: "Ожидаем запрос \(description)")
        var index = 0

        stubServer.interceptRequest { request -> (Response?, Int) in
            switch checkRequest(request, index: index) {
            case .ok:
                expectation.fulfill()
            case .fail(let reason):
                XCTFail(reason ?? "Ожидание зафейлилось")
            case .skip:
                break
            }

            index += 1
            return (nil, index)
        }

        expectation.isInverted = isInverted
        return expectation
    }

    private func checkRequest(_ request: Request, index: Int) -> ExpectationCheckerVerdict {
        guard request.method.lowercased() == method.rawValue.lowercased() else { return .skip }

        let path = path.lowercased()
        let uri = request.uri.lowercased()

        let url = "http://auto.ru"
            + uri.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed.union(.urlQueryAllowed))!

        guard let fullURL = URL(string: url) else {
            return .fail(reason: "Невозможно создать url запроса")
        }

        let checkPathEquality: (String, String) -> Bool = { uri, path in
            uri == (path + "/") || uri == path
        }

        guard checkPathEquality(fullURL.path, path) else {
            return .skip
        }

        switch parameters {
        case .wildcard, .none:
            break
        case .parameters(let params):
            let expectationQueryParams = Set(
                params
                    .flatMap { $0.queryRepresentation
                        .split(separator: "&")
                        .map({ String($0).lowercased() })
                    }
            )
            let queryComponents: Set<String> = Set(
                (fullURL.query ?? "")
                    .split(separator: "&")
                    .map { String($0) }
            )

            if queryComponents != expectationQueryParams {
                return .skip
            }
        }

        var options = JSONDecodingOptions()
        options.ignoreUnknownFields = true

        if let checker = checker {
            guard let data = request.messageBody,
                  let model = try? RequestMessage(jsonUTF8Data: data, options: options) else {
                return .fail(reason: "Невозможно создать протомодель запроса")
            }

            return checker(model, index)
        }

        return .ok
    }
}
