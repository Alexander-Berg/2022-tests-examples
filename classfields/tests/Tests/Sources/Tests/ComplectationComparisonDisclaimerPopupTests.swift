import Foundation
import XCTest
import AutoRuProtoModels
@testable import AutoRuComparison

final class ComplectationComparisonDisclaimerTests: BaseUnitTest {
    func test_popupTextAdditionalOption() {
        let popup1 = ComplectationComparisonDisclaimerPopup(isAdditional: true, onlyForTechParams: nil)
        XCTAssert(popup1.descriptionText == "Может быть установлена за доплату.")

        let popup2 = ComplectationComparisonDisclaimerPopup(isAdditional: true, onlyForTechParams: "[для всех модификаций]")
        XCTAssert(popup2.descriptionText == "Может быть установлена за доплату [для всех модификаций].")
    }

    func test_popupTextBaseOption() {
        let popup1 = ComplectationComparisonDisclaimerPopup(isAdditional: false, onlyForTechParams: "[для всех модификаций]")
        XCTAssert(popup1.descriptionText == "Опция доступна [для всех модификаций].")
    }

    func test_techParamsDisclaimerText() {
        let techParams = Set(["1", "2", "3", "4", "5"])
        let names = ["1": "<1>", "2": "<2>", "3": "<3>", "4": "<4>", "5": "<5>"]

        let text1 = ComplectationComparisonPresenter.makeTechParamsDislaimerIfNeeded(
            techParams: techParams,
            names: names,
            techParamsWithComplectaton: techParams
        )
        // все комплектации => нечего добавлять
        XCTAssertNil(text1)

        let text2 = ComplectationComparisonPresenter.makeTechParamsDislaimerIfNeeded(
            techParams: ["1"],
            names: names,
            techParamsWithComplectaton: techParams
        )
        XCTAssertEqual(text2, "только для модификации <1>")

        let text3 = ComplectationComparisonPresenter.makeTechParamsDislaimerIfNeeded(
            techParams: ["1", "2"],
            names: names,
            techParamsWithComplectaton: techParams
        )
        XCTAssertEqual(text3, "только для модификаций <1> и <2>")

        let text4 = ComplectationComparisonPresenter.makeTechParamsDislaimerIfNeeded(
            techParams: ["1", "2", "3"],
            names: names,
            techParamsWithComplectaton: techParams
        )
        XCTAssertEqual(text4, "для всех модификаций, кроме <4> и <5>")
    }
}
