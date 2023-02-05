import MarketUITestMocks
import XCTest

final class PharmaOrderTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testPrescriptionDelivery() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5397")
        Allure.addEpic("Покупка списком")
        Allure.addFeature("Заказ рецептурных препаратов")
        Allure.addTitle("Доставка курьером")

        var cartPage: CartPage!
        var medicineMapPage: MedicineMapPage!

        "Мокаем состояние".run {
            setupStates()
        }

        "Открываем корзину".run {
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.cartItem(at: 0).element)
        }

        "Переходим к оформлению".run {
            wait(forVisibilityOf: cartPage.compactSummary.orderButton.element)
            cartPage.compactSummary.orderButton.element.tap()
            medicineMapPage = MedicineMapPage.current
            wait(forVisibilityOf: medicineMapPage.element)
        }

        "Проверяем кнопку курьерки".run {
            XCTAssertEqual(medicineMapPage.serviceSelector.label, "Курьер")
            XCTAssertFalse(medicineMapPage.serviceSelector.isEnabled)
        }
    }

}

// MARK: - Test state setup methods

private extension PharmaOrderTest {
    func setupStates() {
        setupEnvironment()
        setupUserState()
        setupPharmaState()
        setupCartState()
        setupOrdersState()
        setupSkuInfoState()
    }

    func setupEnvironment() {
        enable(
            toggles:
            FeatureNames.purchaseByListMedicineFeature,
            FeatureNames.purchaseByListMedicineForceOnFeature
        )

        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLatitude] = "55.741"
        app.launchEnvironment[TestLaunchEnvironmentKeys.locationLongitude] = "37.432"
    }

    func setupUserState() {
        var userAuthState = UserAuthState()
        userAuthState.setUserAddressByGpsCoordinate(
            result: .novinskiy,
            byGps: [.init(region: .moscow, address: .novinskiy)]
        )
        stateManager?.setState(newState: userAuthState)
    }

    func setupPharmaState() {
        var pharmaState = PharmaState()
        pharmaState.setDeliveryBuckets(.empty)
        pharmaState.setShopsBySkus(offers: [.pharmaPrescription])
        stateManager?.setState(newState: pharmaState)
    }

    func setupCartState() {
        var cartState = CartState()
        cartState.setCartStrategy(with: [.pharmaPrescription])
        cartState.setUserOrdersState(with: .pharmaPrescription)
        stateManager?.setState(newState: cartState)
    }

    func setupOrdersState() {
        var ordersState = OrdersState()
        ordersState.setOutlet(outlets: [.rublevskoye])
        stateManager?.setState(newState: ordersState)
    }

    func setupSkuInfoState() {
        var skuInfoState = SKUInfoState()
        skuInfoState.setOffersById(mapper: .pharmaPrescription)
        stateManager?.setState(newState: skuInfoState)
    }

}
