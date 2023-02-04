import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {

    @discardableResult
    func mock_addOfferToFavorites(offerId: String) -> Self {
        server.addHandler("POST /user/favorites/cars/\(offerId)") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_addUserNote(offerId: String) -> Self {
        server.addHandler("PUT /user/notes/cars/\(offerId)") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_addUserNoteToOffer(testTextNote: String) -> Self {
        server.addHandler("GET /offer/CARS/1098252972-99d8c274") { (_, _) -> Response? in
            var model: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098252972-99d8c274_ok")
            model.offer.note = testTextNote
            model.offer.isFavorite = true
            return Response.okResponse(message: model, userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_userNotes() -> Self {
        server.addHandler("GET /user/notes/all") { (_, _) -> Response? in
            Response.okResponse(fileName: "user_notes", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_userFavoriteOffers() -> Self {
        server.addHandler("GET /user/favorites/all?with_data=false") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_favorite_offers")
        }
        return self
    }
}
