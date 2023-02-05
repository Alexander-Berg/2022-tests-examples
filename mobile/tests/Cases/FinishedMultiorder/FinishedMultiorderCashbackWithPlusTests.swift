import MarketUITestMocks
import XCTest
import YandexPlusHome

final class FinishedMultiorderCashbackWithPlusTests: OrderWithCashbackFlow {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testCashbackInSummaryInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3609")
        Allure.addEpic("Экран Спасибо")
        Allure.addFeature("Инфо о баллах за заказ")
        Allure.addTitle("Заказ с кэшбэком. Инфо в \"Подробности\"")

        disable(toggles: FeatureNames.cartRedesign)

        let oldBundleName = "FinishedMultiorderNoPlusWithBonus"
        let newBundleName = "New" + oldBundleName

        "Мокаем данные".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: oldBundleName,
                newBundleName: newBundleName,
                filename: "POST_api_v1_resolveCurrentUser,resolveUserValidPhones,resolveHasYaPlus",
                changes: [
                    (
                        #""hasYaPlus" : false"#,
                        #""hasYaPlus" : true"#
                    )
                ]
            )

            var state = MultiorderCashbackDetailsState()
            state.setOrdersCashbackDetails(with: 39, orderId: 32_753_872)
            stateManager?.setState(newState: state)
        }

        let finishPage = makeOrder(with: oldBundleName, newBundleName)

        "Смотрим текст на блоке кешбэка на спасибке".ybm_run { _ in
            wait(forExistanceOf: finishPage.plusBadgeText)
            finishPage.element.swipe(to: .down, untilVisible: finishPage.plusBadgeText)
            XCTAssertEqual(
                finishPage.plusBadgeText.label,
                "Баллы придут вместе с заказом.\nПотратьте их на следующую покупку."
            )
        }

        "Проверяем количество баллов на бейдже плюса".ybm_run { _ in
            wait(forExistanceOf: finishPage.plusBadge)
            XCTAssertEqual(
                finishPage.plusBadge.label,
                "39"
            )
        }

        "Открываем детальную информацию".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.detailSection)
            finishPage.detailSection.tap()
        }

        "Проверяем информацию о кешбэке".ybm_run { _ in
            finishPage.element.swipe(to: .down, untilVisible: finishPage.cashbackSummary)

            XCTAssertEqual(
                finishPage.titleCashbackSummary.label,
                "Вернётся на Плюс"
            )
            XCTAssertEqual(
                finishPage.cashbackSummary.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "39 баллов"
            )
        }
    }
}
