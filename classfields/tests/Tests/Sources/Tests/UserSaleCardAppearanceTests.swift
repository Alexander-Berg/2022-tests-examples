import XCTest
import AutoRuAppearance
import AutoRuYogaLayout
import AutoRuCellHelpers
import AutoRuModels
import AutoRuFormatters
import AutoRuYoga
import AutoRuUserSaleSharedUI
import Snapshots
@testable import AutoRuUserSaleCard
import AutoRuColorSchema

final class UserSaleCardAppearanceTests: BaseUnitTest {
    func test_promoBuyOut() {
        let model = PromoBuyOutCellHelperModel(
            viewModel: PromoBuyOutViewModel(title: "Тайтл", subtitle: "сабтайтл", phone: "+79991112233"),
            onDetailsTap: { },
            onCallTap: { }
        )
        let cellHelper = PromoBuyOutCellHelper(model: model)

        Snapshot.compareWithSnapshot(
            cellHelper: cellHelper,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_garagePromoBanner() {
        let garageBanner = GaragePromoBanner(onTap: {}, onClose: {})

        Snapshot.compareWithSnapshot(
            layout: garageBanner.getLayout(),
            maxWidth: DeviceWidth.iPhone11
        )
    }
}
