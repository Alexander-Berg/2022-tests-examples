//
//  OffersDriveBannerTests.swift
//  UITests
//
//  Created by Alexander Malnev on 2/3/21.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuUserSaleCard AutoRuReviewEditor AutoRuOfferAdvantage
final class OffersDriveBannerTests: BaseTest {

    private var userProfile: Auto_Api_UserResponse = {
        var profile = Auto_Api_UserResponse()
        profile.user.id = "1"
        profile.user.profile.autoru.about = ""
        return profile
    }()

    private var didLoad: Bool = false

    lazy var mainSteps = MainSteps(context: self)

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["DRIVE_HIDE_KEY"] = "drive_sale_card_hide"
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

    func test_canHideDriveBanners() {
        server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "user_sale_list_1_active")
        }

        let steps = mainSteps.openOffersTab()

        Step("Баннер должен скрыться") {
            let scroller = steps.onOffersScreen().scrollableElement
            scroller.swipeUp()
            scroller.swipeUp()

            steps.onOffersScreen().driveBannerCloseIcon.tap()
            steps.onOffersScreen().driveBanner.shouldNotExist()
        }
    }
}
