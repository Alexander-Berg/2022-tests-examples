//
//  SearchMocker.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf
import XCTest

struct SearchMockerLocatorCounterDistance {
    var radius: Int32
    var count: Int32
    var distanceToOffer: Auto_Api_DistanceToTarget?
}

struct SearchMockerDealerAuctionInfo {
    var offersCount: Int
    var salon: Auto_Api_Salon = {
        let response: Auto_Api_SalonResponse = .init(mockFile: "lada_tula")
        return response.salon
    }()

    func adapted(offer: Auto_Api_Offer) -> Auto_Api_Offer {
        var offer = offer

        offer.salon = salon

        offer.tags.append("auction_leader")
        offer.tags = Array(Set(offer.tags))

        offer.grouppingInfo.dealers = offer.grouppingInfo.dealers.filter { $0.salon.dealerID != salon.dealerID }
        offer.grouppingInfo.dealers.append(
            .with { model in
                model.offersCount = Int32(offersCount)
                model.salon = salon
            }
        )

        return offer
    }
}

enum SearchMockerOfferLocatorCounterType: String {
    case cars
    case moto
    case trucks
    case all
}

extension Mocker {

    @discardableResult
    func mock_searchCarsSpecials() -> Self {
        server.addHandler("POST /search/cars/specials *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_cars_specials")
        }
        return self
    }

    @discardableResult
    func mock_searchCarsContextRecommendNewInStock() -> Self {
        server.addHandler("POST /search/cars/context/recommend-new-in-stock *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_cars_context_recommend-new-in-stock")
        }
        return self
    }

    @discardableResult
    func mock_searchMoto() -> Self {
        server.addHandler("POST /search/moto *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_moto")
        }
        return self
    }

    @discardableResult
    func mock_searchTrucks() -> Self {
        server.addHandler("POST /search/trucks *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_trucks")
        }
        return self
    }

    @discardableResult
    func mock_searchCarsRelated() -> Self {
        server.addHandler("POST /search/cars/related *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_cars_related")
        }
        return self
    }

    @discardableResult
    func mock_searchHistory(rid: UInt32 = 10702,
                           geoRadius: UInt32 = 0,
                           state: Auto_Api_Ui_StateGroup = .all,
                           accelerationFrom: UInt32? = nil,
                           isCar: Bool = true) -> Self {
        server.addHandler("GET /search/history") { (_, _) -> Response? in
            var resp: Auto_Api_SavedSearchesListing
            if isCar {
                resp = .init(mockFile: "searchHistory")
            } else {
                resp = .init(mockFile: "MotoComsearchHistory")
            }
            resp.savedSearches[0].params.rid = [rid]
            resp.savedSearches[0].params.stateGroup = state
            resp.savedSearches[0].params.geoRadius = geoRadius

            if let accelerationFrom = accelerationFrom {
                resp.savedSearches[0].params.accelerationFrom = accelerationFrom
            }

            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_searchCars(newCount: Int,
                        usedCount: Int,
                        isSalon: Bool = false,
                        isNDS: Bool = false,
                        dealerAuctions: [Int: SearchMockerDealerAuctionInfo] = [:],
                        distances: [SearchMockerLocatorCounterDistance] = [
                            .init(radius: 100, count: 1),
                            .init(radius: 200, count: 2),
                            .init(radius: 300, count: 3),
                            .init(radius: 400, count: 4),
                            .init(radius: 500, count: 5),
                            .init(radius: 600, count: 6),
                            .init(radius: 700, count: 7),
                            .init(radius: 800, count: 8),
                            .init(radius: 900, count: 9),
                            .init(radius: 1000, count: 10),
                            .init(radius: 1100, count: 11)
                        ]) -> Self {
        server.addHandler("POST /search/cars *") { (requestEnt, _) -> Response? in
            var resp: Auto_Api_OfferListingResponse = .init(mockFile: "searchCar")
            let request = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: requestEnt.messageBody!)
            var offer = resp.offers[0]
            if isSalon {
                offer = .init(mockFile: "dealerOffer")
            }
            if isNDS {
                XCTAssert(request.onlyNds, "Если был выбран фильтр ндс, то он должен попасть в параметры.")
            }
            let count: Int
            switch request.stateGroup {
            case .all, .used:
                resp.clearGrouping()
                offer.section = .used
                count = usedCount
                break
            case .new:
                if newCount != 0 {
                    resp.grouping.groupsCount = 1
                    resp.grouping.singleOffersCount = 0
                    offer.grouppingInfo = .init(mockFile: "groupInfo")
                }

                offer.section = .new
                count = newCount
            }

            guard let pageString = URLComponents(string: requestEnt.uri)?.queryItems?.first(where: {$0.name == "page"})?.value, let page = Int(pageString) else { fatalError() }
            if request.excludeRid.isEmpty && !request.hasExcludeGeoRadius { // запрос основного листинга
                var offers: [Auto_Api_Offer] = []
                var start = 0

                start += (page - 1) * 20
                for i in start ..< count {
                    var offer = offer
                    offer.id = "\(i)"

                    offer.carInfo.markInfo.name = offer.id

                    if offer.section == .new,
                       !dealerAuctions.isEmpty {
                        XCTAssert(isSalon, "Аукцион в моке может быть только на дилерском оффере")
                        offer = dealerAuctions[Int(i)]?.adapted(offer: offer) ?? offer
                    }

                    offers.append(offer)
                }
                resp.offers = offers
                resp.pagination.totalOffersCount = Int32(count)
                resp.pagination.totalPageCount = Int32((count / 20) + 1)
                resp.pagination.page = Int32(page)
            } else { // запрос БЛ
                // последнее кольцо в БЛ (радиус +1100, но запрашиваем с radius == 0)
                let isWholeRussiaRequest = request.geoRadius == 0 && request.hasExcludeGeoRadius
                let requestRadius = isWholeRussiaRequest ? 1100 : request.geoRadius

                let currentIndex = !request.excludeRid.isEmpty
                    ? distances.firstIndex(where: { $0.count != 0 })!
                    : distances.firstIndex(where: { $0.radius == requestRadius })!
                let count = currentIndex == 0
                    ? distances[currentIndex].count
                    : distances[currentIndex].count - distances[currentIndex - 1].count

                var offers: [Auto_Api_Offer] = []
                for i in 0 ..< count {
                    var offer = offer
                    let distanceInfo = distances[currentIndex]
                    offer.id = "\(distanceInfo.radius)_\(i)"
                    offer.carInfo.markInfo.name = offer.id
                    distanceInfo.distanceToOffer.flatMap { offer.seller.location.distanceToSelectedGeo = [$0] }

                    if offer.section == .new,
                       !dealerAuctions.isEmpty {
                        XCTAssert(isSalon, "Аукцион в моке может быть только на дилерском оффере")
                        offer = dealerAuctions[Int(i)]?.adapted(offer: offer) ?? offer
                    }

                    offers.append(offer)
                }
                resp.offers = offers
                resp.pagination.totalOffersCount = Int32(count)
                resp.pagination.totalPageCount = Int32((count / 20) + 1)
                resp.pagination.page = Int32(page)
            }

            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }
        return self
    }

    @discardableResult
    func mock_searchCars(_ getResponse: @escaping () -> Auto_Api_OfferListingResponse) -> Self {
        server.addHandler("POST /search/cars *") {
            getResponse()
        }
        return self
    }

    @discardableResult
    func mock_searchOfferLocatorCounters(
        type: SearchMockerOfferLocatorCounterType,
        distances: [SearchMockerLocatorCounterDistance] = [
            .init(radius: 100, count: 1),
            .init(radius: 200, count: 2),
            .init(radius: 300, count: 3),
            .init(radius: 400, count: 4),
            .init(radius: 500, count: 5),
            .init(radius: 600, count: 6),
            .init(radius: 700, count: 7),
            .init(radius: 800, count: 8),
            .init(radius: 900, count: 9),
            .init(radius: 1000, count: 10),
            .init(radius: 1100, count: 11)
        ]
    ) -> Self {
        var resp: Auto_Api_OfferLocatorCounterResponse = .init(mockFile: "locatorCounterDist")

        var distancesApi: [Auto_Api_OfferLocatorCounter] = []
        for distance in distances {
            var locatorCounter = resp.distances[0]
            locatorCounter.count = distance.count
            locatorCounter.radius = distance.radius
            distancesApi.append(locatorCounter)
        }
        resp.distances = distancesApi

        if type == .all {
            server.addHandler("POST /search/cars/offer-locator-counters *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
            }

            server.addHandler("POST /search/moto/offer-locator-counters *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
            }

            server.addHandler("POST /search/trucks/offer-locator-counters *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
            }
        } else {
            server.addHandler("POST /search/\(type.rawValue)/offer-locator-counters *") { (_, _) -> Response? in
                return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
            }
        }
        return self
    }

    @discardableResult
    func mock_savedSearch(_ id: String) -> Self {
        server.addHandler("POST /search/saved/\(id) *") { request, _ in
            Response.okResponse(fileName: "search_saved_by_id")
        }
        return self
    }

    @discardableResult
    func mock_searchCount(count: Int, pageSize: Int = 20) -> Self {
        server.addHandler("POST /search/cars/count *") { _, _ in
            let response = Auto_Api_OfferCountResponse.with { msg in
                msg.count = Int32(count)
                msg.pagination = .with { pagination in
                    pagination.page = 1
                    pagination.pageSize = Int32(pageSize)
                    pagination.totalOffersCount = Int32(count)
                    pagination.totalPageCount = Int32((Double(count) / Double(pageSize)).rounded(.up))
                }
            }
            return Response.okResponse(message: response)
        }
        return self
    }

    @discardableResult
    func mock_searchEquipment() -> Self {
        server.addHandler("POST /search/CARS/equipment-filters *") { _, _ in
            return Response.okResponse(fileName: "complectation_comparison_equipment_filters")
        }
        return self
    }
}
