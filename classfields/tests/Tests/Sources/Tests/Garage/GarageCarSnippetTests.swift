import XCTest
import Snapshots
@testable import AutoRuGarageListing

final class GarageCarSnippetTests: BaseUnitTest {
    func test_currentCar() {
        let model: GarageListingViewModel.CarInfo = .init(
            id: "123",
            title: "Test car snippet",
            vinOrGovNumber: "a123aa777",
            markLogo: .testImage(
                withFixedSize: .init(width: 50, height: 50)
            ),
            recalls: .init(title: "recall test title", value: "recall test value"),
            price: "100 000 000",
            discounts: "10 предложений",
            offersCount: 0,
            carType: .currentCar
        )

        let layout = GarageCardListTableModelBuilder().addCarBlock(
            index: 0,
            model: model,
            onTap: {},
            onCopy: nil
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_dreamCar() {
        let model: GarageListingViewModel.CarInfo = .init(
            id: "123",
            title: "Test car snippet",
            vinOrGovNumber: "",
            markLogo: .testImage(
                withFixedSize: .init(width: 50, height: 50)
            ),
            recalls: .init(title: "recall test title", value: "recall test value"),
            price: "100 000 000",
            discounts: "10 предложений",
            offersCount: 100,
            carType: .dreamCar
        )

        let layout = GarageCardListTableModelBuilder().addCarBlock(
            index: 0,
            model: model,
            onTap: {},
            onCopy: nil
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_ex_car() {
        let model: GarageListingViewModel.CarInfo = .init(
            id: "123",
            title: "Test car snippet",
            vinOrGovNumber: "",
            markLogo: .testImage(
                withFixedSize: .init(width: 50, height: 50)
            ),
            recalls: .init(title: "recall test title", value: "recall test value"),
            price: "100 000 000",
            discounts: "10 предложений",
            offersCount: 100,
            carType: .exCar
        )

        let layout = GarageCardListTableModelBuilder().addCarBlock(
            index: 0,
            model: model,
            onTap: {},
            onCopy: nil
        )

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
