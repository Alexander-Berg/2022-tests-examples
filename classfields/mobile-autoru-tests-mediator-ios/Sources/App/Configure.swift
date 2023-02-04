import Vapor
import Mediation
import Foundation
import Teamcity

public struct Config {
    let teamcityHost: String
    let teamcityPort: Int
    let teamcityToken: String
    let useTeamcityStub: Bool
    let apiPort: Int

    public init(
        teamcityHost: String,
        teamcityPort: Int,
        teamcityToken: String,
        useTeamcityStub: Bool,
        apiPort: Int
    ) {
        self.teamcityHost = teamcityHost
        self.teamcityPort = teamcityPort
        self.teamcityToken = teamcityToken
        self.useTeamcityStub = useTeamcityStub
        self.apiPort = apiPort
    }
}

public func configure(_ app: Application, config: Config) throws {
    app.http.server.configuration.hostname = "::"
    app.http.server.configuration.port = config.apiPort
    app.routes.defaultMaxBodySize = "10mb"

    // Decoder
    let decoder = JSONDecoder()
    decoder.keyDecodingStrategy = .convertFromSnakeCase
    decoder.dateDecodingStrategy = .secondsSince1970
    ContentConfiguration.global.use(decoder: decoder, for: .json)

    // Encoder
    let encoder = JSONEncoder()
    encoder.dateEncodingStrategy = .secondsSince1970
    encoder.keyEncodingStrategy = .convertToSnakeCase
    ContentConfiguration.global.use(encoder: encoder, for: .json)

    // State
    let logger = MediationLogger(logger: app.logger)

    let teamcity = TeamcityClientFactory.make(
        host: config.teamcityHost,
        port: config.teamcityPort,
        token: config.teamcityToken
    )

    app.mediation = MediationState(logger: logger, teamcity: teamcity)

    // Routes
    try routes(app)

    app.middleware = {
        var middlewares = Middlewares()
        middlewares.use(CatchNotFound())
        return middlewares
    }()
}

public func configureForMetrics(_ app: Application, metricsPort: Int) throws {
    app.http.server.configuration.hostname = "::"
    app.http.server.configuration.port = metricsPort

    app.get("ping") { _ in "" }
    app.get("metrics") { _ in "" }

    app.middleware = {
        var middlewares = Middlewares()
        middlewares.use(CatchNotFound())
        return middlewares
    }()
}

struct MediationStateKey: StorageKey {
    typealias Value = MediationState
}

extension Application {
    var mediation: MediationState? {
        get {
            self.storage[MediationStateKey.self]
        }
        set {
            self.storage[MediationStateKey.self] = newValue
        }
    }
}

struct CatchNotFound: Middleware {
    func respond(to request: Request, chainingTo next: Responder) -> EventLoopFuture<Response> {
        if request.route == nil {
            return ErrorResponse(status: .methodNotFound, message: nil).encodeResponse(for: request)
        }

        return next.respond(to: request)
    }
}
