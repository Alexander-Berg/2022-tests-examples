import Foundation

public typealias TriggerCompletion = (Result<Build, Error>) -> Void
public typealias StopToken = (Result<Build, Error>) -> Bool
public typealias BuildConfigurationID = String

public protocol TeamcityClient {
    func triggerBuild(
        configID: BuildConfigurationID,
        branch: String,
        parameters: [BuildParameter: BuildParameter.Value],
        comment: String?,
        completion: TriggerCompletion?
    )

    func observeBuild(
        id: Int,
        interval: TimeInterval,
        stopToken: @escaping StopToken
    )
}

public enum TeamcityClientFactory {
    public static func make(host: String, port: Int, token: String) -> TeamcityClient {
        TeamcityRESTClient(host: host, port: port, token: token)
    }

    public static func makeStub() -> TeamcityClient {
        struct TeamcityDummyClient: TeamcityClient {
            func triggerBuild(
                configID: BuildConfigurationID,
                branch: String,
                parameters: [BuildParameter : BuildParameter.Value],
                comment: String?,
                completion: TriggerCompletion?
            ) {
                completion?(.failure(DummyError.notImplemented))
            }

            func observeBuild(id: Int, interval: TimeInterval, stopToken: @escaping StopToken) {
                _ = stopToken(.failure(DummyError.notImplemented))
            }

            enum DummyError: Error {
                case notImplemented
            }
        }

        return TeamcityDummyClient()
    }
}
