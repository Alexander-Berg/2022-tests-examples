import MarketUITestMocks
import XCTest

final class SKUCardModelBookingTest: LocalMockTestCase {

    func testBookingDeliveryOptionShown() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5877")
        Allure.addEpic("КМ")
        Allure.addFeature("Бронирование из наличия")
        Allure.addTitle("Лейбл на КМ - Самовывоз через 1-2 часа")

        var sku: SKUPage!
        var skuState = SKUInfoState()

        var defaultState = DefaultState()

        enable(toggles: FeatureNames.purchaseByListBooking)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo
        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента c букингом (purchase-by-list-booking-test)".ybm_run { _ in
            defaultState.setExperiments(experiments: [.purchaseByLisBookingTest])
            stateManager?.setState(newState: defaultState)
        }

        "Настраиваем стейт".ybm_run { _ in
            let offer = modify(FAPIOffer.default) {
                $0.delivery = .pickup
                $0.aggregateOfferInfo = AggregateInfo.hasBooking
            }
            skuState.setSkuInfoState(offer: offer)
            stateManager?.setState(newState: skuState)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем лейбл на КМ - Самовывоз через 1-2 часа".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.addToCartButton.element
            )
            XCTAssertTrue(
                sku.deliveryOptions.bookingPickup.title.isVisible,
                "Лейбл Самовывоз через 1-2 часа НЕ виден"
            )
            XCTAssertTrue(
                sku.deliveryOptions.bookingPickup.title.label.contains("Самовывоз через 1-2 часа"),
                "Текст лейбла Самовывоз через 1-2 часа НЕ совпадает"
            )
        }
    }

    // MARK: - Private

    private var toggleInfo: String {
        let name = FeatureNames.purchaseByListBooking.lowercased()
        let toggleAdditionalInfo = [
            name: [
                "bookingPickupTime": "Через 1-2 часа"
            ]
        ]
        guard let toggleInfosData = try? JSONSerialization.data(
            withJSONObject: toggleAdditionalInfo,
            options: .prettyPrinted
        )
        else { return "" }
        return String(data: toggleInfosData, encoding: .utf8) ?? ""
    }
}
