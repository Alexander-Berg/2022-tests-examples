import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuCellHelpers AutoRuStandaloneCarHistory
final class CarfaxStandaloneCardBasicTests: BaseTest {
    private let suiteName = SnapshotIdentifier.suiteName(from: #file)
    private lazy var mainSteps = MainSteps(context: self)

    override var launchEnvironment: [String: String] {
        ["shouldForceShowSpinnerForUITest": "true"]
    }

    override func setUp() {
        super.setUp()
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    // MARK: - Хедер

    func test_headerCopyVIN() throws {
        Step("VIN выделяется в хедере") {}
        let steps = openReport()
            .wait(for: 2)

        let vinTextView = try XCTUnwrap(steps.findVIN("WVWZZZ1KZBW515003").first)
        vinTextView.shouldExist().tap(withNumberOfTaps: 2, numberOfTouches: 1)
        Snapshot.compareWithSnapshot(image: steps.snapshot(element: vinTextView))
    }

    // MARK: - ПТС

    func test_ptsCopyVIN() throws {
        Step("VIN выделяется в ПТС") {}
        let steps = openReport().tap(headerButton: .pts)

        let vinTextView = try XCTUnwrap(steps.wait(for: 1).findVIN("WVWZZZ1KZBW515003").last)
        vinTextView.shouldExist().tap(withNumberOfTaps: 2, numberOfTouches: 1)
        Snapshot.compareWithSnapshot(image: steps.snapshot(element: vinTextView))
    }

    // MARK: - Опции по VIN

    func test_equipment() {
        let steps = openReport()
        steps
            .tap(headerButton: .expand)
            .tap(headerButton: .equipment)
            .toggleEquipment(name: "Комфорт")

        Snapshot.compareWithSnapshot(
            image: steps.onScreen().snapshot(),
            identifier: "\(#function)_expand",
            ignoreEdges: UIEdgeInsets(top: 88, left: 0, bottom: 64, right: 0)
        )
        steps
            .toggleEquipment(name: "Комфорт")

        Snapshot.compareWithSnapshot(
            image: steps.onScreen().snapshot(),
            identifier: "\(#function)_collapse",
            ignoreEdges: UIEdgeInsets(top: 88, left: 0, bottom: 64, right: 0)
            )
    }

    func test_layout_popup() {
        let steps = openReport()
        steps
            .tap(headerButton: .expand)
            .tap(headerButton: .fines)
            .findLabel(text: "Штраф")
            .tap()

        steps
            .wait(for: 1)
            .closePopup()
    }

    // MARK: - Обновление отчета

    func test_reportPollReloaded() {
        mocker.mock_reportRaw { model in
            model.report.sources.readyCount = 10
            model.report.sources.sourcesCount = 24
        }
        let steps = openReport()

        Step("Подменяем на ответ с полностью загруженным отчетом") {
            mocker.mock_reportRaw { model in
                model.report.sources.readyCount = 24
                model.report.sources.sourcesCount = 24
            }
        }

        Step("Проверяем отсутствие банера в шапке") {
            steps.checkHasNoReportPollHeader(timeout: 10)
        }

        Step("Валидируем снэкбар") {
            steps.validateActivityHUD()
        }

        Step("Проверяем отсутствие банера в подвале") {
            steps.tap(headerButton: .expand)
                .tap(headerButton: .recalls)
            steps.checkHasNoReportPollFooter(timeout: 10)
        }
    }

    func test_readyReportNoSnackbar() {
        mocker.mock_reportRaw { model in
            model.report.sources.readyCount = 24
            model.report.sources.sourcesCount = 24
        }

        openReport().checkHasNoActivityHUD()
    }

    // MARK: -

    private func openReport() -> CarfaxStandaloneCardBasicSteps {
        app.launchEnvironment["LAUNCH_DEEPLINK_URL"] = "https://auto.ru/history/WP0ZZZ99ZHS112625"
        mocker.mock_reportLayoutForSearch(bought: true)
            .mock_reportLayoutForReport(bought: true)

        launch()
        let startScreen = CarfaxStandaloneScreen(app)
        startScreen.openFullReportButton
            .shouldExist(timeout: 5).tap()

        return mainSteps.as(CarfaxStandaloneCardBasicSteps.self)
    }
}
