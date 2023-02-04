import XCTest
@testable import AutoRuUtils

final class VINGovNumberValidatorTests: BaseUnitTest {
    func test_vinValidation() {
        let validator = VINgovNumberValidator.self

        XCTAssertTrue(validator.isValidVIN("ASDB1234XYZ123456"), "Вин должен быть из 17 символов и A-Z")
        XCTAssertTrue(validator.isValidVIN("asdb1234xyz123456"), "Вин должен быть из 17 символов и A-Z")
        XCTAssertFalse(validator.isValidVIN("ASDB"), "Вин должен быть из 17 символов")
        XCTAssertFalse(validator.isValidVIN(String(repeating: "A", count: 20)), "Вин должен быть из 17 символов")
        XCTAssertFalse(validator.isValidVIN(String(repeating: "Г", count: 17)), "Вин должен быть из букв A-Z")
    }

    func test_govNumberDefaultValidation() {
        let validator = VINgovNumberValidator.self

        XCTAssertTrue(validator.isValidGovNumber("Y111YY77"), "Госномер должен иметь вид A000AA[регион]")
        XCTAssertTrue(validator.isValidGovNumber("Y111YY777"), "Госномер должен иметь вид A000AA[регион]")
        XCTAssertTrue(validator.isValidGovNumber("У111УУ77"), "Госномер должен иметь вид A000AA[регион]")
        XCTAssertTrue(validator.isValidGovNumber("Y111УY77"), "Госномер должен иметь вид A000AA[регион]")
        XCTAssertTrue(validator.isValidGovNumber("x111aу77"), "Госномер может содержать символы любого регистра")

        XCTAssertFalse(validator.isValidGovNumber("S111AB77"), "Госномер должен содержать только ABEKMHOPCTYX и кириллические аналоги")
        XCTAssertFalse(validator.isValidGovNumber("A111AB7"), "Регион должен содержать 2-3 цифры")
        XCTAssertFalse(validator.isValidGovNumber("A111AB9999"), "Регион должен содержать 2-3 цифры")
        XCTAssertFalse(validator.isValidGovNumber("1A11AA"), "Госномер должен быть верного формата")
    }

    func test_govNumberTaxiValidation() {
        let validator = VINgovNumberValidator.self

        XCTAssertTrue(validator.isValidGovNumber("YY11177"), "Госномер должен иметь вид AA000[регион]")
        XCTAssertTrue(validator.isValidGovNumber("YY11177"), "Госномер должен иметь вид AA000[регион]")
        XCTAssertTrue(validator.isValidGovNumber("УУ11177"), "Госномер должен иметь вид AA000[регион]")
        XCTAssertTrue(validator.isValidGovNumber("YУ11177"), "Госномер должен иметь вид AA000[регион]")
        XCTAssertTrue(validator.isValidGovNumber("xу11177"), "Госномер может содержать символы любого регистра")

        XCTAssertFalse(validator.isValidGovNumber("SS11177"), "Госномер должен содержать только ABEKMHOPCTYX и кириллические аналоги")
        XCTAssertFalse(validator.isValidGovNumber("AA1117"), "Регион должен содержать 2-3 цифры")
        XCTAssertFalse(validator.isValidGovNumber("AA1119999"), "Регион должен содержать 2-3 цифры")
        XCTAssertFalse(validator.isValidGovNumber("AA111A77"), "Госномер должен быть верного формата")
    }

    func test_isValidVINOrGovNumber() {
        let validator = VINgovNumberValidator.self

        XCTAssertTrue(validator.isValidVINOrGovNumber("ASDB1234XYZ123456"), "Валидный вин")
        XCTAssertTrue(validator.isValidVINOrGovNumber("А111АА77"), "Валидный госномер")
        XCTAssertFalse(validator.isValidVINOrGovNumber("A2222FR"), "Невалидная строка")
    }
}

extension VINgovNumberValidator {
    static func isValidVINOrGovNumber(_ text: String) -> Bool {
        isValidVIN(text) || isValidGovNumber(text)
    }
}
