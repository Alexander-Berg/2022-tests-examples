import Foundation
import XCTest
import AutoRuProtoModels
@testable import AutoRuComparison

final class ComplectationComparisonClassifierTests: BaseUnitTest {
    // Комплектация -> (число техпарамов, число базовых опций, число допов)
    private typealias ComplectationValues = (Int, Int, Int)

    private lazy var catalogResponse: Auto_Api_CatalogResponse = {
        let url = Bundle.current
            .url(forResource: "complectation_comparison_subtree_skoda_octavia", withExtension: "json")!
        let response = try! Auto_Api_CatalogResponse(jsonUTF8Data: Data(contentsOf: url))
        return response
    }()

    private lazy var complectations: [ComplectationComparisonType.Complectation] = {
        [
            ComplectationComparisonType.Complectation(
                title: "Active",
                priceFrom: 1_050_000,
                complectationIDs: ["20913347", "20913376"],
                techParams: ["20913312", "20913311"]
            ),
            ComplectationComparisonType.Complectation(
                title: "Hockey Edition",
                priceFrom: 1_379_300,
                complectationIDs: ["21403777", "21403744"],
                techParams: ["20913312", "20898376", "20913311"]
            ),
            ComplectationComparisonType.Complectation(
                title: "Style",
                priceFrom: 1_719_099,
                complectationIDs: ["20913628"],
                techParams: ["20913312", "20898376"]
            ),
            ComplectationComparisonType.Complectation(
                title: "Ambition",
                priceFrom: 1_170_000,
                complectationIDs: ["20913391", "21158711"],
                techParams: ["20898379", "20913312", "20898378", "20913311"]
            )
        ]
    }()

    func test_classifierTechParamNamesFromCatalog() {
        let classifier = ComplectationOptionsClassifier(rawCatalog: self.catalogResponse.data)
        self.checkTechParamsMapping(classifier: classifier)
    }

    func test_classifierTechParamNamesFromOffers() {
        let classifier = ComplectationOptionsClassifier(
            complectations: self.complectations,
            rawCatalog: self.catalogResponse.data
        )
        self.checkTechParamsMapping(classifier: classifier)
    }

    func test_classifierOptionsAndTechParamsFromCatalog() {
        let classifications: [String: ComplectationValues] = [
            "Active": (3, 29, 5),
            "Hockey Edition": (5, 47, 14),
            "Ambition": (7, 42, 28),
            "Style": (7, 50, 23),
            "Laurin & Klement": (3, 57, 21)
        ]

        let classifier = ComplectationOptionsClassifier(rawCatalog: self.catalogResponse.data)
        self.checkComplectationsOptionsAndTechParams(classifier: classifier, classifications: classifications)
    }

    func test_classifierOptionsAndTechParamsFromOffers() {
        let classifications: [String: ComplectationValues] = [
            "Active": (2, 29, 5),
            "Hockey Edition": (3, 47, 14),
            "Ambition": (4, 41, 23),
            "Style": (2, 49, 18),
            "Laurin & Klement": (3, 57, 21)
        ]

        let classifier = ComplectationOptionsClassifier(
            complectations: self.complectations,
            rawCatalog: self.catalogResponse.data
        )
        self.checkComplectationsOptionsAndTechParams(classifier: classifier, classifications: classifications)
    }

    func test_classifierOptionFromCatalog() {
        let optionName = "climate-control-2"

        // Климат-контроль 2-зонный
        // Active => недоступна
        // Hockey Edition => базовая опция
        // Ambition => доп, но для всех кроме 2-х техпарамов
        // L & K => недоступна
        // Style => базовая опция для всех кроме 2-х техпарамов

        let classifier = ComplectationOptionsClassifier(rawCatalog: self.catalogResponse.data)
        for complectation in classifier.complectations {
            XCTContext.runActivity(named: "Для комплектации \(complectation.name) проверяем статус опции \(optionName)") { _ in
                switch complectation.name {
                case "Active", "Laurin & Klement":
                    XCTContext.runActivity(named: "\(complectation.name) => \(optionName) недоступна") { _ in
                        XCTAssert(!complectation.hasBaseOption(with: optionName), "В \(complectation.name) нет опции \(optionName)")
                        XCTAssert(!complectation.hasAdditionalOption(with: optionName), "В \(complectation.name) нет опции \(optionName)")
                    }
                case "Hockey Edition":
                    XCTContext.runActivity(named: "Hockey Edition => базовая опция") { _ in
                        XCTAssert(complectation.hasBaseOption(with: optionName), "В HE опция \(optionName) в базовой комплектации")
                        XCTAssert(!complectation.hasAdditionalOption(with: optionName), "В HE опция \(optionName) в базовой комплектации")
                    }
                case "Ambition":
                    XCTContext.runActivity(named: "Ambition => доп, но для всех кроме 2-х техпарамов") { _ in
                        let option = complectation.additionalOptions.first(where: { $0.name == optionName })
                        XCTAssert(!complectation.hasBaseOption(with: optionName), "В Ambition опции \(optionName) нет в базовой комплектации")
                        XCTAssert(
                            complectation.hasAdditionalOption(with: optionName)
                                && option?.techParams.count == complectation.techParams.count - 2,
                            "В Ambition опция \(optionName) доп для всех кроме 2-х техпарамов"
                        )
                    }
                case "Style":
                    XCTContext.runActivity(named: "Style => базовая опция для всех кроме 2-х техпарамов") { _ in
                        let option = complectation.baseOptions.first(where: { $0.name == optionName })
                        XCTAssert(
                            complectation.hasBaseOption(with: optionName)
                                && option?.techParams.count == complectation.techParams.count - 2,
                            "В Style опция \(optionName) в базовой комплектации для всех кроме 2-х техпарамов"
                        )
                        XCTAssert(!complectation.hasAdditionalOption(with: optionName), "В Style опции \(optionName) нет в допах")
                    }
                default:
                    XCTAssert(false, "Неизвестная комплектация")
                }
            }
        }
    }

    func test_classifierOptionFromOffers() {
        let optionName = "climate-control-2"

        // Климат-контроль 2-зонный
        // Active => недоступна
        // Hockey Edition => базовая комплектация
        // Ambition => доп
        // L & K => нету офферов
        // Style => базовая комплектация

        let classifier = ComplectationOptionsClassifier(
            complectations: self.complectations,
            rawCatalog: self.catalogResponse.data
        )
        for complectation in classifier.complectations {
            XCTContext.runActivity(named: "Для комплектации \(complectation.name) проверяем статус опции \(optionName)") { _ in
                switch complectation.name {
                case "Laurin & Klement":
                    XCTAssert(false, "С Laurin & Klement нет офферов")
                case "Active":
                    XCTContext.runActivity(named: "\(complectation.name) => \(optionName) недоступна") { _ in
                        XCTAssert(!complectation.hasBaseOption(with: optionName), "В \(complectation.name) нет опции \(optionName)")
                        XCTAssert(!complectation.hasAdditionalOption(with: optionName), "В \(complectation.name) нет опции \(optionName)")
                    }
                case "Hockey Edition", "Style":
                    XCTContext.runActivity(named: "\(complectation.name) => базовая опция") { _ in
                        XCTAssert(complectation.hasBaseOption(with: optionName), "В \(complectation.name) опция \(optionName) в базовой комплектации")
                        XCTAssert(!complectation.hasAdditionalOption(with: optionName), "В \(complectation.name) опция \(optionName) в базовой комплектации")
                    }
                case "Ambition":
                    XCTContext.runActivity(named: "Ambition => доп, но для всех кроме 2-х техпарамов") { _ in
                        XCTAssert(!complectation.hasBaseOption(with: optionName), "В Ambition опции \(optionName) нет в базовой комплектации")
                        XCTAssert(complectation.hasAdditionalOption(with: optionName), "В Ambition опция \(optionName) доп для всех кроме 2-х техпарамов")
                    }
                default:
                    XCTAssert(false, "Неизвестная комплектация")
                }
            }
        }
    }

    // MARK: - Private

    private func checkComplectationsOptionsAndTechParams(
        classifier: ComplectationOptionsClassifier,
        classifications: [String: ComplectationValues]
    ) {
        for complectation in classifier.complectations {
            XCTContext.runActivity(named: "Для комплектации \(complectation.name) проверяем кол-во опций и техпарамов") { _ in
                guard let values = classifications[complectation.name] else {
                    XCTAssert(false, "Неизвестная комплектация")
                    return
                }

                XCTAssert(
                    values.0 == complectation.techParams.count,
                    "Неверное число техпарамов в комлектации \(complectation.name): \(complectation.techParams.count) вместо \(values.0)"
                )
                XCTAssert(
                    values.1 == complectation.baseOptions.count,
                    "Неверное число базовых опций в комлектации \(complectation.name): \(complectation.baseOptions.count) вместо \(values.1)"
                )
                XCTAssert(
                    values.2 == complectation.additionalOptions.count,
                    "Неверное число допов в комлектации \(complectation.name): \(complectation.additionalOptions.count) вместо \(values.2)"
                )
                XCTAssert(
                    (complectation.additionalOptions.union(complectation.baseOptions)).count == complectation.allOptions.count,
                    "Неверное число опций в комлектации \(complectation.name)"
                )
            }
        }
    }

    private func checkTechParamsMapping(classifier: ComplectationOptionsClassifier) {
        let mapping: [String: String] = [
            "20898335": "1.6d AMT (116 л.с.)",
            "21550284": "2.0 AMT (190 л.с.)",
            "20898379": "1.4 MT (150 л.с.)",
            "21550305": "2.0 AMT (190 л.с.) 4WD",
            "20899621": "1.4 AMT (110 л.с.)",
            "20898376": "1.4 AMT (150 л.с.)",
            "20899599": "1.4 MT (110 л.с.)",
            "21550364": "1.6d MT (116 л.с.) 4WD",
            "20898247": "2.0d MT (150 л.с.) 4WD",
            "20898304": "1.6d MT (90 л.с.)",
            "20913312": "1.6 AT (110 л.с.)",
            "20898319": "1.6d MT (116 л.с.)",
            "20898381": "1.2 MT (86 л.с.)",
            "20898375": "1.0 MT (116 л.с.)",
            "21550235": "1.5 AMT (150 л.с.)",
            "20898378": "1.8 AMT (180 л.с.)",
            "20898246": "1.8 AMT (180 л.с.) 4WD",
            "21550211": "1.5 MT (150 л.с.)",
            "20898354": "2.0d MT (150 л.с.)",
            "20913311": "1.6 MT (110 л.с.)",
            "20898374": "2.0d AMT (150 л.с.)",
            "20898380": "1.0 AMT (116 л.с.)",
            "20898377": "1.8 MT (180 л.с.)",
            "20898266": "2.0d AMT (150 л.с.) 4WD",
            "20898248": "2.0d AMT (184 л.с.) 4WD"
        ]

        XCTContext.runActivity(named: "Проверяем маппинг техпарам => название модификации") { _ in
            XCTAssert(classifier.techParamsToName.count == mapping.count, "Неверное число техпарамов из каталога")
            for (key, value) in mapping where classifier.techParamsToName[key] != value {
                XCTAssert(false, "Неверное название техпарама \(key): \(String(describing: classifier.techParamsToName[key])) вместо \(value)")
            }
        }
    }
}

extension ComplectationOptionsClassifier.Complectation {
    func hasBaseOption(with name: String) -> Bool {
        return self.baseOptions.contains(.init(name: name, techParams: []))
    }

    func hasAdditionalOption(with name: String) -> Bool {
        return self.additionalOptions.contains(.init(name: name, techParams: []))
    }
}
