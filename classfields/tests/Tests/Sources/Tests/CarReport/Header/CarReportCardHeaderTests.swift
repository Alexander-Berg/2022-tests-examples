import AutoRuAppearance
import AutoRuBackendLayout
import AutoRuProtoModels
import AutoRuStandaloneCarHistory
import AutoRuModels
import AutoRuYogaLayout
import SwiftProtobuf
import AutoRuFormatters
import Snapshots
import XCTest
import CoreGraphics

/// Оглавление отчёта
final class CarReportCardHeaderTests: BaseUnitTest, CarReportCardBlockTest {
    private static let headerNodeLayoutSize = CGSize(width: 256, height: CGFloat.nan)

    func test_pts() {
        Step("Данные из ПТС; статус `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusOK")
        }

        Step("Данные из ПТС; статус `ERROR`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusERROR")
        }

        Step("Данные из ПТС; статус `UNKNOWN` / любой другой") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusUNKNOWN")
        }

        Step("Лоадер ещё не готов") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_notReady")
        }
    }

    func test_constraints() {
        Step("Ограничения; статус `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusOK")
        }

        Step("Ограничения; статус `ERROR` / `INVALID`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusERROR")
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusINVALID")
        }

        Step("Ограничения; статус любой другой") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusUNKNOWN")
        }
    }

    func test_pledge() {
        Step("Залоги; статус `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusOK")
        }

        Step("Залоги; статус `ERROR` / `INVALID`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusERROR")
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusINVALID")
        }

        Step("Залоги; статус любой другой") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusUNKNOWN")
        }
    }

    func test_pledgeNotReady() {
        Step("Залоги; статус `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusOK")
        }

        Step("Залоги; статус `ERROR` / `INVALID`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusERROR")

            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusINVALID")
        }

        Step("Залоги; статус любой другой") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusUNKNOWN")
        }
    }

    func test_wanted() {
        Step("Розыск; статус `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusOK")
        }

        Step("Розыск; статус `ERROR` / `INVALID`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusERROR")
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusINVALID")
        }

        Step("Розыск; статус любой другой") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_itemStatusUNKNOWN")
        }
    }

    func test_owners() {
        Step("Владельцы; без кол-ва") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_noOwners")
        }

        Step("Владельцы; 5 `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersOK")
        }

        Step("Владельцы; 5 `ERROR` / `INVALID`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersError")
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersINVALID")
        }

        Step("Владельцы; 5 другой статус") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersUNKNOWN")
        }
    }

    func test_ownersNotReady() {
        Step("Владельцы; без кол-ва") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_noOwners")
        }

        Step("Владельцы; 5 `OK`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersOK")
        }

        Step("Владельцы; 5 `ERROR` / `INVALID`") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersError")
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersINVALID")
        }

        Step("Владельцы; 5 другой статус") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5OwnersUNKNOWN")
        }
    }

    func test_dtp() {
        Step("ДТП; без кол-ва") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_noDTP")
        }

        Step("ДТП; -1") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_-1DTP")
        }

        Step("ДТП; > 0") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5DTP")
        }
    }

    func test_dtpNotReady() {
        Step("ДТП; без кол-ва") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_noDTP")
        }

        Step("ДТП; -1") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_-1DTP")
        }

        Step("ДТП; > 0") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5DTP")
        }
    }

    func test_taxi() {
        testHeaderNode(
            "taxiOnTapNode",
            name: "Такси",
            statuses: ["OK", "ERROR", "INVALID", "UNKNOWN"]
        )    }

    func test_carsharing() {
        testHeaderNode(
            "carsharingOnTapNode",
            name: "Каршеринг",
            statuses: ["OK", "ERROR", "INVALID", "UNKNOWN"]
        )    }

    func test_totalAuction() {
        testHeaderNode(
            "totalAuctionOnTapNode",
            cases: [
                ("Аукцион битых; Нет записей", "{}"),
                ("Аукцион битых; 5 записей", "{\"record_count\": 5}")
            ]
        )    }

    func test_repairCost() {
        testHeaderNode(
            "repairCalculationOnTapScrollNode",
            cases: [
                ("Оценка стоимости ремонта; Нет записей", "{}"),
                ("Оценка стоимости ремонта; 5 записей", "{\"record_count\": 5}")
            ]
        )    }

    func test_autoRuOffers() {
        testHeaderNode(
            "autoruOffersOnTapScrollNode",
            cases: [
                ("Продажи на автору; Нет записей", "{}"),
                ("Продажи на автору; 1 запись", "{\"record_count\": 1}"),
                ("Продажи на автору; 5 записей", "{\"record_count\": 5}")
            ]
        )    }

    func test_recalls() {
        testHeaderNode(
            "recallsOnTapScrollNode",
            cases: [
                ("Отзывные; Нет записей", "{}"),
                ("Отзывные; 5 записей", "{\"record_count\": 5}")
            ]
        )    }

    func test_vehiclePhotos() {
        testHeaderNode(
            "vehiclePhotosOnTapScrollNode",
            cases: [
                ("Фото автолюбителей; 5 фото", "5")
            ]
        )    }

    func test_brandCertification() {
        testHeaderNode(
            "brandCertificationOnTapScrollNode",
            cases: [
                ("Сертификации; нет записей", "{}"),
                ("Сертификации; 5 записей", "{\"record_count\": 5}")
            ]
        )    }

    func test_history() {
        testHeaderNode(
            "historyOnTapScrollNode",
            cases: [
                ("История эксплуатаций; нет записей", "{}"),
                ("История эксплуатаций; 5 записей", "{\"record_count\": 5}")
            ]
        )    }

    func test_equipment() {
        Step("Опции по VIN") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_mileages() {
        Step("Пробег без записей") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_noRecords")
        }

        Step("Пробег 5 записей") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5Records")
        }

        Step("Пробег 5 записей + график") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5Records+Graph")
        }
    }

    func test_mileagesNotReady() {
        Step("Пробег без записей") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_noRecords")
        }

        Step("Пробег 5 записей") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5Records")
        }

        Step("Пробег 5 записей + график") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_5Records+Graph")
        }
    }

    func test_priceStats() {
        Step("Цена < 1000000") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceLower1000000")
        }

        Step("Цена > 1000000") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceGreater1000000")
        }

        Step("Цена < 1000000 + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceLower1000000+Graph")
        }

        Step("Цена > 1000000 + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceGreater1000000+Graph")
        }
    }

    func test_priceStatsNotReady() {
        Step("Цена < 1000000") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceLower1000000")
        }

        Step("Цена > 1000000") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceGreater1000000")
        }

        Step("Цена < 1000000 + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceLower1000000+Graph")
        }

        Step("Цена > 1000000 + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_priceGreater1000000+Graph")
        }
    }

    func test_cheapeningGraph() {
        Step("Потеря в стоимости") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_cheapening")
        }

        Step("Потеря в стоимости + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_cheapening+Graph")
        }

        Step("Увеличение стоимости") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_growth")
        }

        Step("Увеличение стоимости + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_growth+Graph")
        }
    }

    func test_cheapeningGraphNotReady() {
        Step("Потеря в стоимости") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_cheapening")
        }

        Step("Потеря в стоимости + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_cheapening+Graph")
        }

        Step("Увеличение стоимости") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_growth")
        }

        Step("Увеличение стоимости + График") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_growth+Graph")
        }
    }

    func test_tax() {
        struct TestCase {
            let name: String
            let useFullTitle: Bool
            let tax: Int
            let year: Int
            let region: String
        }

        let cases: [TestCase] = [
            TestCase(name: "Налог < 1000; Полный тайтл", useFullTitle: true, tax: 753, year: 2021, region: "Москве"),
            TestCase(name: "Налог > 1000; Полный тайтл", useFullTitle: true, tax: 47896, year: 2021, region: "Москве"),
            TestCase(name: "Налог < 1000; Неполный тайтл", useFullTitle: false, tax: 753, year: 2021, region: "Москве"),
            TestCase(name: "Налог > 1000; Неполный тайтл", useFullTitle: false, tax: 47896, year: 2021, region: "Москве")
        ]

        for (idx, tcase) in cases.enumerated() {
            Step(tcase.name) {
                snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(idx)")

            }
        }
    }

    func test_sellTime() {
        Step("Среднее время продажи") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_reviews() {
        Step("Кол-во отзывов") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_promo() {
        Step("Промо") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_magazine() {
        Step("Журнал") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_writeReview() {
        Step("Написать отзыв") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_autoServices() {
        Step("Автосервисы") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_features() {
        Step("Рейтинг") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_addVIN() {
        Step("Добавить по VIN") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_proAvto() {
        Step("ProAvto") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_healthScore() {
        Step("Скор ещё не рассчитан") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_loading")
        }

        Step("Скор посчитан, нет диапазона скора") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_value")
        }

        Step("Скор посчитан, диапазона скора") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_value_range")
        }

        Step("Скор посчитан, диапазона скора из 1 числа") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)_value_empty_range")
        }
    }

    func test_estimates() {
        Step("Оценка эксперта") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_estimatesWithRecordCount() {
        Step("Оценка эксперта, 33") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_estimatesNotReady() {
        Step("Оценка эксперта, не готов") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    // MARK: - Private

    private func testHeaderNode(_ node: String, name: String, statuses: [String], idPrefix: String = #function) {
        for status in statuses {
            Step("\(name); `\(status)`; Блок готов") {
                snapshot(functionName: "\(String(describing: Self.self))_\(idPrefix)_\(status)")
            }

            Step("\(name); `\(status)`; Блок не готов") {
                snapshot(functionName: "\(String(describing: Self.self))_\(idPrefix)_\(status)_notReady")
            }
        }
    }

    private func testHeaderNode(_ node: String, cases: [(name: String, model: String)], idPrefix: String = #function) {
        for (idx, (name, _)) in cases.enumerated() {
            Step(name) {
                snapshot(functionName: "\(String(describing: Self.self))_\(idPrefix)_\(idx)")
            }
        }
    }

    private static func compareLayout(
        _ layout: LayoutConvertible,
        size: CGSize = CarReportCardHeaderTests.headerNodeLayoutSize,
        identifier: String
    ) {
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: size.width,
            maxHeight: size.height,
            interfaceStyle: [.light, .dark],
            identifier: identifier
        )
    }
}
