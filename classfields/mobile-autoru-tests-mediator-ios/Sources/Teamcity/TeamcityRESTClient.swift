import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

final class TeamcityRESTClient: TeamcityClient {
    private let baseURL: URL
    private let headers: [String: String]

    private lazy var decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .formatted(DateFormatter.teamcityFormat)
        return decoder
    }()

    init(host: String, port: Int, token: String) {
        self.baseURL = URL(string: "http://\(host):\(port)/app/rest")!
        self.headers = [
            "Authorization": "Bearer \(token)",
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Origin": "http://\(host)"
        ]
    }

    func triggerBuild(
        configID: BuildConfigurationID,
        branch: String,
        parameters: [BuildParameter: BuildParameter.Value],
        comment: String?,
        completion: TriggerCompletion?
    ) {
        let request = RunBuild(
            branchName: branch,
            buildType: .init(id: configID),
            comment: comment.flatMap { .init(text: $0) },
            properties: .init(
                property: parameters.map { .init(name: $0.key.rawValue, value: $0.value.value) }
            )
        )

        guard let jsonData = try? JSONEncoder().encode(request) else {
            completion?(.failure(Error.invalidRequestData))
            return
        }

        makeRequest(
            path: ["buildQueue"],
            method: .post,
            headers: headers,
            body: jsonData
        ) { (result: Result<Build, Swift.Error>) in
            completion?(result)
        }
    }

    func observeBuild(id: Int, interval: TimeInterval, stopToken: @escaping StopToken) {
        let timer = DispatchSource.makeTimerSource()

        timer.schedule(deadline: .now(), repeating: interval)
        defer { timer.activate() }

        timer.setEventHandler { [weak self] in
            self?.makeRequest(
                path: ["builds", "id:\(id)"],
                method: .get,
                headers: self?.headers ?? [:]
            ) { (result: Result<Build, Swift.Error>) in
                if stopToken(result) {
                    timer.cancel()
                }
            }
        }

        timer.activate()
    }

    private func makeRequest<Model: Decodable>(
        path: [String],
        method: HTTPMethod,
        headers: [String: String],
        body: Data = Data(),
        completion: ((Result<Model, Swift.Error>) -> Void)?
    ) {
        var request = URLRequest(url: baseURL.appendingPathComponent(path.joined(separator: "/")))
        request.httpMethod = method.rawValue
        request.allHTTPHeaderFields = headers
        request.httpBody = body

        URLSession.shared.dataTask(with: request) { [self] data, response, error in
            if error != nil {
                completion?(.failure(Error.requestFailed))
                return
            }

            guard let data = data else {
                completion?(.failure(Error.requestFailed))
                return
            }

            do {
                let model = try self.decoder.decode(Model.self, from: data)
                completion?(.success(model))
            } catch {
                completion?(.failure(Error.invalidData(error)))
            }
        }.resume()
    }

    enum HTTPMethod: String {
        case get
        case post
        case put
        case delete
    }

    enum Error: Swift.Error {
        case requestFailed
        case invalidRequestData
        case invalidData(Swift.Error)
    }
}

extension DateFormatter {
    static let teamcityFormat: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd'T'HHMMSSZ"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter
    }()
}
