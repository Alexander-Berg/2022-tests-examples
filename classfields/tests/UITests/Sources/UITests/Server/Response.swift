import AutoRuProtoModels
import SwiftProtobuf
import Foundation

final class Response {
    var headers: [String: String]
    var body: Data?
    var status: String

    init(status: String, headers: [String: String], body: Data?) {
        self.headers = headers
        self.body = body
        self.status = status
    }

    var data: Data {
        let CRLF = "\r\n"
        var header = status + CRLF

        for entity in headers {
            header += "\(entity.key): \(entity.value)\(CRLF)"
        }

        if body != nil {
            header += CRLF
        }

        var data = header.data(using: .ascii)!
        body.flatMap { data.append($0) }

        return data
    }
}

extension Response {
    static let defaultJSONEncodingOptions: JSONEncodingOptions = {
        var options = JSONEncodingOptions()
        options.preserveProtoFieldNames = true
        return options
    }()

    static func responseWithStatus(body: Data? = nil, protoName: String? = nil, userAuthorized: Bool? = nil, status: String = "HTTP/1.1 200 OK") -> Response {
        var headers: [String: String] = [
            "Strict-Transport-Security": "max-age=31536000",
            "Connection": "keep-alive",
            "Set-Cookie": "X-Vertis-DC=myt;Max-Age=3600;Path=/",
            "Transfer-Encoding": "Identity",
            "Server": "nginx",
            "Date": "Thu, 07 Nov 2019 08:42:54 GMT",
            "Content-Type": "application/json",
            "X-Session-Id": "a:g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffb|1573116174599.604800.f8QbOBNLQ34w6k86sTWvlw.NSNfTZZlBLrKgNyPU76OSYpg8Jmyj6bQgJmOhh1IIlU",
            "x-device-uid": "g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffb"]

        if let userAuthorized = userAuthorized {
            headers["x-is-login"] = userAuthorized ? "true" : "false"
        }
        headers["x-proto-name"] = protoName

        return Response(status: status,
                 headers: headers,
                 body: body)
    }

    static func okResponse(
        message: Message,
        options: JSONEncodingOptions = defaultJSONEncodingOptions,
        userAuthorized: Bool = false
    ) -> Response {
        let data = try! message.jsonUTF8Data(options: options)
        return Response.responseWithStatus(body: data, userAuthorized: userAuthorized)
    }

    static func okResponse(fileName: String, subdirectory: String? = nil, userAuthorized: Bool = false) -> Response {
        if let fileURL = Bundle.resources.url(forResource: fileName, withExtension: "json", subdirectory: subdirectory) {
            return okResponse(fileURL: fileURL, userAuthorized: userAuthorized)
        } else {
            fatalError("File \(fileName) doesnt exist in \(String(describing: subdirectory)) in bundle")
        }
    }

    static func okResponse(fileURL: URL, userAuthorized: Bool = false) -> Response {
        do {
            let data = try Data(contentsOf: fileURL, options: .uncached)
            return Response.responseWithStatus(body: data, userAuthorized: userAuthorized)
        } catch(let error) {
            fatalError("Cannot read data from \(fileURL). Error \(error)")
        }
    }

    static func badResponse(fileName: String, userAuthorized: Bool = false) -> Response {
        guard let fileURL = Bundle.resources.url(forResource: fileName, withExtension: "json")
        else { fatalError("File \(fileName) doesn't exist") }

        do {
            let body: Data = try Data(contentsOf: fileURL)
            return Response.responseWithStatus(body: body, protoName: "auto.api.ErrorResponse", userAuthorized: userAuthorized, status: "HTTP/1.1 400 BAD_REQUEST")
        } catch {
            fatalError("Cannot read data from \(fileURL). Error \(error)")
        }
    }

    static func badResponse(code: Auto_Api_ErrorCode, userAuthorized: Bool = false) -> Response {
        var error = Auto_Api_ErrorResponse()
        error.error = code
        let body = try! error.jsonUTF8Data()
        return Response.responseWithStatus(body: body, protoName: "auto.api.ErrorResponse", userAuthorized: false, status: "HTTP/1.1 400 BAD_REQUEST")
    }

    static func responseWith(errorModel: String = "auto.api.ErrorResponse", code: String, fileName: String, userAuthorized: Bool = false) -> Response {
        let filePath = Bundle.resources.url(forResource: fileName, withExtension: "json")
        let body: Data? = filePath.flatMap { try? Data(contentsOf: $0) }
        return Response.responseWithStatus(body: body, protoName: errorModel, userAuthorized: userAuthorized, status: "HTTP/1.1 \(code)")
    }
}

extension Response {
    static func notFoundResponse() -> Response {
        let headers: [String: String] = [
            "x-proto-name": "auto.api.ErrorResponse",
            "Strict-Transport-Security": "max-age=31536000",
            "Connection": "keep-alive",
            "Set-Cookie": "X-Vertis-DC=myt;Max-Age=3600;Path=/",
            "Transfer-Encoding": "Identity",
            "Server": "nginx",
            "Date": "Thu, 07 Nov 2019 08:42:54 GMT",
            "Content-Type": "application/json",
            "X-Session-Id": "a:g5cc1b3c24cd9lgo13k0noqhpd09qqdf.5509d34a817b8f2b2fc2a2052600bffb|1573116174599.604800.f8QbOBNLQ34w6k86sTWvlw.NSNfTZZlBLrKgNyPU76OSYpg8Jmyj6bQgJmOhh1IIlU"]

        let filePath = Bundle.main.url(forResource: "404_error", withExtension: "json")
        let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }

        return Response(status: "HTTP/1.1 404 NOT FOUND",
                        headers: headers,
                        body: body)
    }
}
