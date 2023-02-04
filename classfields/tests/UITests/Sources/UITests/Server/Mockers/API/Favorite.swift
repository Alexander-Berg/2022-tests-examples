//
//  Favorite.swift
//  UITests
//
//  Created by Pavel Savchenkov on 08.06.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {
    @discardableResult
    func mock_favoriteSoldOffers() -> Self {
        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "favs_offers_no_updates_4_sold")
        }
        return self
    }

    @discardableResult
    func mock_favorite3Sold1ActiveOffers() -> Self {
        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "favs_offers_no_updates_3_sold_1_active")
        }
        return self
    }

    @discardableResult
    func mock_addOfferToFavorite(_ offerId: String? = nil) -> Self {
        var postfix: String
        if let offerId = offerId {
            postfix = "/\(offerId)"
        } else {
            postfix = " *"
        }
        server.addHandler("POST /user/favorites/cars\(postfix)") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_deleteOfferFromFavorite(_ offerId: String? = nil) -> Self {
        var postfix: String
        if let offerId = offerId {
            postfix = "/\(offerId)"
        } else {
            postfix = " *"
        }
        server.addHandler("DELETE /user/favorites/cars\(postfix)") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_deleteInactiveOffersFromFavorite() -> Self {
        server.addHandler("DELETE /user/favorites/all/not_active") { (_, _) -> Response? in
            Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_addToFavouriteSubscriptions() -> Self {
        server.addHandler("POST /user/favorites/cars/subscriptions") { request, _ -> Response? in
            return Response.okResponse(fileName: "success")
        }
        return self
    }

    @discardableResult
    func mock_favouriteSubscriptions() -> Self {
        server.addHandler("GET /user/favorites/all/subscriptions") { request, _ in
            Response.okResponse(fileName: "enable_push_saved_searches")
        }
        return self
    }

    @discardableResult
    func mock_favouriteSubscriptionById(_ id: String) -> Self {
        server.addHandler("GET /user/favorites/all/subscriptions/\(id)") { request, _ in
            Response.okResponse(fileName: "user_favorites_all_subscriptions_by_id")
        }
        return self
    }
}
