import Foundation
import AutoRuProtoModels

extension Responses {
    enum Auth {
        enum LoginOrRegister { }
    }
}

extension Responses.Auth.LoginOrRegister {
    static func success(for state: BackendState) -> Auto_Api_LoginOrRegisterResponse {
        return .with { msg in
            msg.codeLength = 4
        }
    }

}
