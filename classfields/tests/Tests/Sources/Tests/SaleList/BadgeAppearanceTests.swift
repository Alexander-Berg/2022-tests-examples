//
//  BadgeAppearanceTests.swift
//  Tests
//
//  Created by Andrei Iarosh on 19.03.2021.
//

import Foundation
import XCTest
import AutoRuAppConfig
import AutoRuAppearance
import AutoRuProtoModels
import AutoRuYogaLayout
import Snapshots
@testable import AutoRuViews
import AutoRuColorSchema
import CoreGraphics

final class BadgeAppearanceTests: BaseUnitTest {
    func test_callHistoryBadgeSuccess() {
        Step("Проверка отрисовки значка \"Вы звонили\" - успешный звонок")
        let offer = makeOffer("offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        SharedDefaultsHelper.shared.callBadgeEnabled = true

        guard let model = CallHistoryBadgeViewModel.fromSourceLastCalls(offer.sourceLastCalls) else {
            XCTFail("CallHistoryBadgeViewModel shouldn't be nil")
            return
        }
        let layout = CallHistoryBadgeLayout(model: model)

        Snapshot.compareWithSnapshot(
            layout: StackWithBackgroundLayout(childs: [layout], configNode: nil, configView: nil),
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_callHistoryBadgeSuccessCollapsed() {
        Step("Проверка отрисовки значка \"Вы звонили\" - успешный звонок в свернутом виде")
        let offer = makeOffer("offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        SharedDefaultsHelper.shared.callBadgeEnabled = true

        guard var model = CallHistoryBadgeViewModel.fromSourceLastCalls(offer.sourceLastCalls) else {
            XCTFail("CallHistoryBadgeViewModel shouldn't be nil")
            return
        }

        model.shouldAutoCollapse = true
        let layout = CallHistoryBadgeLayout(model: model)
        let stack = StackWithBackgroundLayout(childs: [layout], configNode: nil, configView: nil)
        let creator = BasicViewHierarchyCreator(
            rootComponent: stack.getLayout(),
            boundingSize: CGSize(width: CGFloat.nan, height: .nan)
        )

        let view = creator.createView()
        view.backgroundColor = ColorSchema.Background.surface

        asyncAfter(model.collapseTimeout + 1) {
            Snapshot.compareWithSnapshot(view: view)
        }
    }

    func test_callHistoryBadgeFailed() {
        Step("Проверка отрисовки значка \"Вы звонили\" - неудачный звонок")
        let offer = makeOffer("offer_CARS_1093580048-0872915d_with_source_last_calls_failed")
        SharedDefaultsHelper.shared.callBadgeEnabled = true

        guard let model = CallHistoryBadgeViewModel.fromSourceLastCalls(offer.sourceLastCalls) else {
            XCTFail("CallHistoryBadgeViewModel shouldn't be nil")
            return
        }
        let layout = CallHistoryBadgeLayout(model: model)

        Snapshot.compareWithSnapshot(
            layout: StackWithBackgroundLayout(childs: [layout], configNode: nil, configView: nil),
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_callHistoryBadgeFailedCollapsed() {
        Step("Проверка отрисовки значка \"Вы звонили\" - неудачный звонок в свернутом виде")
        let offer = makeOffer("offer_CARS_1093580048-0872915d_with_source_last_calls_failed")
        SharedDefaultsHelper.shared.callBadgeEnabled = true

        guard var model = CallHistoryBadgeViewModel.fromSourceLastCalls(offer.sourceLastCalls) else {
            XCTFail("CallHistoryBadgeViewModel shouldn't be nil")
            return
        }
        model.shouldAutoCollapse = true
        let layout = CallHistoryBadgeLayout(model: model)
        let stack = StackWithBackgroundLayout(childs: [layout], configNode: nil, configView: nil)
        let creator = BasicViewHierarchyCreator(
            rootComponent: stack.getLayout(),
            boundingSize: CGSize(width: CGFloat.nan, height: .nan)
        )

        let view = creator.createView()
        view.backgroundColor = ColorSchema.Background.surface

        asyncAfter(model.collapseTimeout + 1) {
            Snapshot.compareWithSnapshot(view: view)
        }
    }

    // MARK: - Private

    private func makeOffer(_ fileName: String) -> Auto_Api_Offer {
        let url = Bundle.current.url(forResource: fileName, withExtension: "json")
        XCTAssertNotNil(url, "File \(fileName).json doesn't exists in the bundle")
        let response = try! Auto_Api_OfferResponse(jsonUTF8Data: Data(contentsOf: url!))
        return response.offer
    }
}
