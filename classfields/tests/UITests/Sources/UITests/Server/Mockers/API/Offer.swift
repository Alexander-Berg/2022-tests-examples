//
//  Offer.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf
import Snapshots

extension Mocker {
    @discardableResult
    func mock_offerCars(
        id: String? = nil,
        isSalon: Bool = false,
        safeDealInfo: Auto_Api_SafeDealInfo? = nil,
        price: Int? = nil,
        tags: [String] = [],
        customResponseHander: ((inout Auto_Api_OfferResponse) -> Void)? = nil
    ) -> Self {

        var response = Auto_Api_OfferResponse()
        if isSalon {
            response.offer = .init(mockFile: "dealerOffer")
        } else {
            let searchResp: Auto_Api_OfferListingResponse = .init(mockFile: "searchCar")
            response.offer = searchResp.offers[0]
        }

        var postfix: String
        if let id = id {
            response.offer.id = id
            postfix = "/\(id)"
        } else {
            postfix = " *"
        }

        if let safeDealInfo = safeDealInfo {
            response.offer.safeDealInfo = safeDealInfo
        }

        if let price = price {
            response.offer.priceInfo.price = Float(price)
        }

        response.offer.tags = (response.offer.tags + tags).unique()
        customResponseHander?(&response)

        server.addHandler("GET /offer/CARS\(postfix)") { (requestEnt, _) -> Response? in
            return Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }

        return self
    }

    @discardableResult
    func mock_userOffersCarsOne() -> Self {
        server.api.user.offers.category(.cars).get(parameters: .wildcard)
            .ok(mock: .file("user_offers_cars_1_for_trade_in"))
        return self
    }

    @discardableResult
    func mock_userOffersCarsCouple() -> Self {
        server.api.user.offers.category(.cars).get(parameters: .wildcard)
            .ok(mock: .file("user_offers_cars_2_for_trade_in"))
        return self
    }

    @discardableResult
    func mock_userOffersAllExternalPanoramaNoPOI() -> Self {
        server.addHandler("GET /user/offers/all?page=1&page_size=10&with_daily_counters=true") {
            .init(mockFile: "GET user_offers_all_external_Panoramas_no_poi") as Auto_Api_OfferListingResponse
        }
        return self
    }

    @discardableResult
    func mock_userOfferFromAllExternalPanoramaNoPOI() -> Self {
        let panoramaURLStr = "http://127.0.0.1:\(port)/test.mp4"
        let handler = { () -> Auto_Api_OfferResponse in
            var resp = .init(mockFile: "GET user_offers_CARS_1101389279-dccb254c") as Auto_Api_OfferResponse
            resp.offer.state.externalPanorama.published.videoMp4R16X9.fullURL = panoramaURLStr
            return resp
        }
        server.addHandler("GET /user/offers/CARS/1101389279-dccb254c") {
            handler()
        }
        return self
    }

    @discardableResult
    func mock_fullPanoramaFailToLoad() -> Self {
        server.addHandler("GET /test.mp4") {
            Response.badResponse(code: .externalServiceUnavailable)
        }
        return self
    }

    @discardableResult
    func mock_userOfferFromAllExternalPanoramaNoPOIStats() -> Self {
        server.addHandler("GET /user/offers/CARS/1101389279-dccb254c/stats?from=2021-06-01&to=2021-06-08") {
            .init(mockFile: "GET user_offers_CARS_1101389279-dccb254c_stats") as Auto_Api_DailyCountersResponse
        }
        return self
    }

    @discardableResult
    func mock_userOfferFromAllExternalPanoramaNoPOIDraft() -> Self {
        server.addHandler("POST /user/offers/CARS/1101389279-dccb254c/edit") {
            .init(mockFile: "POST user_offers_CARS_DRAFT_1101389279-dccb254c") as Auto_Api_OffersSaveSuccessResponse
        }
        return self
    }

    @discardableResult
    func mock_userOfferFromAllExternalPanoramaNoPOIDraftSuggest() -> Self {
        server.addHandler("GET /reference/catalog/CARS/suggest?body_type=SEDAN&engine_type=GASOLINE&gear_type=FORWARD_CONTROL&mark=AUDI&model=80&super_gen=7878108&tech_param_id=7879201&transmission=MECHANICAL&year=1995") {
            Response.okResponse(fileName: "garage_form_suggest_mark", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_activateUserOfferFrom(category: String, id offerID: String) -> Self {
        server.addHandler("POST /user/offers/\(category.uppercased())/\(offerID)/activate") { (request, _) in
            Response.okResponse(fileName: "success")
        }
        return self
    }

    @discardableResult
    func mock_userOffer(
        id: String,
        category: Auto_Api_Category = .cars,
        status: Auto_Api_OfferStatus = .active,
        hasActivationWithAutoprolongation: Bool = false,
        searchPosition: Int? = nil,
        counterAll: Int? = nil,
        nds: Bool = false,
        description: String? = nil,
        isBroken: Bool? = nil
    ) -> Self {
        let categoryName: String = {
            switch category {
            case .cars: return "cars"
            case .trucks: return "trucks"
            case .moto: return "moto"
            default: fatalError()
            }
        }()

        server.addHandler("GET /user/offers/\(categoryName)/\(id) *") { (_, _) -> Response? in
            var response = Auto_Api_OfferResponse()
            let mockUrl = Bundle.resources
                .url(forResource: "user_sale_list_premium_offer_inactive", withExtension: "json")!
            let mock: Auto_Api_OfferListingResponse = Auto_Api_OfferListingResponse.fromFile(path: mockUrl)
            var offer = mock.offers[0]

            let params = OfferParameters(id: id, category: category, status: status, nds: nds, description: description, isBroken: isBroken)
            mock_fillOfferWithParameters(offer: &offer, params: params)

            let activateVAS = offer.servicePrices.first(where: { $0.service == "all_sale_activate" })!
            offer.servicePrices = offer.servicePrices.filter { $0.service != activateVAS.service }

            if hasActivationWithAutoprolongation {
                offer.servicePrices.append(activateVAS)
            }

            if let position = searchPosition {
                offer.searchPosition = Int32(position)
            }

            if let counter = counterAll {
                offer.counters.all = Int32(counter)
            }

            response.offer = offer

            return Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_userDraft(
        id: String,
        category: Auto_Api_Category = .cars
    ) -> Self {
        let categoryName: String = {
            switch category {
            case .cars: return "cars"
            case .trucks: return "trucks"
            case .moto: return "moto"
            default: fatalError()
            }
        }()

        server.addHandler("GET /user/draft/\(categoryName) *") { (_, _) -> Response? in
            var response = Auto_Api_OfferResponse()
            let mockUrl = Bundle.resources
                .url(forResource: "user_sale_list_premium_offer_inactive", withExtension: "json")!
            let mock: Auto_Api_OfferListingResponse = Auto_Api_OfferListingResponse.fromFile(path: mockUrl)
            var offer = mock.offers[0]

            offer.id = id
            offer.status = .draft
            offer.category = category
            offer.documents = Auto_Api_Documents()
            offer.carInfo.modelInfo = Auto_Api_Model()
            offer.carInfo.superGen = Auto_Api_SuperGeneration()
            offer.carInfo.configuration = Auto_Api_Configuration()
            offer.carInfo.techParam = Auto_Api_TechParam()

            response.offer = offer

            return Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_userOfferStats(
        id: String,
        category: Auto_Api_Category = .cars
    ) -> Self {
        let categoryName: String = {
            switch category {
            case .cars: return "cars"
            case .trucks: return "trucks"
            case .moto: return "moto"
            default: fatalError()
            }
        }()

        server.addHandler("GET /user/offers/\(categoryName)/\(id)/stats *") { (_, _) -> Response? in
            var response = .init(mockFile: "GET user_offers_TRUCKS_16227978-01dc7292_stats") as Auto_Api_DailyCountersResponse

            response.items[0].offerID = id

            return Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_userOffers(
        counters: [Int: (searchPosition: Int, counterAll: Int)],
        normalizeIDs: Bool = false
    ) -> Self {
        var response = .init(mockFile: "GET user_offers_all") as Auto_Api_OfferListingResponse

        for (key, val) in counters {
            response.offers[key].searchPosition = Int32(val.searchPosition)
            response.offers[key].counters.all = Int32(val.counterAll)
        }

        if normalizeIDs {
            response.offers = response.offers.enumerated().map {
                var offer = $0.element
                offer.id = "\($0.offset)"
                return offer
            }
        }

        server.addHandler("GET /user/offers/all *") { (_, _) in
            Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }

        return self
    }

    @discardableResult
    func mock_userOffersAllWithActive() -> Self {
        let response: Auto_Api_OfferListingResponse = .init(mockFile: "GET user_offers_all_onboarding")

        server.addHandler("GET /user/offers/all *") { (_, _) in
            Response.responseWithStatus(body: try! response.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_userOffersVAS() -> Self {
        server.api.user.offers.category(._unknown("all"))
            .get(parameters: .wildcard)
            .ok(mock: .file("GET user_offers_vas"))

        return self
    }

    @discardableResult
    func mock_userOfferProductProlongable(offerId: String, product: String) -> Self {
        server.addHandler("PUT /user/offers/cars/\(offerId)/product/\(product)/prolongable *") { (request, _) in
            Response.okResponse(fileName: "success")
        }
        return self
    }

    @discardableResult
    func mock_userOfferDescriptionParseOptions(isNds: Bool) -> Self {
        server.addHandler("POST /reference/catalog/cars/parse-options *") { (request, _) in
            var result = Auto_Api_OfferDescriptionParseResponse()
            result.result.showWithNds.value = isNds
            return Response.responseWithStatus(body: try! result.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_userOffersWithGaragePromo() -> Self {
        server.api.user.offers.category(._unknown("all")).get(parameters: .wildcard)
            .ok(mock: .file("GET user_offers_all") { response in
                response.offers[0].additionalInfo.garageInfo.acceptableForGarage = true
            })
        return self
    }

    @discardableResult
    func mock_userOfferWithGaragePromo(_ id: String) -> Self {
        server.api.user.offers.category(.trucks).offerID(id).get
            .ok(mock: .file("GET user_offers_CARS_1101389279-dccb254c") { response in
                response.offer.additionalInfo.garageInfo.acceptableForGarage = true
                response.offer.status = .active
            })
        return self
    }

    @discardableResult
    func mock_userOfferHide(_ id: String) -> Self {
        server.api.user.offers.category(.cars).offerID(id).hide.post
            .ok(mock: .file("user_offers_CARS_1097256960-ad2747f9_hide_ok"))
        return self
    }

    @discardableResult
    func mock_registerCallback(_ id: String) -> Self {
        server.api.offer.category(.cars).offerID(id).registerCallback.post
            .ok(mock: .file("success"))
        return self
    }
}

extension Array where Element: Hashable {
    func unique() -> [Element] {
        var alreadyAdd = Set<Element>()
        return self.filter { alreadyAdd.update(with: $0) == nil }
    }
}
