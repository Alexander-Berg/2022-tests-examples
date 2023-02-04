import XCTest

/// @depends_on AutoRuSaleList AutoRuElectroCars
final class SaleListElectrocarsTests: BaseTest {
    override func setUp() {
        super.setUp()
        setupServer()
    }

    override func tearDown() {
        super.tearDown()
        mocker.stopMock()
    }

    func test_saleCardElectrocarsBanner() {
        launchMain(options: .init(overrideAppSettings: ["electrocarsEnabled": true]))
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist).focus {
                $0.tap(.engine)
            }
            .should(provider: .modalPicker, .exist).focus {
                $0.tap(.item("Электро"))
                $0.tap(.item("Готово"))
            }
            .should(provider: .filtersScreen, .exist).focus {
                $0.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist).focus {
                $0.tap(.electrocarsBanner)
            }
            .should(provider: .electroCarsMainScreen, .exist)
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_searchCount(count: 2)
            .mock_searchCars {
                Responses.Search.Cars.success(for: .global)
            }

        mocker.startMock()
    }
}
