import AutoRuAppearance
import AutoRuColorSchema
import AutoRuFetchableImage
import AutoRuModels
import AutoRuProtoModels
import AutoRuUtils
import CoreGraphics
import Snapshots
import XCTest
@testable import AutoRuEstimate

final class EstimateResultTests: BaseUnitTest {
    func test_estimateResult() {
        let viewState = makeViewState()
        checkView(viewState: viewState, height: 1_543.5)
    }

    func test_estimateResultWithoutPriceGroup() {
        var viewState = makeViewState()
        viewState.priceGroups = []
        checkView(viewState: viewState, height: 1_486)
    }

    func test_estimateResultWithoutChart() {
        var viewState = makeViewState()
        viewState.chartData = nil
        checkView(viewState: viewState, height: 1_338)
    }

    func test_estimateResultWithoutTradeIn() {
        var viewState = makeViewState()
        viewState.price?.tradeIn = nil
        viewState.chartData = nil
        checkView(viewState: viewState, height: 992)
    }

    func test_estimateResultWithoutPrice() {
        var viewState = makeViewState()
        viewState.price = nil
        checkView(viewState: viewState, height: 982.5)
    }

    // MARK: - Private

    private func checkView(viewState: EstimationResultViewState, height: CGFloat, id: String = #function) {
        let view = EstimationResultView(viewState: viewState)
        view.frame = CGRect(width: DeviceWidth.iPhone11, height: height)
        view.backgroundColor = ColorSchema.Background.background
        Snapshot.compareWithSnapshot(view: view, identifier: id)
    }

    private func makeViewState() -> EstimationResultViewState {
        let characteristics = EstimationResultCharacteristicViewState(
            icon: FetchableImage.testImage(withFixedSize: CGSize(width: 24, height: 24)),
            image: nil,
            markModel: "Kia Optima",
            characteristics: [
                "2.4/188 л.с.",
                "Бензин",
                "5 555 км",
                "2018 г.",
                "Передний",
                "AT",
                "Седан",
                "2 владелец",
                "Белый",
                "Куплен в 2021",
                "Luxe"
            ]
        )

        let tradeInPrice = EstimationResultPriceViewState(
            title: "Продажа дилеру в трейд-ин",
            price: 1_795_000,
            from: 1_725_000,
            to: 1_864_000
        )

        let autoRuPrice = EstimationResultPriceViewState(
            title: "Продажа на Авто.ру",
            price: 1_930_000,
            from: 1_860_000,
            to: 1_999_000
        )

        let chartPoints = [CGPoint(x: 0.166, y: 0.5), CGPoint(x: 0.5, y: 1.0), CGPoint(x: 0.833, y: 0.3)]
        let chartPosition = CGFloat(0.7474)

        let priceGroups = [
            EstimationResultPriceGroupViewState(amount: 5, price: 1_425_000),
            EstimationResultPriceGroupViewState(amount: 10, price: 1_650_000),
            EstimationResultPriceGroupViewState(amount: 3, price: 1_875_000)
        ]

        return EstimationResultViewState(
            characteristics: characteristics,
            price: (tradeInPrice, autoRuPrice),
            chartData: (chartPoints, chartPosition),
            priceGroups: priceGroups,
            canAddToGarage: true
        )
    }
}
