//
//  DealerCardTest.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 16.07.2020.
//

import Foundation
import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuDealerCard
final class DealerCardTests: BaseTest {
    enum Const {
        static var offerId = "1099385564-a470b464"
    }

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func setupServer() {
        let forceLogin = false
        server.forceLoginMode = .preservingResponseState
        server.addHandler("GET /salon/lada_tula") { (_, _) -> Response? in
            return Response.okResponse(fileName: "lada_tula", userAuthorized: forceLogin)
        }

        server.addHandler("GET /salon/lada_tula/phones *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "lada_tula_phone", userAuthorized: forceLogin)
        }

        basicMockReproducer.setup(server: server, mockFolderName: "DealerCard", userAuthorized: false)

        try! server.start()
    }

    func test_call() {
        launchAndGoToDealerCard()
            .tapCallButton()
            .alertExist()
    }

    func test_openMap() {
        launchAndGoToDealerCard()
            .tapMap()
            .exist(selector: "FullscreenMapViewController")
    }

    func test_callBack() {
        launchAndGoToDealerCard()
            .tapCallBackButton()
            .exist(selector: "PhoneInputPickerViewController")
    }

    func test_titleViewShoudExistWhenScroll() {
        launchAndGoToDealerCard()
            .swipeUp()
            .navBarTitleVisible()
    }

    func test_navBarCallButtonShoudExistWhenScrollUnderCallButton() {
        launchAndGoToDealerCard()
            .swipeUp()
            .navBarCallButtonVisible()
    }

    func test_filtersButtonShoudExistWhenScrollDownUnderLightForm() {
        launch()
        launchAndGoToDealerCard()
            .swipeUp()
            .swipeUp()
            .swipeUp()
            .swipeUp()
            .swipeDown()
            .filtersButtonVisible()
    }

    func test_complain() {
        server.addHandler("GET /user/offers/CARS/\(Const.offerId)") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offer_active_no_images", userAuthorized: true)
        }

        server.addMessageHandler("POST /offer/cars/\(Const.offerId)/complaints") { _, _ in
            Auto_Api_SuccessResponse.with { response in
                response.status = .success
            }
        }

        let complainExpectation = expectationForRequest { req -> Bool in
            guard let data = req.messageBody,
                  let model = try? Auto_Api_ComplaintRequest(jsonUTF8Data: data),
                  model.placement == "serp_offers_item",
                  req.uri.lowercased().range(
                    of: "/offer/cars/.*/complaints",
                    options: .regularExpression
                  ) != nil,
                  req.method == "POST"
            else { return false }

            return true
        }

        launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(Const.offerId)")))
        SaleCardSteps(context: self)
            .scrollTo("DealerInfoLayout")
            .dealerNameTap()
            .dealerOfferCountTap()
            .wait(for: 2)
            .scroll(to: .moreButton)
            .tap(.moreButton)
            .tap(.complainButton)
            .tap(.saledButton)

        wait(for: [complainExpectation], timeout: 10)
    }

    func test_saveAndRemoveSearchFromNavBar() {
        let addSubscriptionExpectation = XCTestExpectation()
        server.addHandler("POST /user/favorites/cars/subscriptions") { request, _ -> Response? in
            addSubscriptionExpectation.fulfill()
            return Response.okResponse(fileName: "lada_tula_save_search", userAuthorized: false)
        }

        let deleteSubscriptionExpectation = XCTestExpectation()
        server.addHandler("DELETE /user/favorites/all/subscriptions/1195e68a1f44d4cd0c191c45a8028cb86aadbb9b") { request, _ -> Response? in
            deleteSubscriptionExpectation.fulfill()
            return Response.okResponse(fileName: "success")
        }

        launchAndGoToDealerCard()
            .saveNavBarButtonHasUnsavedState()
            .tapSaveNavBarButton()
            .saveNavBarButtonHasSavingState()
            .wait(for: 1)
            .saveNavBarButtonHasSavedState()
            .tapSaveNavBarButton()
            .saveNavBarButtonHasRemovingState()
            .wait(for: 1)
            .saveNavBarButtonHasUnsavedState()

        wait(for: [addSubscriptionExpectation, deleteSubscriptionExpectation], timeout: 0)
    }

    func launchAndGoToDealerCard() -> DealerCardSteps {
        launch(options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(Const.offerId)")))
        return SaleCardSteps(context: self)
            .scrollTo("DealerInfoLayout", windowInsets: .init(top: 0, left: 0, bottom: 100, right: 0))
            .dealerNameTap()
            .dealerOfferCountTap()
            .wait(for: 2)
            .validateSnapShot(accessibilityId: "DealerCardViewController", snapshotId: "DealerCardViewController", message: "Общий вид карточки")
    }
}
