import XCTest
import AutoRuProtoModels
import AutoRuCellHelpers
import Snapshots
import AutoRuColorSchema
import AutoRuTableController
@testable import AutoRuOfferAdvantage

final class OfferAdvantagesTests: BaseUnitTest {

    // Онлайн-показ
    func test_onlineViewAdvantage() {
        Self.snapshotSingleAdvatage(.onlineView)
    }

    // Продаёт собственник
    func test_provenOwnerAdvantage() {
        Self.snapshotSingleAdvatage(.provenOwner)
    }

    // ДТП не найдены
    func test_noAccidentsAdvantage() {
        Self.snapshotSingleAdvatage(.noAccidents)
    }

    // 1 владелец
    func test_oneOwnerAdvantage() {
        Self.snapshotSingleAdvatage(.oneOwner)
    }

    // Проверенный автомобиль
    func test_certificateManufacturerAdvantage() {
        Self.snapshotSingleAdvatage(.certificateManufacturer)
    }

    // На гарантии
    func test_warrantyAdvantage() {
        Self.snapshotSingleAdvatage(.warranty)
    }

    // Почти как новый
    func test_almostNewAdvantage() {
        Self.snapshotSingleAdvatage(.almostNew)
    }

    // Отличная модель
    func test_highReviewMarkAdvantage() {
        Self.snapshotSingleAdvatage(.highReviewMark)
    }

    // Стабильная цена
    func test_stablePriceFivePercentAdvantage() {
        Self.snapshotSingleAdvatage(.stablePrice, state: "fivePercent") { offer in
            offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 5
        }
    }

    // Медленно теряет в цене
    func test_stablePriceFourPercentAdvantage() {
        Self.snapshotSingleAdvatage(.stablePrice, state: "fourPercent") { offer in
            offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 4
        }
    }

    // Электромобили
    func test_electrocarAdvantage() {
        Self.snapshotSingleAdvatage(.electrocar)
    }

    // Проверенный собственник неактивный
    func test_provenOwnerInactiveAdvantage() {
        Self.snapshotSingleAdvatage(.provenOwnerInactive)
    }

    // Проверка в группе по 2 шт
    func test_onlineView_provenOwner_Advantages() {
        Self.snapshotMultiAdvatages([.onlineView, .provenOwner])
    }

    func test_noAccidents_oneOwner_Advantages() {
        Self.snapshotMultiAdvatages([.noAccidents, .oneOwner])
    }

    func test_certificateManufacturer_warranty_Advantages() {
        Self.snapshotMultiAdvatages([.certificateManufacturer, .warranty])
    }

    func test_almostNew_highReviewMark_Advantages() {
        Self.snapshotMultiAdvatages([.almostNew, .highReviewMark])
    }

    func test_stablePriceFivePercent_electrocar_Advantages() {
        Self.snapshotMultiAdvatages([.stablePrice, .electrocar]) { offer in
            offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 5
        }
    }

    func test_stablePriceFourPercent_provenOwnerInactive_Advantages() {
        Self.snapshotMultiAdvatages([.stablePrice, .provenOwnerInactive]) { offer in
            offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 4
        }
    }

    // Проверка в группе по 3 шт
    func test_onlineView_provenOwner_noAccidents_Advantages() {
        Self.snapshotMultiAdvatages([.onlineView, .provenOwner, .noAccidents])
    }

    func test_oneOwner_certificateManufacturer_warranty_Advantages() {
        Self.snapshotMultiAdvatages([.oneOwner, .certificateManufacturer, .warranty])
    }

    func test_almostNew_highReviewMark_stablePriceFivePercent_Advantages() {
        Self.snapshotMultiAdvatages([.almostNew, .highReviewMark, .stablePrice]) { offer in
            offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 5
        }
    }

    func test_stablePriceFourPercent_electrocar_provenOwnerInactive_Advantages() {
        Self.snapshotMultiAdvatages([.stablePrice, .electrocar, .provenOwnerInactive], forShrinkedLayout: true) { offer in
            offer.additionalInfo.priceStats.lastYearPricePercentageDiff = 4
        }
    }

    private static func snapshotSingleAdvatage(
        _ advantage: OfferAdvantage,
        state: String? = nil,
        mutateOffer: ((inout Auto_Api_Offer) -> Void)? = nil
    ) {
        var offer = Auto_Api_Offer()
        mutateOffer?(&offer)
        let single = OfferAdvantagesCellHelper(
            model: OfferAdvantagesCellHelperModel(
                advantages: [
                    OfferAdvantageBadgeModel(
                        kind: .plain(advantage),
                        title: advantage.title(for: offer),
                        description: advantage.description(for: offer),
                        titleSuffix: advantage.titleSuffix(for: offer)
                    )
                ],
                actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
            )
        )
        Snapshot.compareWithSnapshot(
            cellHelper: single,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "\(advantage.analyticsName)\(state != nil ? "_\(state ?? "")" : "")_advantage"
        )
    }

    private static func snapshotMultiAdvatages(
        _ advantages: [OfferAdvantage],
        forShrinkedLayout: Bool = false,
        mutateOffer: ((inout Auto_Api_Offer) -> Void)? = nil
    ) {
        var offer = Auto_Api_Offer()
        mutateOffer?(&offer)

        var advantagesName = String()
        let advantagseModels = advantages.map { adv -> OfferAdvantageBadgeModel in
            advantagesName.append(contentsOf: adv.analyticsName + "_")
            return .init(
                kind: .plain(adv),
                title: adv.title(for: offer, forShrinkedLayout: forShrinkedLayout),
                description: adv.description(for: offer),
                titleSuffix: adv.titleSuffix(for: offer)
            )
        }
        let model = OfferAdvantagesCellHelperModel(
            advantages: advantagseModels,
            actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
        )

        if advantagseModels.count > 2 {
            let cellHelper = OfferAdvantageListCellHelper(model: model)
            cellHelper.precalculateLayout(indexPath: IndexPath(item: 0, section: 0), width: 460)
            let view = cellHelper.createCellView(width: 460)
            view.backgroundColor = ColorSchema.Background.surface

            Snapshot.compareWithSnapshot(view: view, identifier: "\(advantagesName)advantages")
        } else {
            let cellHelper = OfferAdvantagesCellHelper(model: model)
            Snapshot.compareWithSnapshot(
                cellHelper: cellHelper,
                maxWidth: DeviceWidth.iPhone11,
                backgroundColor: ColorSchema.Background.surface,
                identifier: "\(advantagesName)advantages"
            )
        }
    }
}
