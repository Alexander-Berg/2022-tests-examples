//
//  PremiumOffersAssistantBannersTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 2/1/21.
//

import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

final class PremiumOffersAssistantBannersTests: BaseTest {

    override var appSettings: [String: Any] {
        var settings = super.appSettings
        settings["enablePremiumAssistant"] = 1
        return settings
    }

    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    private var offerActiveResponse: Auto_Api_OfferResponse = {
        let offer = try! Auto_Api_OfferResponse(jsonUTF8Data: getStubBody(resource: "user_offers_cars_1_ok"))
        return offer
    }()

    private var offerInactiveResponse: Auto_Api_OfferResponse = {
        let offer = try! Auto_Api_OfferResponse(jsonUTF8Data: getStubBody(resource: "user_offers_cars_1_inactive_ok"))
        return offer
    }()

    private lazy var draftResponse: Auto_Api_DraftResponse = {
        let draft = try! Auto_Api_DraftResponse(jsonUTF8Data: PremiumOffersAssistantBannersTests.getStubBody(resource: "offer_edit_get_draft"))
        return draft
    }()

    private static func getStubBody(resource: String) -> Data {
        let filePath = Bundle.resources.url(forResource: resource, withExtension: "json")
        let body: Data = filePath.flatMap { try? Data(contentsOf: $0 ) }!
        return body
    }

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        setupServer()
        launch()
        super.setUp()
    }

    private func setupServer() {
        advancedMockReproducer.setup(server: self.server, mockFolderName: "ChatNewMessages")
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=true") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /user?with_auth_types=false") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.userProfile.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /geo/suggest *") { (_, _) -> Response? in
            let regions: [UInt64] = [1, 213, 2, 10174, 43]
            var response = Auto_Api_GeoSuggestResponse()

            regions.forEach { (id) in
                var region = Auto_Api_RegionInfo()

                region.id = id
                region.supportsGeoRadius = true
                region.defaultRadius = 200

                response.regions.append(region)
            }

            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)

        }

        try! server.start()
    }

    func test_premiumOfferAssistantBannerActive() {
        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_premium_offer_active")
        }

        server.addHandler("GET /user/offers/CARS/1097093888-84b3a4c4") { (_, _) -> Response? in
            var response = self.offerActiveResponse
            var servicePrices = response.offer.servicePrices
            var activateService = servicePrices.first(where: { service in
                return service.service == "all_sale_activate"
            })
            activateService?.paidReason = .premiumOffer
            servicePrices.removeAll(where: { service in
                return service.service == "all_sale_activate"
            })
            servicePrices.append(activateService!)
            response.offer.servicePrices = servicePrices
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: false)
        }

        let steps = mainSteps.openOffersTab()

        Step("Баннер ассистента для активного премиум оффера в списке") {
            steps
                .scrollToPremiumAssistantBanner()
                .validatePremiumAssistantBannerActiveScreenshoot()
        }

        Step("Баннер ассистента для активного премиум оффера в списке открывает чат") {
            steps.tapPremiumAssistantBannerButton()
                .exist(selector: "Сообщение")
                .tapBackButton()
        }

        Step("Баннер ассистента для активного премиум оффера на карточке открывает чат") {
            steps.scrollToTop()
                .openOffer(offerId: "1097093888-84b3a4c4")
                .checkScreenLoaded()
                .scrollToPremiumAssistantBanner()
                .tapPremiumAssistantBannerButton()
                .exist(selector: "Сообщение")
        }
    }

    func test_premiumOfferAssistantBannerInactive() {
        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_premium_offer_inactive")
        }

        server.addHandler("GET /user/offers/CARS/1097093888-84b3a4c4") { (_, _) -> Response? in
            var response = self.offerInactiveResponse
            var servicePrices = response.offer.servicePrices
            var activateService = servicePrices.first(where: { service in
                return service.service == "all_sale_activate"
            })
            activateService?.paidReason = .premiumOffer
            servicePrices.removeAll(where: { service in
                return service.service == "all_sale_activate"
            })
            servicePrices.append(activateService!)
            response.offer.servicePrices = servicePrices
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: false)
        }

        let steps = mainSteps.openOffersTab()

        Step("Баннер ассистента для неактивного премиум оффера в списке") {
            steps.validatePremiumAssistantBannerInactiveScreenshoot()
        }
        Step("Баннер ассистента для неактивного премиум оффера на карточке") {
            steps.openOffer(offerId: "1097093888-84b3a4c4")
                .checkScreenLoaded()
                .chackPremiumAssistantInactiveBannerExists()
        }
    }

    func test_premiumOfferAssistantBannerFullForm() {
        server.addHandler("GET /reference/catalog/cars/all-options") { (request, _) -> Response? in
            return Response.okResponse(fileName: "offer_edit_get_equipment", userAuthorized: false)
        }
        server.addHandler("GET /reference/catalog/CARS/suggest?body_type=LIFTBACK&engine_type=GASOLINE&gear_type=REAR_DRIVE&mark=BMW&model=4&super_gen=20906765&tech_param_id=20907126&transmission=AUTOMATIC&year=2018") { (request, _) -> Response? in
            return Response.okResponse(fileName: "offer_edit_get_suggestions", userAuthorized: true)
        }

        var draftResponse: Auto_Api_DraftResponse = self.draftResponse
        var services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVAS = services[activationVASIndex]
        activationVAS.recommendationPriority = 2
        activationVAS.price = 1000
        activationVAS.paidReason = .premiumOffer
        activationVAS.originalPrice = 2000
        activationVAS.days = 7
        services[activationVASIndex] = activationVAS

        draftResponse.servicePrices = services

        server.addHandler("GET /user/draft/CARS") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! draftResponse.jsonUTF8Data(), userAuthorized: false)
        }
        server.addHandler("GET /user/draft/CARS/5124032264023535200-83a820a0") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! draftResponse.jsonUTF8Data(), userAuthorized: false)
        }

        let steps = mainSteps
            .openOffersTab()
            .tapAddOffer()
            .tapToCarsCategory()
            .wait(for: 2)

        Step("Баннер ассистента для неактивного премиум оффера на экране редактирования") {
            let scroller = steps.onOfferEditScreen().scrollableElement
            let cell = steps.onOfferEditScreen().cellFor(.publish)
            scroller.scrollTo(element: cell, swipeDirection: .up)
            steps.onOfferEditScreen().premiumOfferAssistantInactiveBanner.shouldExist()
        }
    }
}
