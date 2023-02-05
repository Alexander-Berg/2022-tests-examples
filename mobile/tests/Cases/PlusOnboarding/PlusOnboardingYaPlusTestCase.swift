import MarketUITestMocks
import XCTest

class PlusOnboardingYaPlusTestCase: PlusOnboardingTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testOnboardingPlusWithoutBalanceTier1Tier2() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4230")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Плюсовик без баллов")
        Allure.addTitle("Плюсовик без баллов. Тир1/Тир2")

        let pages: [PageContent] = [
            PageContent(
                title: "Вы в Плюсе — можете\nкопить и тратить баллы",
                text: "Ими можно оплатить до 99% покупок\nна Маркете или в других сервисах Яндекса",
                buttonTitle: "А что с доставкой?"
            ),
            PageContent(
                title: "А за доставку \nвам платить не нужно",
                text: "Курьер бесплатно привезёт\nзаказы от 699 ₽",
                buttonTitle: "Здорово, а ещё?"
            )
        ]

        setZeroBalanceState()

        completeTestFlow(with: pages, bundleName: "PlusOnboarding_PlusWithoutBalanceTier1Tier2")
    }

    func testOnboardingPlusWithoutBalanceTier3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4229")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Плюсовик без баллов")
        Allure.addTitle("Плюсовик без баллов. Тир3")

        let pages: [PageContent] = [
            PageContent(
                title: "Вы в Плюсе — можете\nкопить и тратить баллы",
                text: "Ими можно оплатить до 99% покупок\nна Маркете или в других сервисах Яндекса",
                buttonTitle: "Здорово, а ещё?"
            )
        ]

        setZeroBalanceState()

        completeTestFlow(
            with: pages,
            bundleName: "PlusOnboarding_PlusWithoutBalanceTier3"
        )
    }

    func testOnboardingPlusWithBalanceTier1Tier2() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4231")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Плюсовик с баллами")
        Allure.addTitle("Плюсовик с баллами. Тир1/Тир2")

        let pages: [PageContent] = [
            PageContent(
                title: "Ваши баллы ждут,\nкогда их потратят",
                text: "Ими можно оплатить до 99% покупок\nна Маркете или в других сервисах Яндекса",
                buttonTitle: "А что с доставкой?"
            ),
            PageContent(
                title: "А за доставку \nвам платить не нужно",
                text: "Курьер бесплатно привезёт\nзаказы от 699 ₽",
                buttonTitle: "Здорово, а ещё?"
            )
        ]

        setNoZeroBalanceState()

        completeTestFlow(with: pages, bundleName: "PlusOnboarding_PlusWithBalanceTier1Tier2")
    }

    func testOnboardingPlusWithBalanceTier3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4232")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Плюсовик с баллами")
        Allure.addTitle("Плюсовик с баллами. Тир3")

        let pages: [PageContent] = [
            PageContent(
                title: "Ваши баллы ждут,\nкогда их потратят",
                text: "Ими можно оплатить до 99% покупок\nна Маркете или в других сервисах Яндекса",
                buttonTitle: "Здорово, а ещё?"
            )
        ]

        setNoZeroBalanceState()

        completeTestFlow(
            with: pages,
            bundleName: "PlusOnboarding_PlusWithBalanceTier3"
        )
    }
}
