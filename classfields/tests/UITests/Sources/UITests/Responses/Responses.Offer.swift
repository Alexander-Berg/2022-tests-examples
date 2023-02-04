import Foundation
import AutoRuProtoModels

extension Responses {
    enum Offer {
        enum Phones { }
    }
}

extension Responses.Offer.Phones {
    static func success(for state: BackendState, offer: BackendState.Offer) -> Auto_Api_PhoneResponse {
        var response = makePhones(offer)
        for index in response.phones.indices {
            response.phones[index].app2AppCallAvailable = state.user.authorized
        }
        return response
    }
}
