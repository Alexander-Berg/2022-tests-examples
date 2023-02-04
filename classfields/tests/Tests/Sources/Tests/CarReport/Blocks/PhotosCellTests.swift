import AutoRuProtoModels
import AutoRuUtils
import AutoRuFetchableImage
@testable import AutoRuStandaloneCarHistory
import Foundation

final class PhotosCellTests: BaseUnitTest, CarReportCardBlockTest {
    static let statuses: [String] = ["OK", "ERROR", "INVALID", "UNDEFINED"]

    override func setUp() {
        super.setUp()

        FetchableImage.blockThreadUntilFinished = true
        setReplaceImagesWithStub(nil)
    }

    override func tearDown() {
        super.tearDown()

        FetchableImage.blockThreadUntilFinished = false
        setReplaceImagesDefaultBehavior()
    }

    func test_freeReportWithScore() {
        Step("Хедер (бесплатный со скором)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_paidReportNotReady() {
        Step("Хедер (куплен, не все источники)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_paidReportWithScore() {
        Step("Хедер (куплен со скором)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_PTS() {
        Step("ПТС") {
            for status in Self.statuses {
                Step("Статус \(status)") {
                    snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
                }
            }
        }
    }

    func test_PTS_updating() {
        Step("ПТС") {
            for status in Self.statuses {
                Step("Статус \(status)") {
                    snapshot(functionName: "\(String(describing: Self.self))_\(#function)_\(status)")
                }
            }
        }
    }

    func test_repairsLocked() {
        Step("Расчет стоимости ремонта (locked)") {
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
