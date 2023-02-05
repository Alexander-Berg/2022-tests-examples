import MarketUITestMocks
import XCTest

class PlusOnboardingAuthTestCase: PlusOnboardingTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testOnboardingNotPlusWithoutBalanceTier1Tier2() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4226")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Неплюсовик без баллов")
        Allure.addTitle("Неплюсовик без баллов. Тир1/Тир2")

        let pages: [PageContent] = [
            PageContent(
                title: "С кешбэком Плюса можно\nпокупать на 99% дешевле",
                text: "А ещё экономить на такси, развлечениях\nи покупках в других сервисах Яндекса",
                buttonTitle: "Неплохо, а как?"
            ),
            PageContent(
                title: "Просто выбирайте\nтовары со значком  ",
                text: "До 5% стоимости вернётся баллами Плюса.\n1 балл это 1 рубль.",
                buttonTitle: "А дальше?"
            ),
            PageContent(
                title: "Подключите Плюс, чтобы\nкопить и тратить баллы",
                text: "И не платить за доставку заказов\nкурьером, если они стоят 699 ₽ или больше\n(зависит от города)",
                buttonTitle: "Пойду подключу"
            )
        ]

        setZeroBalanceState()

        completeTestFlow(with: pages, bundleName: "PlusOnboarding_WithoutBalanceTier1Tier2")
    }

    func testOnboardingNotPlusWithoutBalanceTier3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4225")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Неплюсовик без баллов")
        Allure.addTitle("Неплюсовик без баллов. Тир3")

        let pages: [PageContent] = [
            PageContent(
                title: "С кешбэком Плюса можно\nпокупать на 99% дешевле",
                text: "А ещё экономить на такси, развлечениях\nи покупках в других сервисах Яндекса",
                buttonTitle: "Неплохо, а как?"
            ),
            PageContent(
                title: "Просто выбирайте\nтовары со значком  ",
                text: "До 5% стоимости вернётся баллами Плюса.\n1 балл это 1 рубль.",
                buttonTitle: "А дальше?"
            ),
            PageContent(
                title: "Подключите Плюс, чтобы\nкопить и тратить баллы",
                text: "С кешбэком Плюса покупки станут на 99%\nдешевле",
                buttonTitle: "Пойду подключу"
            )
        ]

        setZeroBalanceState()

        completeTestFlow(
            with: pages,
            bundleName: "PlusOnboarding_WithoutBalanceTier3"
        )
    }

    func testOnboardingNotPlusWithBalanceTier1Tier2() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4228")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Неплюсовик с баллами")
        Allure.addTitle("Неплюсовик с баллами. Тир1/Тир2")

        let pages: [PageContent] = [
            PageContent(
                title: "Баллы ждут,\nкогда их потратят",
                text: "Их можно списывать на Маркете\nи экономить до 99% на покупках",
                buttonTitle: "Я не против, а как?"
            ),
            PageContent(
                title: "Подключите Плюс, чтобы\nкопить и тратить баллы",
                text: "И не платить за доставку заказов\nкурьером, если они стоят 699 ₽ или больше\n(зависит от города)",
                buttonTitle: "Пойду подключу"
            )
        ]

        setNoZeroBalanceState()

        completeTestFlow(with: pages, bundleName: "PlusOnboarding_WithBalanceTier1Tier2")
    }

    func testOnboardingNotPlusWithBalanceTier3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4227")
        Allure.addEpic("Онбординг Яндекс Плюса")
        Allure.addFeature("Неплюсовик с баллами")
        Allure.addTitle("Неплюсовик с баллами. Тир3")

        let pages: [PageContent] = [
            PageContent(
                title: "Баллы ждут,\nкогда их потратят",
                text: "Их можно списывать на Маркете\nи экономить до 99% на покупках",
                buttonTitle: "Я не против, а как?"
            ),
            PageContent(
                title: "Подключите Плюс, чтобы\nкопить и тратить баллы",
                text: "С кешбэком Плюса покупки станут на 99%\nдешевле",
                buttonTitle: "Пойду подключу"
            )
        ]

        setNoZeroBalanceState()

        completeTestFlow(
            with: pages,
            bundleName: "PlusOnboarding_WithBalanceTier3"
        )
    }
}
