import AutoRuAppearance
@testable import AutoRuCellHelpers
import AutoRuModels
@testable import AutoRuStandaloneCarHistory
import AutoRuYogaLayout
import Foundation
import Snapshots
import AutoRuColorSchema

final class CarReportDiscountBannerLayoutTests: BaseUnitTest {
    func test_standaloneBuy() {
        let layout = CarReportsReportBundleLayoutCreator.make(
            model: CarReportsReportBundleLayoutCreator.Model(
                onBuy: {},
                kind: .buy(
                    CarReportsReportBundleModel.BuyPack(size: "10 отчётов", sizeGen: "10 отчётов", basePrice: "700", perReportPrice: "35", effectivePrice: "350", discountPercent: 50)
                )
            )
        )
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_standaloneExtend() {
        let layout = CarReportsReportBundleLayoutCreator.make(
            model: CarReportsReportBundleLayoutCreator.Model(
                onBuy: {},
                kind: .extend(
                    CarReportsReportBundleModel.ExtendPack(
                        buyInfo: .init(size: "10 отчётов", sizeGen: "10 отчётов", basePrice: "700", perReportPrice: "35", effectivePrice: "350", discountPercent: 50),
                        boughtInfo: .init(reportsLeft: 5)
                    )
                )
            )
        )
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }
}
