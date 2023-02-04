import AutoRuProtoModels
import Foundation

final class LegalCellTests: BaseUnitTest, CarReportCardBlockTest {
    static let statuses: [String] = ["OK", "ERROR", "INVALID", "UNDEFINED", "UNKNOWN", "IN_PROGRESS", "UNTRUSTED", "NOT_MATCHED_PLATE"]

    func test_constraints() {
        Step("Ограничения") {
            for status in Self.statuses {
                Step("Статус \(status)") {
                    snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
                }
            }
        }
    }

    func test_pledge() {
        Step("Залоги") {
            for status in Self.statuses {
                Step("Статус \(status)") {
                    snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
                }
            }
        }
    }

    func test_wanted() {
        Step("Розыск") {
            for status in Self.statuses {
                Step("Статус \(status)") {
                    snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
                }
            }
        }
    }
}
