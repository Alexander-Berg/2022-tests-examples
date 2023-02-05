import MarketUITestMocks
import Metrics
import XCTest

final class ExpressMetricsTests: LocalMockTestCase {

    func testExpressErrorMetrics() {
        Allure.addEpic("Экспресс")
        Allure.addFeature("Метрики экспресса")
        Allure.addTitle("События здоровья. Ошибка загрузки экрана экспресса")

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock
            setupState(
                with: CMSState.CMSCollections.testCollections,
                forceEmptyNavigationTree: false,
                forceErrorForNavigationTree: false
            )
        }

        "Открываем экран экспресса".ybm_run { _ in
            goToExpress()
        }

        "Проверяем отправленные события".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                MetricRecorder.events(from: .health)
                    .with(name: "ERROR_EXPRESS")
                    .isNotEmpty
            })
        }
    }

    func testEmptyCMSExpressErrorMetrics() {
        Allure.addEpic("Экспресс")
        Allure.addFeature("Метрики экспресса")
        Allure.addTitle("События здоровья. Пустой экран экспресса")

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock
            setupState(
                with: CMSState.CMSCollections.emptyExpressCollections,
                forceEmptyNavigationTree: false,
                forceErrorForNavigationTree: false
            )
        }

        "Открываем экран экспресса".ybm_run { _ in
            goToExpress()
        }

        "Проверяем отправленные события".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                MetricRecorder.events(from: .health)
                    .with(name: "EMPTY_EXPRESS")
                    .isNotEmpty
            })
        }
    }

    func testExpressErrorCategoriesMetrics() {
        Allure.addEpic("Экспресс")
        Allure.addFeature("Метрики экспресса")
        Allure.addTitle("События здоровья. Ошибка загрузки данных для виджетов экспресса")

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock
            setupState(
                with: CMSState.CMSCollections.expressCollections,
                forceEmptyNavigationTree: false,
                forceErrorForNavigationTree: true
            )
        }

        "Открываем экран экспресса".ybm_run { _ in
            goToExpress()
        }

        "Проверяем отправленные события".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                MetricRecorder.events(from: .health)
                    .with(name: "ERROR_EXPRESS_CATEGORIES")
                    .isNotEmpty
            })
        }
    }

    func testExpressEmptyCategoriesMetrics() {
        Allure.addEpic("Экспресс")
        Allure.addFeature("Метрики экспресса")
        Allure.addTitle("События здоровья. Пустые данные для виджетов экспресса")

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock
            setupState(
                with: CMSState.CMSCollections.expressCollections,
                forceEmptyNavigationTree: true,
                forceErrorForNavigationTree: false
            )
        }

        "Открываем экран экспресса".ybm_run { _ in
            goToExpress()
        }

        "Проверяем отправленные события".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                MetricRecorder.events(from: .health)
                    .with(name: "EMPTY_EXPRESS_CATEGORIES")
                    .isNotEmpty
            })
        }
    }
}

private extension ExpressMetricsTests {
    private func setupState(
        with cmsCollections: ResolveCMS.Collections,
        forceEmptyNavigationTree: Bool,
        forceErrorForNavigationTree: Bool
    ) {
        var cmsState = CMSState()
        cmsState.setCMSState(with: cmsCollections)
        stateManager?.setState(newState: cmsState)

        let expressState = ExpressState()
        stateManager?.setState(newState: expressState)

        var expressNavigationTreeState = ExpressNavigationTreeState()
        if forceEmptyNavigationTree {
            expressNavigationTreeState.addResolveEmptyExpressNavigationTreeHandler()
        }
        if forceErrorForNavigationTree {
            expressNavigationTreeState.addResolveWithErrorExpressNavigationTreeHandler()
        }
        stateManager?.setState(newState: expressNavigationTreeState)

        var userState = UserAuthState()
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }
}
