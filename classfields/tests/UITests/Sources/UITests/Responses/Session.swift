import Foundation
import AutoRuProtoModels

extension Responses {
    enum Session {
    }
}

extension Responses.Session {
    static func success(for state: BackendState) -> Vertis_Passport_SessionResult {
        return .with { msg in
            msg.session = state.makeSession()
        }
    }
}
