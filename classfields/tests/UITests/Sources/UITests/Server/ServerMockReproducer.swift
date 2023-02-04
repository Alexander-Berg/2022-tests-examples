//
//  ServerMockPlayer.swift
//  UITests
//
//  Created by Roman Bevza on 7/22/20.
//

import Foundation

struct DynamicKey: CodingKey {
    var stringValue: String

    init?(stringValue: String) {
        self.stringValue = stringValue
    }

    var intValue: Int? { return nil }
    init?(intValue: Int) { return nil }
}

extension UnkeyedDecodingContainer {
    mutating func decodeDynamicValues() throws -> [Any] {
        var array = [Any]()
        while !isAtEnd {
            if let v = try? decode(String.self) {
                array.append(v)
            } else if let v = try? decode(Bool.self) {
                array.append(v)
            } else if let v = try? decode(Int.self) {
                array.append(v)
            } else if let v = try? decode(Double.self) {
                array.append(v)
            } else if let v = try? decode(Float.self) {
                array.append(v)
            } else if let container = try? nestedContainer(keyedBy: DynamicKey.self),
                let v = try? container.decodeDynamicKeyValues() {
                array.append(v)
            } else if var container = try? nestedUnkeyedContainer(),
                let v = try? container.decodeDynamicValues() {
                array.append(v)
            } else {
                throw DecodingError.dataCorruptedError(in: self, debugDescription: "Type not supported")
            }
        }
        return array
    }
}
extension KeyedDecodingContainer where Key == DynamicKey {
    func decodeDynamicKeyValues() throws -> [String: Any] {
        var dict = [String: Any]()
        for key in allKeys {
            if let v = try? decode(String.self, forKey: key) {
                dict[key.stringValue] = v
            } else if let v = try? decode(Bool.self, forKey: key) {
                dict[key.stringValue] = v
            } else if let v = try? decode(Int.self, forKey: key) {
                dict[key.stringValue] = v
            } else if let v = try? decode(Double.self, forKey: key) {
                dict[key.stringValue] = v
            } else if let v = try? decode(Float.self, forKey: key) {
                dict[key.stringValue] = v
            } else if let container = try? nestedContainer(keyedBy: DynamicKey.self, forKey: key),
                let v = try? container.decodeDynamicKeyValues() {
                dict[key.stringValue] = v
            } else if var container = try? nestedUnkeyedContainer(forKey: key),
                let v = try? container.decodeDynamicValues() {
                dict[key.stringValue] = v
            } else {
                throw DecodingError.dataCorruptedError(forKey: key, in: self, debugDescription: "Key \(key.stringValue) type not supported")
            }
        }
        return dict
    }
}

protocol ServerMockReproducer {
    func setup(server: StubServer, mockFolderName: String)
}

class AdvancedMockReproducer {
    struct ResponseMock: Codable {
        var method: String
        var request: String
        var query: String?
        var requestTimestamp: TimeInterval
        var requestHeaders: [String: String]?

        var responseStatusCode: Int?
        var responseHeaders: [String: String]?
        var responseBody: Data?
        var responseTimestamp: TimeInterval
    }

    let sourceFolderName: String
    init(sourceFolderName: String = "ReproducerServerStubs.bundle") {
        self.sourceFolderName = sourceFolderName
    }

    func setup(server: StubServer, mockFolderName: String, preciseQueryMatching: Bool = false) {
        server.forceLoginMode = .preservingResponseState
        let handlers = self.handlers(mockFolderName: mockFolderName, preciseQueryMatching: preciseQueryMatching)
        handlers.forEach({ (key, value) in
            server.addHandler(key, value)
        })
    }

    func setupHandler(server: StubServer, fromFile mockFileName: String, preciseQueryMatching: Bool = false) {
        server.forceLoginMode = .preservingResponseState
        guard let mockFile = Bundle(url: Bundle.resources.bundleURL.appendingPathComponent(sourceFolderName))?.url(forResource: mockFileName, withExtension: "json") else {
            fatalError("Can't init mockserver from \(mockFileName)")
        }
        let (endpoint, _, response) = self.handler(mockFile: mockFile, preciseQueryMatching: preciseQueryMatching)
        server.addHandler(endpoint, { _, _ in
            return response
        })
    }

    private func handler(mockFile: URL, preciseQueryMatching: Bool) -> (endpoint: String, timestamp: TimeInterval, response: Response) {
        do {
            let mockData: Data = try Data(contentsOf: mockFile)

            let decoder = JSONDecoder()
            decoder.dataDecodingStrategy = .custom({ (decoder) -> Data in
                let container = try decoder.container(keyedBy: DynamicKey.self)
                let json = try container.decodeDynamicKeyValues()
                return try JSONSerialization.data(withJSONObject: json, options: [])
            })
            let responseMock = try decoder.decode(ResponseMock.self, from: mockData)

            let statusString: String
            if let statusCode = responseMock.responseStatusCode {
                statusString = "HTTP/1.1 \(statusCode)"
            } else {
                statusString = "HTTP/1.1 200 OK"
            }

            let response = Response(status: statusString,
                                    headers: responseMock.responseHeaders ?? [:],
                                    body: responseMock.responseBody)

            let requestString: String
            if preciseQueryMatching {
                var queryString: String = ""
                if let query = responseMock.query,
                    !query.isEmpty {
                    queryString = "?\(query)"
                }
                requestString = "\(responseMock.method.uppercased()) \(responseMock.request)\(queryString)"
            } else {
                requestString = "\(responseMock.method.uppercased()) \(responseMock.request) *"
            }
            return (endpoint: requestString, timestamp: responseMock.requestTimestamp, response: response)
        } catch {
            fatalError("Can't read mockfile \(mockFile)")
        }
    }

    private func handlers(mockFolderName: String, preciseQueryMatching: Bool) -> [String: StubServer.RequestHandler] {
        guard let mockFiles = Bundle(url: Bundle.resources.bundleURL.appendingPathComponent(sourceFolderName))?.urls(forResourcesWithExtension: "json", subdirectory: mockFolderName) else {
            fatalError("Can't init mockserver from \(mockFolderName)")
        }

        var mocks: [String: [(TimeInterval, Response)]] = [:]
        for mockFile in mockFiles {

            let (endpoint, timestamp, response) = handler(mockFile: mockFile, preciseQueryMatching: preciseQueryMatching)
            var currentHandlers = mocks[endpoint] ?? []
            currentHandlers.append((timestamp, response))
            mocks[endpoint] = currentHandlers
        }

        var handlers: [String: StubServer.RequestHandler] = [:]
        for (method, mock) in mocks {
            handlers[method] = { (request, index) -> Response? in
                let sortedMocks = mock.sorted { (lhs, rhs) -> Bool in
                    return lhs.0 < rhs.0
                }
                .map { $0.1 }

                return sortedMocks[min(sortedMocks.count - 1, index)]
            }
        }

        return handlers
    }
}

class BasicMockReproducer {
    let sourceFolderName: String
    init(sourceFolderName: String = "ReproducerServerStubs.bundle") {
        self.sourceFolderName = sourceFolderName
    }

    func setup(server: StubServer, mockFolderName: String, userAuthorized: Bool) {
        let handlers = self.handlers(mockFolderName: mockFolderName, userAuthorized: userAuthorized)
        handlers.forEach({ (key, value) in
            server.addHandler(key, value)
        })
    }

    func handlers(mockFolderName: String, userAuthorized: Bool) -> [String: StubServer.RequestHandler] {
        guard let mockFiles = Bundle(url: Bundle.resources.bundleURL.appendingPathComponent(sourceFolderName))?.urls(forResourcesWithExtension: "json", subdirectory: mockFolderName) else {
            fatalError("Can't init mockserver from \(mockFolderName)")
        }

        var mocks: [String: [URL]] = [:]
        for mockFile in mockFiles {
            // parsing files with filenames METHOD path_path index.json, like "GET reference_catalog_CARS_suggest 2.json"
            guard let name = mockFile.pathComponents.last else {
                continue
            }
            let components = name.replacingOccurrences(of: ".json", with: "").split(separator: " ")
            if components.count >= 2 {
                var method = String(components[0] + " /" + components[1].lowercased())
                method = method.replacingOccurrences(of: "_", with: "/")
                var existingMocks = mocks[method] ?? []
                existingMocks.append(mockFile)
                mocks[method] = existingMocks
            }
        }

        var handlers: [String: StubServer.RequestHandler] = [:]
        for (method, mockFiles) in mocks {
            handlers[method + " *"] = { (request, index) -> Response? in
                let sortedMockFiles = mockFiles.sorted { (lhs, rhs) -> Bool in
                    guard let lhsName = lhs.pathComponents.last,
                        let rhsName = rhs.pathComponents.last else {
                            return false
                    }
                    return lhsName.compare(rhsName) == .orderedDescending
                }

                return Response.okResponse(fileURL: sortedMockFiles[min(sortedMockFiles.count - 1, index)], userAuthorized: userAuthorized)
            }
        }

        return handlers
    }
}
