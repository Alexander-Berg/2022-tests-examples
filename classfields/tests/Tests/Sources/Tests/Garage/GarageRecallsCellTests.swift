//
//  GarageReviewsCellTests.swift
//  Tests
//
//  Created by Igor Shamrin on 23.07.2021.
//

import XCTest
import AutoRuProtoModels
import Snapshots
@testable import AutoRuGarageCard

final class GarageRecallsCellTests: BaseUnitTest {
    func test_emptyRecalls() throws {
        // arrange
        let modelStub = createModelStub(Self.emptyRecallsJson)

        let layout = GarageRecallsCell(recalls: modelStub.garageCard.recalls.card.recalls,
                                       recallsSubscription: try XCTUnwrap(modelStub.recallsSubscription),
                                       canSubscribe: true,
                                       onTapAction: { _, _ in },
                                       learnMoreAction: { _ in },
                                       updateRecallSubscription: {})
        // assert
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_recalls() throws {
        // arrange
        let modelStub = createModelStub(Self.recallsJson)

        let layout = GarageRecallsCell(recalls: modelStub.garageCard.recalls.card.recalls,
                                       recallsSubscription: try XCTUnwrap(modelStub.recallsSubscription),
                                       canSubscribe: true,
                                       onTapAction: { _, _ in },
                                       learnMoreAction: { _ in },
                                       updateRecallSubscription: {})
        // assert
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}

private extension GarageRecallsCellTests {
    func createModelStub(_ recallsJsonMock: String) -> GarageCardViewModel {

        var card = Auto_Api_Vin_Garage_Card()
        card.recalls = try! .init(jsonString: recallsJsonMock)

        let modelStub = GarageCardViewModel(
            cardSnippets: [],
            selectedSnippetIndex: nil,
            indexToScroll: nil,
            cardType: .currentCar,
            cardOffer: nil,
            isSharedView: false,
            sharedViewButtonMode: .hidden,
            features: nil,
            headerPromos: [],
            promos: [],
            canLoadMorePromos: false,
            expandedCells: [],
            error: nil,
            recallsSubscription: .init(isOn: true, isEnabled: true),
            title: "test",
            garageCard: card,
            provenOwnerState: .unverified,
            showLoader: false,
            feed: nil,
            insuranceInfo: .init(),
            offers: [],
            creditMonthlyPayment: nil,
            creditApplicationExist: false,
            isSberExclusive: false,
            taxInfo: .init(
                apiTax: card.tax,
                regionInfo: nil,
                modification: card.vehicleInfo.carInfo
            ),
            estimatedPrice: 1_000_000,
            tradeInPrice: 1_000_000,
            targetPromo: nil,
            cellIdToScroll: nil,
            isLoading: false,
            canReload: false,
            carReportModel: nil
        )

        return modelStub
    }

    static let emptyRecallsJson =
    """
        {
        "card": {
            "cardId": "2846436",
            "vinCodeId": "6300277",
            "title": "Great Wall Hover H5",
            "vinCodeMasked": "Z8P**************",
            "mark": "GREAT_WALL",
            "year": 2013,
            "engine": "2.4 л. / 126 л.с.",
            "color": "Серый Светлый",
            "markLogo": "//avatars.mds.yandex.net/get-verba/997355/2a00000179b2e612d87a399edac5fd3afb2e/dealer_logo",
            "isSubscribed": true,
            "created": "2021-07-07T14:41:57.707Z",
            "reportLabel": "Содержит 8 (или больше) записей из 55 источников"
        }
        }
    """

    static let recallsJson =
    """
      {"card": {
        "card_id": "4338",
        "vin_code_id": "438781",
        "title": "Audi A5",
        "vin_code_masked": "WAU**************",
        "mark": "AUDI",
        "year": 2011,
        "engine": "2.0 л. / 211 л.с.",
        "color": "СЕРЕБРИСТЫЙ",
        "recalls": [
          {
            "recall_id": "1635",
            "campaign_id": "321",
            "title": "Проникновение влаги в электронные компоненты",
            "description": "На автомобилях Audi с двигателями 2,0 TFSI, выпущенных в определенный период, возможно проникновение влаги в электронные компоненты. Это может привести к перегреву и выходу из строя дополнительного насоса охлаждающей жидкости. Как следствие не исключены локальные оплавления",
            "url": "https://www.gost.ru/portal/gost/home/presscenter/news?portal:isSecure=true&navigationalstate=JBPNS_rO0ABXczAAZhY3Rpb24AAAABAA5zaW5nbGVOZXdzVmlldwACaWQAAAABAAQ1NTg4AAdfX0VPRl9f&portal:componentId=88beae40-0e16-414c-b176-d0ab5de82e16",
            "published": "2018-12-11T21:00:00Z",
            "is_resolved": true
          },
          {
            "recall_id": "44",
            "campaign_id": "38",
            "title": "Посторонние частицы в охлаждающей жидкости",
            "description": "Причиной отзыва автомобилей марки Audi является возможное появление посторонних частиц в охлаждающей жидкости, которые могут привести к блокировке дополнительного насоса охлаждающей жидкости. Как следствие, возможен перегрев насоса.",
            "url": "http://old.gost.ru/wps/portal/pages/news/?article_id=6201",
            "published": "2017-04-10T21:00:00Z"
          }
        ],
        "mark_logo": "//avatars.mds.yandex.net/get-verba/997355/2a000001651f4baeb0ee7d9d292ce0db5e9a/dealer_logo",
        "is_subscribed": true,
        "created": "2021-05-24T14:09:53Z",
        "report_label": "Содержит 8 (или больше) записей из 44 источника"
      }}
    """
}
