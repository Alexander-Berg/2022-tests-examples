import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuCellHelpers
import Snapshots
@testable import AutoRuGarageCard
import Foundation
import AutoRuAppConfig

final class GarageCardHeaderCellTests: BaseUnitTest {
    override func setUp() {
        super.setUp()
        SharedDefaultsHelper.shared.dreamCarEstimateEnabled = true
    }

    func test_GarageCardHeaderWithRecalls() throws {
        let modelStub = try createModelStub { card in
            card.recalls = AutoRuProtoModels.Auto_Api_Vin_Garage_Recalls()
            card.recalls.card = Auto_Api_Recalls_Card()
            card.recalls.card.recalls = [Auto_Api_Recalls_Recall()]
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageCardHeaderWithoutRecalls() throws {
        let modelStub = try createModelStub { card in
            card.recalls = AutoRuProtoModels.Auto_Api_Vin_Garage_Recalls()
            card.recalls.card = Auto_Api_Recalls_Card()
            card.recalls.card.recalls = []
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageCardHeaderWithAddVINCard() throws {
        let modelStub = try createModelStub { card in
            card.recalls = AutoRuProtoModels.Auto_Api_Vin_Garage_Recalls()
            card.recalls.card = Auto_Api_Recalls_Card()
            card.sourceInfo.manuallyAdded = true
            card.vehicleInfo.documents.vin = ""
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageCardHeaderWithOffer() throws {
        let modelStub = try createModelStub(
            cardOffer: .init(id: "123", price: 10_000_000, isActive: true)
        )

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageCardHeaderProvenOwnerOK() throws {
        let modelStub = try createModelStub { card in
            card.provenOwnerState.status = .ok
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageCardHeaderProvenOwnerPanding() throws {
        let modelStub = try createModelStub { card in
            card.provenOwnerState.status = .pending
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageCardHeaderProvenOwnerUnknown() throws {
        let modelStub = try createModelStub { card in
            card.provenOwnerState.status = .unknown
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageDreamCardHeader() throws {
        let modelStub = try createModelStub(
            creditMonthlyPayment: 10_000
        ) { card in
            card.credit.suggestedCreditAmount = 1_000_000
            card.listingOffers.listingOffersCount = 100
            card.pricePredict.averagePrice = 1_000_000
            card.cardTypeInfo.cardType = .dreamCar
        }

        checkLayoutSnapshot(model: modelStub)
    }

    func test_GarageExCardHeader() throws {
        let modelStub = try createModelStub() { card in
            card.cardTypeInfo.cardType = .exCar
        }

        checkLayoutSnapshot(model: modelStub)
    }
}

private extension GarageCardHeaderCellTests {
    func checkLayoutSnapshot(
        model: GarageCardViewModel,
        identifier: String = #function
    ) {
        let layout = GarageCardHeaderCell(
            model: model,
            actions: [:]
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            identifier: identifier
        )
    }

    func createModelStub(
        cardOffer: GarageCardViewModel.CardOffer? = nil,
        creditMonthlyPayment: Int? = nil,
        _ cardMutation: (inout Auto_Api_Vin_Garage_Card) -> Void = { _ in }
    ) throws -> GarageCardViewModel {
        let fileURL = try XCTUnwrap(Bundle.current.url(forResource: "GaragePriceStatsGraphCell", withExtension: "json"))
        let data = try Data(contentsOf: fileURL)

        var card = try Auto_Api_Vin_Garage_GetCardResponse(jsonUTF8Data: data).card
        card.priceStats.priceDistribution = .with { msg in
            msg.predictedPrice = 1000
            msg.showSegments = [.init()]
        }
        card.tax.tax = 1000
        card.tax.year = 2021
        cardMutation(&card)

        let feedStub = FeedModel<TableModelCell>(
            identifier: "",
            tableCells: [TableModelCell(manuallyAddToBuilder: { _ in })],
            feedSize: 10,
            totalFeedSize: 100,
            error: nil
        )

        let modelStub = GarageCardViewModel(
            cardSnippets: [],
            selectedSnippetIndex: nil,
            indexToScroll: nil,
            cardType: try XCTUnwrap(.init(apiModel: card.cardTypeInfo.cardType)),
            cardOffer: cardOffer,
            isSharedView: false,
            sharedViewButtonMode: .hidden,
            features: GarageCardViewModel.Features(
                model: Auto_Api_FeaturesResponse(),
                selectedIndex: 0,
                isExpanded: false,
                ratingFromReviews: 5,
                reviewsCount: 100
            ),
            headerPromos: [],
            promos: [],
            canLoadMorePromos: false,
            expandedCells: [],
            error: nil,
            recallsSubscription: nil,
            title: "test",
            garageCard: card,
            provenOwnerState: .unverified,
            showLoader: false,
            feed: feedStub,
            insuranceInfo: .init(),
            offers: [],
            creditMonthlyPayment: creditMonthlyPayment,
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
}
