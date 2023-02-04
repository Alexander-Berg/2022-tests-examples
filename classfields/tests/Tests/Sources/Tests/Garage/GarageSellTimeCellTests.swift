@testable import AutoRuGarageCard
import AutoRuProtoModels
import Snapshots

final class GarageSellTimeTests: BaseUnitTest {

    func test_SellTimeCell() {
        var sellTimeModelStub = Auto_Api_Vin_Garage_Card().sellTime
        sellTimeModelStub.vasSellTimeDays = 10

        let layout = SellTimeCell(sellTimeModel: sellTimeModelStub, canSell: true) { _ in }
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_garageSellTimePublicCell() {
        Step("Стоимость без кнопки \"Продать\" на публичной карточке") { }

        var sellTimeModelStub = Auto_Api_Vin_Garage_Card().sellTime
        sellTimeModelStub.vasSellTimeDays = 10

        let layout = SellTimeCell(sellTimeModel: sellTimeModelStub, canSell: false) { _ in }
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
