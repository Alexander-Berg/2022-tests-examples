import Foundation

public struct Build: Decodable {
    public let id: Int
    public let number: String?
    public let state: State
    public let buildTypeId: BuildConfigurationID

    public init(
        id: Int,
        number: String,
        state: State,
        buildTypeId: BuildConfigurationID
    ) {
        self.id = id
        self.number = number
        self.state = state
        self.buildTypeId = buildTypeId
    }
}

extension Build {
    public enum State: String, Decodable {
        case finished
        case running
        case queued
        case unknown
    }

    public enum Status: String, Decodable {
        case status = "STATUS"
        case failure = "FAILURE"
        case unknown = "UNKNOWN"
    }
}
