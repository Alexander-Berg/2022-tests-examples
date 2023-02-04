import AutoRuProtoModels
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuGarageCard

final class GarageMagazineCellTests: BaseUnitTest {

    override func setUp() {
        super.setUp()
        setReplaceImagesWithStub("audi_snippet_stub")
        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()
        setReplaceImagesDefaultBehavior()
        FetchableImage.blockThreadUntilFinished = false
    }

    func test_MagazineCell() throws {
        let magazineMock: Auto_Api_Vin_Garage_Magazine = try .init(jsonString: Self.magazineJsonMock)
        let layout = GarageMagazineCell(model: magazineMock,
                                        showMagazine: {},
                                        showMagazineArticle: {})

        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}

private extension GarageMagazineCellTests {
    static let magazineJsonMock =
    """
        {
          "article": {
            "autoInfo": [
              {
                "markInfo": {
                  "code": "MERCEDES",
                  "name": "Mercedes-Benz",
                  "ruName": "Мерседес-Бенц",
                  "logo": {
                    "name": "mark-logo",
                    "sizes": {
                      "logo": "//avatars.mds.yandex.net/get-verba/997355/2a00000179b3dd6837b829e20043c2907396/logo",
                      "black-logo": "//avatars.mds.yandex.net/get-verba/787013/2a0000017a7fdc6287ed38e9ce3c59ff28d5/logo",
                      "big-logo": "//avatars.mds.yandex.net/get-verba/997355/2a00000179b3dd6837b829e20043c2907396/dealer_logo"
                    }
                  },
                  "numericId": "3172"
                },
                "modelInfo": {
                  "code": "G_KLASSE",
                  "name": "G-Класс",
                  "ruName": "G-класс"
                }
              }
            ],
            "mainPhoto": {
              "sizes": {
                "wide": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide",
                "mobile": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/mobile",
                "wide@3": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide@3",
                "wide@2": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide@2",
                "4x3@3": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3@3",
                "desktop": "https://autoru-mag.s3.yandex.net/2021/05/26/08a408fa2a1744b4b11a7080082cc9e1.jpg/desktop",
                "4x3": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3",
                "4x3@1.5": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3@1.5",
                "4x3@2": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/4x3@2",
                "wide@1.5": "https://autoru-mag.s3.yandex.net/2021/05/26/4efb1d2ceb154636bbefd9e4cd3d1c70.jpg/wide@1.5"
              }
            },
            "publishTime": "2021-05-30T12:45:41Z",
            "categories": [
              "Разбор",
              "Внедорожники",
              "Дневник трат"
            ],
            "articleUrl": "https://mag.auto.ru/article/gclassdiary/",
            "text": "О машине Марка и модель: Mercedes-Benz G350 BlueTec Год выпуска: 2015 Пробег: 112 000 км Город: Нижний Новгород   О владельце Возраст: 48 лет Стаж вождения: 28 лет Род занятий: производство торгово-холодильного оборудования   Покупка мечты Мечты должны сбываться. Неважно,",
            "title": "Во сколько обходится содержание Mercedes-Benz G-класса: дневник трат"
          }
        }
    """
}
