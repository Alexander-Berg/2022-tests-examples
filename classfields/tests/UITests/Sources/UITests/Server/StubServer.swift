import Foundation
import Network
import SwiftProtobuf
import XCTest
import Snapshots
import Dispatch

enum StubServerError: Error {
    case alreadyStarted
    case badPort
}

class StubServer {
    private static let MTU = 65536
    private let port: UInt16
    private var listener: NWListener?
    private var handlers: [String: RequestInterceptor] = [:]
    private var interceptors: [String: RequestInterceptor] = [:]
    public enum LoginMode {
        case forceLoggedIn
        case forceLoggedOut
        case discardingResponseState
        case preservingResponseState

        func buildLoginHeaders( headers: inout [String: String]) {
            switch self {
            case .forceLoggedIn:
                headers["x-is-login"] = "true"
            case .forceLoggedOut:
                headers["x-is-login"] = "false"
            case .discardingResponseState:
                headers["x-is-login"] = nil
            case .preservingResponseState:
                break
            }
        }
    }
    public var forceLoginMode: LoginMode = .preservingResponseState
    typealias RequestHandler = (Request, Int) -> Response? // подменить n-ый по порядку реквест ответом
    typealias RequestInterceptor = (Request) -> (Response?, Int) // передаем реквест - получаем ответ + порядковый номер подмены

    /// - Parameters:
    ///   - port:
    init(port: UInt16) {
        self.port = port
    }

    private func receiveData(connection: NWConnection, existData: Data? = nil) {
        connection.receive(minimumIncompleteLength: 0, maximumLength: Self.MTU) { [weak self] (data, context, flag, error) in
            DispatchQueue.global(qos: .userInteractive).async {
                guard existData != nil || data != nil else {
                    connection.cancel()
                    return
                }

                var newExistData = existData ?? Data()
                data.flatMap { newExistData.append($0) }

                guard let request = Request(data: newExistData) else {
                    connection.forceCancel()
                    return
                }

                if let contentLength = request.valueForHeader("Content-Length").flatMap({ Int($0) }) {
                    let missedBytesCount = contentLength - (request.messageBody.flatMap { $0.count } ?? 0)
                    if missedBytesCount > 0 {
                        self?.receiveData(connection: connection, existData: newExistData)
                        return
                    }
                }

                self?.processRequest(connection: connection, request: request)
            }
        }
    }

    private func processRequest(connection: NWConnection, request: Request) {
        for interceptor in self.interceptors.values {
            if let response = interceptor(request).0 {
                connection.send(content: response.data, contentContext: .finalMessage, completion: .idempotent)
                return
            }
        }

        // У урла может быть trailing slash
        var uri = request.uri
        if uri.last == "/", !uri.contains("?") {
            uri = String(uri.dropLast())
        }

        var handler = self.handlers["\(request.method) \(uri)".lowercased()]

        if handler == nil {
            let uri = uri.components(separatedBy: "?")[0]
            handler = self.handlers["\(request.method) \(uri) *".lowercased()]
        }

        guard let uriHandler = handler, let response = uriHandler(request).0 else {
            if request.uri != "/device/websocket" {
                print("Handler not found for \(request.method) \(request.uri)")
            }
            let resp = Response.notFoundResponse()
            forceLoginMode.buildLoginHeaders(headers: &resp.headers)
            connection.send(content: resp.data, contentContext: .finalMessage, completion: .idempotent)
            return
        }
        print("Handler found for \(request.method) \(request.uri)")
        forceLoginMode.buildLoginHeaders(headers: &response.headers)
        response.headers["Content-Encoding"] = nil // иначе там может быть gzip и мы получим ошибку энкодера
        connection.send(content: response.data, contentContext: .finalMessage, completion: .idempotent)
    }

    func start() throws {
        guard listener == nil else {
            throw StubServerError.alreadyStarted
        }

        guard let port = NWEndpoint.Port(rawValue: port) else {
            throw StubServerError.badPort
        }

        let options = NWProtocolTCP.Options()
        let listener = try NWListener(using: NWParameters(tls: nil, tcp: options), on: port)
        listener.newConnectionHandler = { connection in
            connection.stateUpdateHandler = { [weak self] state in
                switch state {
                case .ready:
                    self?.receiveData(connection: connection)
                default:
                    break
                }
            }

            connection.start(queue: DispatchQueue.global(qos: .userInteractive))
        }

        listener.start(queue: DispatchQueue.global(qos: .userInteractive))

        self.listener = listener
        Step("Started server at \(port.rawValue)")
    }

    func stop() {
        handlers.removeAll()
        listener?.cancel()
        listener = nil
    }

    ////// - Parameters:
    ///   - methodWithURI: Должен иметь вид "Method URL" GET /geo/1 или "Method URL *" GET /geo/suggest *
    ///   - handler: возврат ответа
    func addHandler(_ methodWithURI: String, _ handler: @escaping RequestHandler) {
        var index = 0
        handlers[methodWithURI.lowercased()] = { request in
            let response = handler(request, index)
            index += 1
            return (response, index)
        }
    }

    func removedHandlers(_ methodWithURI: String) {
        handlers[methodWithURI] = nil
    }

    @discardableResult
    func interceptRequest(handler: @escaping RequestInterceptor) -> String {
        let id = "\(interceptors.count + 1)"
        interceptors[id] = handler
        return id
    }

    @discardableResult
    func interceptRequest(_ methodWithURI: String, handler: @escaping (Request) -> Void) -> String {
        interceptRequest { [methodWithURI = methodWithURI.lowercased()] request in
            let requestMethodWithURI = "\(request.method) \(request.uri)".lowercased()

            if methodWithURI == requestMethodWithURI {
                handler(request)
                return (nil, 0)
            }

            if methodWithURI.hasPrefix(" *"), methodWithURI.dropLast(2) == requestMethodWithURI {
                handler(request)
                return (nil, 0)
            }

            return (nil, 0)
        }
    }

    func removeIntercepter(_ id: String) {
        interceptors[id] = nil
    }
}

extension StubServer {
    /// - Parameters:
    ///   - methodWithURI: Должен иметь вид "Method URL" GET /geo/1 или "Method URL *" GET /geo/suggest *
    ///   - mutate: принимает запрос, порядковый номер запроса, исходный ответ, возвращает измененный ответ
    func mutateHandler(_ methodWithURI: String, _ mutation: @escaping (Request, Response?, Int) -> Response?) {
        guard let handler = handlers[methodWithURI.lowercased()] else {
            return
        }
        handlers[methodWithURI.lowercased()] = { request in
            let (response, index) = handler(request)
            let mutatedResponse = mutation(request, response, index)
            return (mutatedResponse, index)
        }
    }

    func mutateHandler<T: Message>(_ methodWithURI: String, _ mutation: @escaping (Request, T?, Int) -> T?) {
        mutateHandler(methodWithURI, { request, response, index in
            guard let unwrapedResponse = response else {
                return response
            }
            var message: T? = try! T(jsonUTF8Data: unwrapedResponse.data)
            message = mutation(request, message, index)
            return Response(status: unwrapedResponse.status, headers: unwrapedResponse.headers, body: try! message?.jsonUTF8Data())
        })
    }
}

extension StubServer {
    func addHandler<RequestMessage: Message>(
        _ methodWithURI: String,
        @ResponseBuilder _ handler: @escaping (RequestMessage, Request, Int) -> Response
    ) {
        addHandler(methodWithURI) { request, index -> Response? in
            let message: RequestMessage
            do {
                message = try RequestMessage(jsonUTF8Data: request.messageBody ?? Data())
            } catch {
                XCTFail("Unable to decode request message \(error.localizedDescription)")
                return nil
            }

            return handler(message, request, index)
        }
    }

    func addHandler<RequestMessage: Message>(
        _ methodWithURI: String,
        @ResponseBuilder _ handler: @escaping (RequestMessage) -> Response
    ) {
        addHandler(methodWithURI, { (requestMessage: RequestMessage, _, _) in handler(requestMessage) })
    }

    func addHandler(
        _ methodWithURI: String,
        @ResponseBuilder _ handler: @escaping () -> Response
    ) {
        addHandler(methodWithURI, { (_, _) in handler() })
    }
}

extension StubServer {
    func addMessageHandler<RequestMessage: Message>(
        _ methodWithURI: String,
        userAuthorized: Bool = false,
        _ handler: @escaping (RequestMessage, Request, Int) -> Message
    ) {
        addHandler(methodWithURI) { request, index -> Response? in
            let message: RequestMessage
            do {
                message = try RequestMessage(jsonUTF8Data: request.messageBody ?? Data())
            } catch {
                XCTFail("Unable to decode request message \(error.localizedDescription)")
                return nil
            }

            let responseMessage = handler(message, request, index)
            return .okResponse(message: responseMessage, userAuthorized: userAuthorized)
        }
    }

    func addMessageHandler<RequestMessage: Message>(
        _ methodWithURI: String,
        userAuthorized: Bool = false,
        _ handler: @escaping (RequestMessage) -> Message
    ) {
        addMessageHandler(methodWithURI, userAuthorized: userAuthorized, { (requestMessage, _, _) in handler(requestMessage) })
    }

    func addMessageHandler(
        _ methodWithURI: String,
        userAuthorized: Bool = false,
        _ handler: @escaping (Request, Int) -> Message
    ) {
        addHandler(methodWithURI) { request, index -> Response? in
            let responseMessage = handler(request, index)
            return .okResponse(message: responseMessage, userAuthorized: userAuthorized)
        }
    }

    func addMessageHandler(
        _ methodWithURI: String,
        userAuthorized: Bool = false,
        _ handler: @escaping () -> Message
    ) {
        addMessageHandler(methodWithURI, userAuthorized: userAuthorized, { (_, _) in handler() })
    }
}

extension StubServer {
    func interceptRequest<RequestMessage: Message>(_ methodWithURI: String, _ handler: @escaping (RequestMessage) -> Void) {
        interceptRequest(methodWithURI) { (request: Request) in
            let message: RequestMessage
            do {
                message = try RequestMessage(jsonUTF8Data: request.messageBody ?? Data())
            } catch {
                XCTFail("Unable to decode request message \(error.localizedDescription)")
                return
            }

            handler(message)
        }
    }
}

extension StubServer {
    var api: API {
        API(server: self)
    }
}
