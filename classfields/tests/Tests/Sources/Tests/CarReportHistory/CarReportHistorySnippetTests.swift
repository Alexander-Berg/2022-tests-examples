import XCTest
import AutoRuProtoModels
import AutoRuTableController
import AutoRuAppearance
import AutoRuCommonViews
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuStandaloneCarHistory
import AutoRuColorSchema
import AutoRuProgressIndicators

final class CarReportHistorySnippetTests: BaseUnitTest {
    private enum LayoutType {
        case body
        case buttons
    }

    override func setUp() {
        super.setUp()

        Spinner.disableAnimationForTests = true
        FetchableImage.blockThreadUntilFinished = true
        setReplaceImagesWithStub(nil)
    }

    override func tearDown() {
        super.tearDown()

        Spinner.disableAnimationForTests = false
        FetchableImage.blockThreadUntilFinished = false
        setReplaceImagesDefaultBehavior()
    }

    func test_withHealthScore_value() {
        Step("Проверяем сниппет купленного отчета с хелс-скором (значение)") { }

        var report = Self.makeReport()
        report.rawReport.healthScore = .with { score in
            score.score = .init(81.0)
        }

        Self.checkReport(report)
    }

    func test_withHealthScore_loading() {
        Step("Проверяем сниппет купленного отчета с хелс-скором (скор не готов): бейдж скора со спиннером") { }

        var report = Self.makeReport()
        report.rawReport.healthScore = .with { score in
            score.header = .with { header in
                header.isUpdating = true
            }
        }

        Self.checkReport(report)
    }

    func test_withoutHealthScore() {
        Step("Проверяем сниппет купленного отчета без хелс-скора: вместо бейджа иконка марки") { }

        var report = Self.makeReport()
        report.rawReport.clearHealthScore()

        Self.checkReport(report)
    }

    func test_inProgress_withoutScore() {
        Step("Проверяем сниппет купленного отчета c неготовым отчетом и без скора: должен быть знак '!'") { }

        var report = Self.makeReport()
        report.rawReport.header.isUpdating = true
        report.rawReport.clearHealthScore()

        Self.checkReport(report)
    }

    func test_inProgress_withScore() {
        Step("Проверяем сниппет купленного отчета c неготовым отчетом и со скором: должен быть бейдж скора со спиннером") { }

        var report = Self.makeReport()
        report.rawReport.header.isUpdating = true
        report.rawReport.healthScore = .with { score in
            score.header = .with { header in
                header.isUpdating = true
            }
        }

        Self.checkReport(report)
    }

    func test_isFavorite() {
        Step("Проверяем сниппет купленного отчета c оффером в избранном") { }
        let report = Self.makeReport()

        Self.checkReport(report, isFavorite: true, toggleFavoritesButtonVisible: true, layoutType: .buttons)
    }

    func test_notFavorite() {
        Step("Проверяем сниппет купленного отчета c оффером в избранном") { }
        let report = Self.makeReport()

        Self.checkReport(report, isFavorite: false, toggleFavoritesButtonVisible: true, layoutType: .buttons)
    }

    // MARK: - Private

    private static func checkReport(_ report: Auto_Api_BoughtReport, identifier: String = #function, isFavorite: Bool = false, toggleFavoritesButtonVisible: Bool = false, layoutType: LayoutType = .body) {
        let model = CarReportSnippetModel(with: report, favorites: [])

        let tableModelBuilder = CarReportsListTableModelBuilder()
        tableModelBuilder.addReportRow(
            title: model.title,
            vin: model.vin,
            image: model.image,
            paramsBlocks: model.paramsBlocks,
            healthScore: model.healthScore,
            reportInProgress: model.reportInProgress,
            openReport: { },
            openOffer: { },
            loadReportPDF: { },
            toggleFavorites: { },
            toggleFavoritesButtonVisible: toggleFavoritesButtonVisible,
            isFavorite: isFavorite,
            reportPDFLoading: false
        )

        let cellHelper: CellHelper

        switch layoutType {
        case .body:
            cellHelper = tableModelBuilder.tableItem(withIdStartsWith: "report_body_row_WDD2229851A278094")!.cellHelper
        case .buttons:
            cellHelper = tableModelBuilder.tableItem(withIdStartsWith: "report_buttons_row_WDD2229851A278094")!.cellHelper
        }

        Snapshot.compareWithSnapshot(
            cellHelper: cellHelper,
            maxWidth: 414,
            backgroundColor: ColorSchema.Background.surface,
            identifier: identifier
        )
    }

    private static func makeReport() -> Auto_Api_BoughtReport {
        let url = Bundle.current
            .url(forResource: "car_report_bought_report", withExtension: "json")!
        let response = try! Auto_Api_BoughtReport(jsonUTF8Data: Data(contentsOf: url))
        return response
    }
}
