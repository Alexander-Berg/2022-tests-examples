import MarketUITestMocks
import XCTest

final class ReferralPromocodeUsage: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testFriendPromocodeUsage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4495")
        Allure.addEpic("Реферальная программа")
        Allure.addFeature("Промокод")
        Allure.addTitle("Ошибка при использовании промокда для друга на своём аккаунте")

        var root: RootPage!
        var profile: ProfilePage!
        var referral: ReferralPromocodePage!
        var cart: CartPage!
        var sku: SKUPage!

        let endDate = makeStringRepresentation(of: Date().addingTimeInterval(.week))

        disable(toggles: FeatureNames.cartRedesign)

        "Изменяем дату окончания промокода на валидную".ybm_run { _ in
            mockStateManager?.changeMock(
                bundleName: "ReferralProgram",
                newBundleName: "ReferralProgramPromocodeDateUpdate",
                filename: "POST_api_v1_resolveReferralPromocode",
                changes: [
                    (
                        #""refererPromoCodeExpiredDate" : "2021-06-28T13:36:36.562790Z""#,
                        "\"refererPromoCodeExpiredDate\" : \"\(endDate.en)\""
                    )
                ]
            )
        }

        "Мокаем реферальную программу".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgram")
            mockStateManager?.pushState(bundleName: "ReferralProgramPromocodeDateUpdate")
            mockStateManager?.pushState(bundleName: "Experiments_ReferralProgram")
        }

        "Настраиваем FT".ybm_run { _ in
            app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)
            app.launchEnvironment[TestLaunchEnvironmentKeys.enabledToggles] = FeatureNames.referralProgram
        }

        "Авторизуемся, открываем профиль".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            profile = goToProfile(root: RootPage(element: app))
        }

        "Открываем окно реферальной программы".ybm_run { _ in
            profile.collectionView.swipe(to: .down, untilVisible: profile.referral.element)
            referral = profile.referral.tap()
            wait(forVisibilityOf: referral.element)
        }

        "Нажать на значок копирования".ybm_run { _ in
            let toast = referral.copyClipboard()
            wait(forVisibilityOf: toast.element)
            wait(forInvisibilityOf: toast.element)
        }

        "Нажать на значок закрытия экрана".ybm_run { _ in
            referral.close()
            // кнопку перекрывает системное окно
            ybm_recoverableWait(
                forFulfillmentOf: { profile.element.isVisible },
                timeout: 5,
                recoverBlock: { referral.close() },
                recoversCount: 2
            )
        }

        "Мокаем добавление".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_IPhone")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 1_732_171_388,
                skuId: 100_210_864_680,
                offerId: "go24rjhk7QUrlqh-5kNAmQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage(root: root)
        }

        "Мокаем переход в корзину".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_IPhoneInCart")
        }

        "Проверяем отображение кнопки \"Добавить в корзину\"".ybm_run { _ in
            sku.collectionView.ybm_swipeCollectionView(toFullyReveal: sku.addToCartButton.element)

            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.isVisible })
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "Добавить в корзину" })

            sku.addToCartButton.element.tap()
            ybm_wait(forFulfillmentOf: { sku.addToCartButton.element.label == "1 товар в корзине" })
        }

        "Переходим в корзину".ybm_run { _ in
            cart = goToCart(root: root)
            ybm_wait(forFulfillmentOf: {
                cart.element.isVisible
            })

            let addedGood = cart.cartItem(at: 0)
            ybm_wait(forFulfillmentOf: { addedGood.element.exists })
        }

        "Мокаем применение промокода".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "ReferralProgramPromocode")
        }

        "Применяем промокод".ybm_run { _ in
            cart.promocode.input.tapUnhittable()
            cart.promocode.input.typeText("SOME PROMO")
            cart.promocode.applyButton.tap()
        }

        "Проверяем отображение ошибки".ybm_run { _ in
            let popup = DefaultToastPopupPage.currentPopup
            wait(forVisibilityOf: popup.element)

            let errorMessage = "Этот промокод только для ваших друзей"
            XCTAssertEqual(popup.text.label, errorMessage)
            XCTAssertEqual(cart.promocode.error.label, errorMessage)
        }
    }
}

// MARK: - ReferralProgramMockHelper

extension ReferralPromocodeUsage: ReferralProgramMockHelper {}
