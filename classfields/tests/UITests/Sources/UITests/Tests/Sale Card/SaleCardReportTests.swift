import Foundation

/// @depends_on AutoRuSaleCard
final class SaleCardOpenReportTests: BaseTest {
    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_canCopyPreviewVIN() {
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .scroll(to: .carReportPreview)
            .focus(on: .carReportPreview, ofType: .carReportPreviewCell) { cell in
                cell.focus(on: .vin("Z94C351BBJR002450")) { vin in
                        vin
                            .longTap()
                            .validateSnapshot()
                    }
            }
    }

    func test_openPurchaseCarReportFromVinOrGrz() {
        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .scroll(to: .сharacteristicCell)
            .focus(on: .сharacteristicCell, ofType: .characteristicCell) { cell in
                cell
                    .scroll(to: .carReportWithVin)
                    .tap(.carReportWithVin)
            }
            .should(provider: .carReportScreen, .exist)
    }

    // MARK: - Private

    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_offerFromHistoryLastAll()
            .mock_reportLayoutForReport(bought: true)
            .mock_reportLayoutForOffer(bought: true, copyVIN: true)
            .startMock()
    }
}
