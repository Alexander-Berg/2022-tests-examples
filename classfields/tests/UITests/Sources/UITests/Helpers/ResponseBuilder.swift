import SwiftProtobuf
import Foundation

@resultBuilder
struct ResponseBuilder {
    static func buildEither<TrueResponse: ResponseConvertible, FalseResponse: ResponseConvertible>(first: TrueResponse) -> Either<TrueResponse, FalseResponse> {
        .left(first)
    }

    static func buildEither<TrueResponse: ResponseConvertible, FalseResponse: ResponseConvertible>(second: FalseResponse) -> Either<TrueResponse, FalseResponse> {
        .right(second)
    }

    static func buildBlock<Content: ResponseConvertible>(_ response: Content) -> Content {
        response
    }

    static func buildExpression<M: Message>(_ message: M) -> MessageResponse<M> {
        MessageResponse(message: message)
    }

    static func buildExpression<Convertible: ResponseConvertible>(_ convertible: Convertible) -> Convertible {
        convertible
    }

    static func buildFinalResult<Convertible: ResponseConvertible>(_ convertible: Convertible) -> Response {
        convertible.toResponse()
    }
}

extension Response: ResponseConvertible {
    func toResponse() -> Response {
        self
    }
}

struct MessageResponse<MessageType: Message>: ResponseConvertible {
    var code: Int = 200
    var message: MessageType
    var additionalHeaders: [String: String] = [:]

    func toResponse() -> Response {
        Response(
            status: getStatus(),
            headers: getHeaders(),
            body: try! message.jsonUTF8Data(options: Response.defaultJSONEncodingOptions)
        )
    }

    private func getHeaders() -> [String: String] {
        let state = BackendState.global

        let rfcDateFormat = DateFormatter()
        rfcDateFormat.dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z"
        rfcDateFormat.locale = Locale(identifier: "en_US_POSIX")

        var headers = [
            "Strict-Transport-Security": "max-age=31536000",
            "Connection": "keep-alive",
            "Set-Cookie": "X-Vertis-DC=myt;Max-Age=3600;Path=/",
            "Transfer-Encoding": "Identity",
            "Server": "nginx",
            "Date": rfcDateFormat.string(from: state.now),
            "Content-Type": "application/json",
            "X-Session-Id": state.session.id,
            "x-device-uid": state.device.uid,
            "x-is-login": state.user.authorized ? "true" : "false"
        ]

        headers.merge(additionalHeaders, uniquingKeysWith: { _, second in second })

        return headers
    }

    private func getStatus() -> String {
        let name: String

        switch code {
        case 200:
            name = "OK"

        case 400:
            name = "BAD_REQUEST"

        case 404:
            name = "Not Found"

        default:
            name = ""
        }

        return "HTTP/1.1 \(code) \(name)"
    }
}

extension Either: ResponseConvertible where Left: ResponseConvertible, Right: ResponseConvertible {
    func toResponse() -> Response {
        switch self {
        case let .left(value):
            return value.toResponse()

        case let .right(value):
            return value.toResponse()
        }
    }
}

extension ResponseConvertible {
    func modifyState(_ modify: @escaping (inout BackendState) -> Void) -> ResponseConvertibleWithEffect<Self> {
        ResponseConvertibleWithEffect(base: self) {
            modify(&BackendState.global)
        }
    }
}

extension Message {
    func modifyState(_ modify: @escaping (inout BackendState) -> Void) -> ResponseConvertibleWithEffect<MessageResponse<Self>> {
        ResponseConvertibleWithEffect(base: MessageResponse(message: self)) {
            modify(&BackendState.global)
        }
    }
}

struct ResponseConvertibleWithEffect<Convertible: ResponseConvertible>: ResponseConvertible {
    let base: Convertible
    let effect: () -> Void

    func toResponse() -> Response {
        effect()
        return base.toResponse()
    }
}
