import Foundation
import AutoRuProtoModels

extension Responses {
    enum User {
        enum Confirm { }
        enum Favorites { }
    }
}

extension Responses.User {
    static func get(for state: BackendState) -> Auto_Api_UserResponse {
        .with { msg in
            if state.user.authorized {
                msg.user = state.makeUser()
            }

            msg.status = .success
        }
    }
}

extension Responses.User.Confirm {
    static func success(for state: BackendState) -> Vertis_Passport_ConfirmIdentityResult {
        return .with { msg in
            msg.session = state.makeSession()
            msg.user = state.makeUser()
            msg.userTicket = "eyJ0eX"
        }
    }

    static func failure(for state: BackendState) -> Auto_Api_ErrorResponse {
        return .with { msg in
            msg.error = .confirmationCodeNotFound
            msg.status = .error
            msg.detailedError = "CONFIRMATION_CODE_NOT_FOUND"
        }
    }
}

extension Responses.User.Favorites {
    static func success(for state: BackendState) -> Auto_Api_FavoriteListingResponse {
        return .with { msg in
            msg.offers = state.user.favorites.offers.map { offer in
                makeOffer(offer, state: state)
            }
            msg.status = .success
        }
    }
}
