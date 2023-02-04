import XCTest
import AutoRuProtoModels
import AutoRuTableController
import SwiftProtobuf
import Snapshots
@testable import AutoRuOfferAdvantage

final class OfferAdvantageScoreBadgeTests: BaseUnitTest {
    func test_scoreLow() {
        let view = Self.makeScoreAdvantageView(score: 1, isOwner: false)

        XCTContext.runActivity(named: "Проверяем для скора 1 скриншот") { _ in
            Snapshot.compareWithSnapshot(view: view, interfaceStyle: [.light])
        }
    }

    func test_scoreMedium() {
        let view = Self.makeScoreAdvantageView(score: 4, isOwner: false)

        XCTContext.runActivity(named: "Проверяем для скора 4 скриншот") { _ in
            Snapshot.compareWithSnapshot(view: view, interfaceStyle: [.light])
        }
    }

    func test_scoreHigh() {
        let view = Self.makeScoreAdvantageView(score: 7, isOwner: false)

        XCTContext.runActivity(named: "Проверяем для скора 7 скриншот") { _ in
            Snapshot.compareWithSnapshot(view: view, interfaceStyle: [.light])
        }
    }

    func test_scoreLow_userOffer() {
        let view = Self.makeScoreAdvantageView(score: 1, isOwner: true)

        XCTContext.runActivity(named: "Проверяем для скора 1 скриншот") { _ in
            Snapshot.compareWithSnapshot(view: view, interfaceStyle: [.light])
        }
    }

    func test_scoreMedium_userOffer() {
        let view = Self.makeScoreAdvantageView(score: 4, isOwner: true)

        XCTContext.runActivity(named: "Проверяем для скора 4 скриншот") { _ in
            Snapshot.compareWithSnapshot(view: view, interfaceStyle: [.light])
        }
    }

    func test_scoreHigh_userOffer() {
        let view = Self.makeScoreAdvantageView(score: 7, isOwner: true)

        XCTContext.runActivity(named: "Проверяем для скора 7 скриншот") { _ in
            Snapshot.compareWithSnapshot(view: view, interfaceStyle: [.light])
        }
    }

    func test_twoBadges_userOffer() {
        let offer = Auto_Api_Offer.with { offer in
            offer.tags = ["no_accidents"]
            offer.score = Auto_Api_Score.with { scoreModel in
                scoreModel.transparency = Google_Protobuf_FloatValue(40.0)
            }
        }

        let advantages = OfferAdvantage.advantagesForOffer(offer: offer, isOwner: true)

        // Несколько
        let multiple = OfferAdvantagesCellHelper(
            model: OfferAdvantagesCellHelperModel(
                advantages: advantages,
                actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
            )
        )

        Snapshot.compareWithSnapshot(
            cellHelper: multiple,
            maxWidth: DeviceWidth.iPhone11,
            maxHeight: 162,
            backgroundColor: .white,
            interfaceStyle: [.light]
        )
    }

    func test_scoreRoundup() {
        var score = Auto_Api_Score()
        score.transparency = 77.0
        XCTAssertEqual(score.normalizedTransparency, 8)
        score.transparency = 74.0
        XCTAssertEqual(score.normalizedTransparency, 8)
    }

    // MARK: - Private

    private static func makeScoreAdvantageView(score: Int, isOwner: Bool) -> UIView {
        let offer = Auto_Api_Offer.with { offer in
            offer.score = Auto_Api_Score.with { scoreModel in
                scoreModel.transparency = Google_Protobuf_FloatValue(Float(score) * 10.0)
            }
        }

        let advantages = OfferAdvantage.advantagesForOffer(offer: offer, isOwner: isOwner)

        let model = OfferAdvantagesCellHelperModel(
            advantages: advantages,
            actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
        )
        let view = OfferAdvantagesCellHelper(model: model).createCellView(width: DeviceWidth.iPhone11)
        view.backgroundColor = .white

        return view
    }
}
