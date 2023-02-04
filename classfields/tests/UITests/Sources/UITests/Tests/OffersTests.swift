//
//  OffersTests.swift
//  UITests
//
//  Created by Arkady Smirnov on 9/19/19.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuUserSaleCard AutoRuReviewEditor AutoRuOfferAdvantage
class OffersTests: BaseTest {
    private static var needSetup: Bool = true
    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    private var offerResponse: Auto_Api_OfferResponse = {
        let filePath = Bundle.resources.url(forResource: "user_offers_cars_1_ok", withExtension: "json")
        let body: Data = filePath.flatMap { try? Data(contentsOf: $0 ) }!
        let offer = try! Auto_Api_OfferResponse(jsonUTF8Data: body)
        return offer
    }()

    private var geoSuggestResponse: Auto_Api_GeoSuggestResponse?

    private var didLoad: Bool = false

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["DRIVE_MOCK_PROMO"] = "drive_tests_promo"
        return env
    }

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    private func setupServer() {
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

        server.addHandler("GET /story/search") { (request, _) -> Response? in
            return Response.okResponse(fileName: "story_search_ok", userAuthorized: false)
        }

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "get_user_sale_paged_ok", userAuthorized: false)
        }

        server.addHandler("GET /user/offers/CARS/\(offerResponse.offer.id)") { (request, _) -> Response? in
            return Response.responseWithStatus(body: try! self.offerResponse.jsonUTF8Data(), userAuthorized: false)
        }

        server.addHandler("GET /geo/suggest *") { (_, _) -> Response? in
            while !self.didLoad {
                usleep(300)
            }

            return self.geoSuggestResponse.flatMap { (response) -> Response? in
                return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: false)
            }
        }

        try! server.start()
    }

    lazy var mainSteps = MainSteps(context: self)

    func test_addReviewAfterDeactivate() {
        didLoad = true

        let category = "cars"
        let offerId = "1"

        server.addHandler("GET /user/reviews *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_reviews_ok", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/\(category.uppercased())/offer/\(offerId)?campaign=aftersale_promo_ios") { (request, _) -> Response? in
            return Response.okResponse(fileName: "review_fromOffer_ok", userAuthorized: false)
        }

        server.addHandler("POST /user/offers/\(category.uppercased())/\(offerId)/hide") { (request, _) -> Response? in
            return Response.responseWithStatus(body: "{\"status\":\"SUCCESS\"}".data(using: .utf8), userAuthorized: false)
        }

        server.addHandler("GET /user/offers/\(category.uppercased())/\(offerId)") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_cars_1_ok", userAuthorized: false)
        }

        mainSteps
            .openOffersTab()
            .tapDeactivate()
            .selectReason(.rethink)
            .exist("Поделитесь опытом")
            .tapAddReview()
            .exist("Внешний вид")
    }

    func test_addReviewAfterDeactivate_alreadyHasReview() {
        didLoad = true

        let category = "cars"
        let offerId = "1"

        server.addHandler("GET /user/reviews *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_reviews_with_1_ok", userAuthorized: false)
        }

        server.addHandler("POST /user/offers/\(category.uppercased())/\(offerId)/hide") { (request, _) -> Response? in
            return Response.responseWithStatus(body: "{\"status\":\"SUCCESS\"}".data(using: .utf8), userAuthorized: false)
        }

        server.addHandler("GET /user/offers/\(category.uppercased())/\(offerId)") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_cars_1_ok", userAuthorized: false)
        }

        mainSteps
            .openOffersTab()
            .tapDeactivate()
            .selectReason(.rethink)
            .notExist(selector: "Поделитесь опытом")
    }

    func test_userOffer_shoudBeApprovedOwnerBadgeOnActiveOffer() {
        didLoad = true

        server.addHandler("GET /chat/room/tech-support") { (request, _) -> Response? in
            return Response.okResponse(fileName: "chat_room_techsupport_GET_ok", userAuthorized: false)
        }

        server.addHandler("GET /chat/message *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_GET_ok", userAuthorized: false)
        }

        server.addHandler("POST /chat/message *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_POST_ok", userAuthorized: false)
        }

        server.addHandler("DELETE /chat/message/unread *") { (request, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_unread_DELETE_ok", userAuthorized: false)
        }

        let steps = mainSteps
            .openOffersTab()
            .openOffer(offerId: "1")
            .exist(selector: "advantages")
            .validateSnapShot(accessibilityId: "advantages", snapshotId: "proven_owner_inactive")
            .tapOnApprovedSellerBadge()
            .exist(selector: "ModalViewControllerHost")
            .validateSnapShot(accessibilityId: "ModalViewControllerHost", snapshotId: "approvedSellerDialog")

        steps.tapApprovedSellerDialogButton()
        steps.app.tap()

        Snapshot.compareWithSnapshot(
            image: mainSteps.app.waitAndScreenshot().image
        )
    }

    func test_userOffer_instantModalForProvenOwnerApproved() {
        server.addHandler("GET /user/offers/CARS/\(offerResponse.offer.id)") { (_, _) -> Response? in
            var response = self.offerResponse
            response.offer.tags.append("proven_owner")
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: false)
        }

        didLoad = true

        server.addHandler("GET /chat/room/tech-support") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_room_techsupport_GET_ok", userAuthorized: false)
        }

        server.addHandler("GET /chat/message *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_GET_ok", userAuthorized: false)
        }

        server.addHandler("POST /chat/message *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_POST_ok", userAuthorized: false)
        }

        server.addHandler("DELETE /chat/message/unread *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_unread_DELETE_ok", userAuthorized: false)
        }

        mainSteps
            .openOffersTab()
            .openOffer(offerId: "1")
            .exist(selector: "advantages")
            .validateSnapShot(accessibilityId: "advantages", snapshotId: "test_userOffer_instantModalForProvenOwnerApproved_badge")
            .tapOnAdvantages()
            .validateSnapshot(of: "ModalViewControllerHost", ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 48, right: 0), snapshotId: "test_userOffer_instantModalForProvenOwnerApproved_modal")
    }

    func test_userOffer_instantModalForProvenOwnerVerifying() {
        server.addHandler("GET /user/offers/CARS/\(offerResponse.offer.id)") { (_, _) -> Response? in
            var response = self.offerResponse
            response.offer.state.documentImage = [
                Auto_Api_Photo.with { $0.photoType = .stsBack },
                Auto_Api_Photo.with { $0.photoType = .stsFront },
                Auto_Api_Photo.with { $0.photoType = .drivingLicense }
            ]
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: false)
        }

        didLoad = true

        server.addHandler("GET /chat/room/tech-support") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_room_techsupport_GET_ok", userAuthorized: false)
        }

        server.addHandler("GET /chat/message *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_GET_ok", userAuthorized: false)
        }

        server.addHandler("POST /chat/message *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_POST_ok", userAuthorized: false)
        }

        server.addHandler("DELETE /chat/message/unread *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "chat_message_unread_DELETE_ok", userAuthorized: false)
        }

        mainSteps
            .openOffersTab()
            .openOffer(offerId: "1")
            .exist(selector: "advantages")
            .validateSnapShot(accessibilityId: "advantages", snapshotId: "test_userOffer_instantModalForProvenOwnerVerifying_badge")
            .tapOnAdvantages()
            .validateSnapshot(of: "ModalViewControllerHost", ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 48, right: 0), snapshotId: "test_userOffer_instantModalForProvenOwnerVerifying_modal")
    }

    func test_userOffer_shoudNotBeApprovedOwnerBadgeOnInactiveOffer() {
        didLoad = true

        offerResponse.offer.status = .inactive

        mainSteps
            .openOffersTab()
            .openOffer(offerId: "1")
            .notExist(selector: "proven_owner_inactive")
    }

    func test_userOffer_shoudBeAdvantageBadgeAndApprovedOwnerBadge() {
        didLoad = true

        offerResponse.offer.tags = ["online_view_available"]

        mainSteps
            .openOffersTab()
            .openOffer(offerId: "1")
            .exist(selector: "advantages")
            .validateSnapShot(accessibilityId: "advantages", snapshotId: "advantages")
    }

    func test_userOffer_shoudNotBeApprovedOwnerBadgeIfAlreadyApproved() {
        didLoad = true

        offerResponse.offer.tags = ["proven_owner", "online_view_available", "almost_new"]

        mainSteps
            .openOffersTab()
            .openOffer(offerId: "1")
            .exist(selector: "advantages")
            .notExist(selector: "proven_owner_inactive")
            .validateSnapShot(accessibilityId: "advantages", snapshotId: "advantagesWithproven_owner")
    }

    func test_autoprolongationWarning() {
        didLoad = true

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            let filePath = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json")
            let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
            var response = body.flatMap { try? Auto_Api_OfferListingResponse(jsonUTF8Data: $0) }!
            var offer = response.offers.first!

            let activateVASIndex = offer.services.firstIndex(where: { $0.service == "all_sale_activate" })!
            var activateServiceVAS = offer.services[activateVASIndex]
            activateServiceVAS.prolongable = false
            offer.additionalInfo.expireDate = UInt64(Date.init(timeInterval: 100000, since: Date()).timeIntervalSince1970) * 1000

            let activatePriceVASIndex = offer.servicePrices.firstIndex(where: { $0.service == "all_sale_activate" })!
            var activateServicePriceVAS = offer.servicePrices[activatePriceVASIndex]
            activateServicePriceVAS.prolongationAllowed = true
            activateServicePriceVAS.prolongationForcedNotTogglable = true

            offer.services[activateVASIndex] = activateServiceVAS
            offer.servicePrices[activatePriceVASIndex] = activateServicePriceVAS
            response.offers[0] = offer
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }
        mainSteps
            .openOffersTab()
            .exist("autoprolongation_warning")
            .validateSnapShot(accessibilityId: "autoprolongation_warning")
    }

    func test_activationDisabled() {
        didLoad = true
        let offerID = "1234"

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            let filePath = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json")
            let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
            var response = body.flatMap { try? Auto_Api_OfferListingResponse(jsonUTF8Data: $0) }!
            var offer = response.offers.first!
            offer.actions.activate = false
            offer.status = .inactive
            offer.id = offerID
            response.offers[0] = offer
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }
        mainSteps
            .openOffersTab()
            .validateSnapShot(accessibilityId: "offer_\(offerID)_archiveSnippetActions")
    }

    func test_noActivationPrice() {
        didLoad = true
        let offerID = "1234"

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            let filePath = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json")
            let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
            var response = body.flatMap { try? Auto_Api_OfferListingResponse(jsonUTF8Data: $0) }!
            var offer = response.offers.first!
            offer.actions.activate = true
            offer.servicePrices = [] // simulate offline salesman
            offer.status = .inactive
            offer.id = offerID
            response.offers[0] = offer
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }
        mainSteps
            .openOffersTab()
            .validateSnapShot(accessibilityId: "offer_\(offerID)_archiveSnippetActions")
    }

    func test_userBanned() {
        server.addHandler("GET /session") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_session_banned", userAuthorized: true)
        }

        mainSteps
            .openOffersTab()
            .exist("user_banned_warning")
            .validateSnapShot(accessibilityId: "user_banned_warning")
    }

    func test_userBannedReseller() {
        server.addHandler("GET /session") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_session_banned_reseller", userAuthorized: true)
        }

        mainSteps
            .openOffersTab()
            .notExist("user_banned_warning")
    }

    func test_userBannedButNotNotified() {
        server.addHandler("GET /session") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_session_banned_in_not_essentials", userAuthorized: true)
        }

        mainSteps
            .openOffersTab()
            .notExist(selector: "user_banned_warning")
    }

    func test_userBannedOpenSupportChat() {
        server.addHandler("GET /session") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_session_banned", userAuthorized: true)
        }

        server.addHandler("GET /chat/room/tech-support *") { (_, _) -> Response? in
            Response.okResponse(fileName: "chat_room_techsupport_GET_ok", userAuthorized: true)
        }

        mainSteps
            .openOffersTab()
            .exist("user_banned_warning")
            .tapUserBannedSupportButton()
        .wait()
        .should(provider: .chatScreen, .exist)
    }

    func test_noDriveBannerForNoRegion() {
        didLoad = true

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_1_active")
        }

        let steps = mainSteps.openOffersTab()

        Step("Не должно быть банера для пустого региона") {
            steps.onOffersScreen().scrollableElement.swipeUp()
            steps.onOffersScreen().driveBanner.shouldNotExist()
        }
    }

    func test_hasDriveBannerAfterActiveOffer() {
        let regions: [UInt64] = [1, 213, 2, 10174, 43]
        var geoSuggestResponse = Auto_Api_GeoSuggestResponse()

        regions.forEach { (id) in
            var region = Auto_Api_RegionInfo()

            region.id = id
            region.supportsGeoRadius = true
            region.defaultRadius = 200

            geoSuggestResponse.regions.append(region)
        }

        self.geoSuggestResponse = geoSuggestResponse
        didLoad = true

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_1_active")
        }

        let steps = mainSteps.openOffersTab()

        Step("Должен быть баннер для поддерживаемых регионов (мск, спб, каз)") {
            let scroller = steps.onOffersScreen().scrollableElement
            scroller.swipeUp()
            scroller.swipeUp()

            validateSnapshots(
                accessibilityId: steps.onOffersScreen().driveBanner.identifier,
                snapshotId: "test_hasDriveBannerAfterActiveOffer"
            )
        }
    }

    func test_noDriveBannerAfterInactiveOffer() {
        let regions: [UInt64] = [1, 213, 2, 10174, 43]
        var geoSuggestResponse = Auto_Api_GeoSuggestResponse()

        regions.forEach { (id) in
            var region = Auto_Api_RegionInfo()

            region.id = id
            region.supportsGeoRadius = true
            region.defaultRadius = 200

            geoSuggestResponse.regions.append(region)
        }

        self.geoSuggestResponse = geoSuggestResponse
        didLoad = true

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_1_inactive")
        }

        let steps = mainSteps.openOffersTab()

        Step("не должно быть банера если только неактивный оффер") {
            let scroller = steps.onOffersScreen().scrollableElement
            scroller.swipeUp()
            scroller.swipeUp()

            steps.onOffersScreen().driveBanner.shouldNotExist()
        }
    }

    func test_canOpenDriveBanner() {
        let regions: [UInt64] = [1, 213, 2, 10174, 43]
        var geoSuggestResponse = Auto_Api_GeoSuggestResponse()

        regions.forEach { (id) in
            var region = Auto_Api_RegionInfo()

            region.id = id
            region.supportsGeoRadius = true
            region.defaultRadius = 200

            geoSuggestResponse.regions.append(region)
        }

        self.geoSuggestResponse = geoSuggestResponse
        didLoad = true

        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_1_active")
        }

        let steps = mainSteps.openOffersTab()

        Step("Должен открыться юрл по тапу `Перейти`") {
            let scroller = steps.onOffersScreen().scrollableElement
            scroller.swipeUp()
            scroller.swipeUp()

            steps.onOffersScreen().driveBannerButton.tap()
            app.buttons["Done"].shouldExist()
        }
    }

    // MARK: - Payment Reason

    func test_activationPaymentReason_reasonUnknown() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .reasonUnknown
        })
        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_paymentGroup() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .paymentGroup
        })
        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .exist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_premiumOffer() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .premiumOffer
        })
        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .exist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_region() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .region
        })

        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_userQuota() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .userQuota
        })

        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_sameSale() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .sameSale
        })

        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .exist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_freeLimit() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .freeLimit
        })

        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
    }

    func test_activationPaymentReason_with7DaysOffer() {
        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
            $0.actions.activate = true
        }, withActivationVAS: {
            $0.recommendationPriority = 2
            $0.price = 1000
            $0.originalPrice = 2000
            $0.autoProlongPrice = 500
            $0.prolongationForcedNotTogglable = true
            $0.prolongationAllowed = true
            $0.days = 7
        })

        mainSteps
            .openOffersTab()
            .exist("Платное размещение")
            .validateSnippetSnapShot(offerID: offerID)
    }

    // MARK: - Reseller Warning

    func test_resellerWarning_scrollToFooter() {
        didLoad = true
        let offerID = "1234"
        var lastPage = false

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            let filePath = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json")
            let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
            var response = body.flatMap { try? Auto_Api_OfferListingResponse(jsonUTF8Data: $0) }!
            var offer = response.offers.first!

            response.pagination.totalOffersCount = 5
            response.pagination.totalPageCount = 2
            response.pagination.pageSize = 3
            response.pagination.page = 0
            if lastPage {
                response.pagination.pageSize = 2
                response.pagination.page = 2
                response.offers = (0..<2).map { _ in offer }
            } else {
                lastPage = true
                response.offers = (0..<3).map { _ in offer }
                offer.status = .inactive
                offer.id = offerID
                let activationVASIndex = offer.servicePrices.firstIndex { $0.service == "all_sale_activate" }!
                var activationVAS = offer.servicePrices[activationVASIndex]
                activationVAS.paidReason = .sameSale
                offer.servicePrices[activationVASIndex] = activationVAS
                response.offers[0] = offer
            }
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }

        mainSteps
            .openOffersTab()
            .scrollToResellerWarning()
            .exist("reseller_warning_cell")
            .validateSnapShot(accessibilityId: "reseller_warning_cell")
    }

    func test_resellerWarning_tapSupport() {
        server.addHandler("GET /chat/room/tech-support") { (request, _) -> Response? in
            return Response.okResponse(fileName: "chat_room_techsupport_GET_ok", userAuthorized: false)
        }

        didLoad = true
        let offerID = "1234"

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
        }, withActivationVAS: {
            $0.paidReason = .sameSale
        })

        mainSteps
            .openOffersTab()
            .scrollToResellerWarning()
            .tapSupportOnResselerWarning()
            .should(provider: .chatScreen, .exist)
    }

    func test_resellerWarning_hidden() {
        var lastPage = false

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            let filePath = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json")
            let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
            var response = body.flatMap { try? Auto_Api_OfferListingResponse(jsonUTF8Data: $0) }!
            let offer = response.offers.first!

            response.pagination.totalOffersCount = 5
            response.pagination.totalPageCount = 2
            response.pagination.pageSize = 3
            response.pagination.page = 0
            if lastPage {
                response.pagination.pageSize = 2
                response.pagination.page = 2
                response.offers = (0..<2).map { _ in offer }
            } else {
                lastPage = true
                response.offers = (0..<3).map { _ in offer }
            }
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }

        mainSteps
            .openOffersTab()
            .scrollToResellerWarning()
            .notExist("reseller_warning_cell")
    }

    // MARK: - Offer Activation

    func test_activateFreeOffer() {
        didLoad = true
        let offerID = "1"
        let category = "cars"

        server.addHandler("POST /user/offers/\(category.uppercased())/\(offerID)/activate") { (request, _) -> Response? in
            return Response.responseWithStatus(body: "{\"status\":\"SUCCESS\"}".data(using: .utf8), userAuthorized: false)
        }
        server.addHandler("GET /user/offers/\(category.uppercased())/\(offerID)") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_cars_1_ok", userAuthorized: false)
        }

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
            $0.actions.activate = true
        }, withActivationVAS: {
            $0.price = 0
        })

        mainSteps
            .openOffersTab()
            .notExist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
            .tapActivate()
            .tapDone()
            .notExist("Активировать")
    }

    func test_activatePaymentOffer() {
        didLoad = true
        let offerID = "1"
        let category = "cars"

        server.addHandler("POST /user/offers/\(category.uppercased())/\(offerID)/activate") { (request, _) -> Response? in
            return Response.responseWithStatus(body: "{\"status\":\"SUCCESS\"}".data(using: .utf8), userAuthorized: false)
        }
        server.addHandler("GET /user/offers/\(category.uppercased())/\(offerID)") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_cars_1_ok", userAuthorized: false)
        }

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
            $0.actions.activate = true
        }, withActivationVAS: { _ in
            //
        })

        mainSteps
            .openOffersTab()
            .notExist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
            .tapActivate("Активировать за 99 ₽")
            .tapDone()
            .notExist("Активировать за 99")
    }

    func test_activatePaymentOffer_with7Days() {
        didLoad = true
        let offerID = "1"
        let category = "cars"

        server.addHandler("POST /user/offers/\(category.uppercased())/\(offerID)/activate") { (request, _) -> Response? in
            return Response.responseWithStatus(body: "{\"status\":\"SUCCESS\"}".data(using: .utf8), userAuthorized: false)
        }
        server.addHandler("GET /user/offers/\(category.uppercased())/\(offerID)") { (request, _) -> Response? in
            return Response.okResponse(fileName: "user_offers_cars_1_ok", userAuthorized: false)
        }

        addResponse(withOffer: {
            $0.status = .inactive
            $0.id = offerID
            $0.actions.activate = true
        }, withActivationVAS: {
            $0.prolongationForcedNotTogglable = true
            $0.prolongationAllowed = true
            $0.days = 7
            $0.price = 1000
            $0.originalPrice = 2000
            $0.autoProlongPrice = 500
        })

        mainSteps
            .openOffersTab()
            .notExist("Платное размещение")
            .notExist("reseller_warning_cell")
            .validateSnippetSnapShot(offerID: offerID)
            .tapActivate("Активировать за 1 000 ₽ / 7 дней")
            .tapDone()
            .notExist("Активировать за 1 000 ₽ / 7 дней")
    }

    func test_userOffer_activation_withoutTradeIn() {
        didLoad = true

        mocker
            .mock_activateUserOfferFrom(category: "cars", id: "1")
            .mock_userOffer(id: "1", status: .inactive)
            .mock_tradeInIsAvailable(with: true)
            .mock_tradeInApply()
            .mock_statsPredict()

        addResponse(
            withOffer: {
                $0.status = .inactive
                $0.id = "1"
                $0.actions.activate = true
            },
            withActivationVAS: {
                $0.price = 0
            }
        )

        mainSteps
            .openOffersTab()
            .tapActivate()
            .tapDone()
            .should(provider: .tradeInPicker, .be(.hidden))
    }

    func test_userOffer_shouldBeAddPanoramaBannerInListAndCard() {
        didLoad = true

        var photo = Auto_Api_Photo()
        photo.name = "autoru-vos:4145772-2257914d2ad1a459c871a20dc07bc9be"
        photo.sizes = ["320x240": "//avatars.mds.yandex.net/get-autoru-vos/4145772/2257914d2ad1a459c871a20dc07bc9be/320x240"]

        offerResponse.offer.state.imageUrls = [photo]

        mainSteps
            .openOffersTab()
            .checkAddPanoramaBannerOpened()
            .tapCloseAddPanoramaBanner()
            .checkAddPanoramaBannerClosed()
            .openOffer(offerId: "1")
            .checkScreenLoaded()
            .checkAddPanoramaBannerOpened()
            .tapCloseAddPanoramaBanner()
            .checkAddPanoramaBannerClosed()
    }

    func test_offerListShowDraft() {
        didLoad = true

        mocker
            .mock_userDraft(id: "1")

        mainSteps
            .openOffersTab()
            .checkDraftAppear()
            .checkShortTitle()
            .checkDeleteDraftMenu()
            .checkEditDraftButton()
    }

    // MARK: - 

    private func validateSnapshots(accessibilityId: String, snapshotId: String) {
        let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image

        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 0.02, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16))
    }

    private func addResponse(
        withOffer: @escaping (inout Auto_Api_Offer) -> Void,
        withActivationVAS: @escaping (inout Auto_Api_PaidServicePrice) -> Void) {

        server.addHandler("GET /user/offers/all *") { (request, _) -> Response? in
            let filePath = Bundle.resources.url(forResource: "user_offers_all_ok", withExtension: "json")
            let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
            var response = body.flatMap { try? Auto_Api_OfferListingResponse(jsonUTF8Data: $0) }!
            var offer = response.offers.first!
            withOffer(&offer)
            let activationVASIndex = offer.servicePrices.firstIndex { $0.service == "all_sale_activate" }!
            var activationVAS = offer.servicePrices[activationVASIndex]
            withActivationVAS(&activationVAS)
            offer.servicePrices[activationVASIndex] = activationVAS
            response.offers[0] = offer
            return Response.responseWithStatus(body: try! response.jsonUTF8Data(), userAuthorized: true)
        }
    }
}
