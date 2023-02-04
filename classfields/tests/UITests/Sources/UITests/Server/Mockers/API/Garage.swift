import Foundation

extension Mocker {
    @discardableResult
    func mock_publicGarageCard(_ id: String) -> Self {
        server.addHandler("GET /garage/card/\(id)") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_public_card_1955418404")
        }
        return self

    }

    @discardableResult
    func mock_garageReviews() -> Self {
        server.addHandler("GET /reviews/auto/CARS/rating *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_ratings", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/CARS/counter *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_counter", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/features/CARS *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_features", userAuthorized: true)
        }

        server.addHandler("GET /reviews/auto/listing *") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_card_reviews_listing", userAuthorized: true)
        }

        return self
    }

    @discardableResult
    func mock_garageListing() -> Self {
        server.addHandler("POST /garage/user/cards") { _, _ -> Response? in
            Response.okResponse(fileName: "garage_cards")
        }

        return self
    }

    @discardableResult
    func mock_garageListingWithOneCar() -> Self {
        server.addHandler("POST /garage/user/cards") { _, _ in
            Response.okResponse(fileName: "garage_cards_one_car")
        }

        return self
    }

    @discardableResult
    func mock_garageListingEmpty() -> Self {
        server.addHandler("POST /garage/user/cards") { _, _ in
            Response.okResponse(fileName: "garage_cards_empty")
        }

        return self
    }

    @discardableResult
    func mock_garageCard(_ id: String) -> Self {
        server.api.garage.user.card.offer.offerId(id).post
            .ok(mock: .file("garage_card_1955418404"))
        return self
    }

    @discardableResult
    func mock_publicGarageDreamCard(_ id: String) -> Self {
        server.api.garage.card.cardId(id)
            .get
            .ok(mock: .file("garage_dreamCar_shared_card"))
        return self
    }
}
