import Foundation

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case delete = "DELETE"
    case put = "PUT"
}

// https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
enum HTTPResponseStatus: String {
    case _200 = "200 OK"
    case _400 = "400 Bad Request"
    case _401 = "401 Unauthorized"
    case _403 = "403 Forbidden"
    case _404 = "404 Not Found"
    case _409 = "409 Conflict"
    case _500 = "500 Internal Server Error"
    case _502 = "502 Bad Gateway"
    case _503 = "503 Service Unavailable"
    case _504 = "504 Gateway Timeout"
}
