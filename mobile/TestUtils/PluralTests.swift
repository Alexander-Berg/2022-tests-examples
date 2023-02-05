import XCTest
@testable import Beru

/// Plural Rules:
/// https://unicode-org.github.io/cldr-staging/charts/37/verify/numbers/ru.html

// swiftlint:disable l10n
final class PluralTests: XCTestCase {

    /// 0
    private let zero = 0
    /// 1
    private let one = 1
    /// 3
    private let few = 3
    /// 25
    private let many = 25

    func testCashback() {
        // given
        let oneForm = "\(one) балл"
        let fewForm = "\(few) балла"
        let manyForm = "\(many) баллов"

        // when
        let onePlural = L10n.cashback(one)
        let fewPlural = L10n.cashback(few)
        let manyPlural = L10n.cashback(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCashbackPlural() {
        // given
        let oneForm = "\(one) балл на Плюс"
        let fewForm = "\(few) балла на Плюс"
        let manyForm = "\(many) баллов на Плюс"

        // when
        let onePlural = L10n.cashbackPlural(one)
        let fewPlural = L10n.cashbackPlural(few)
        let manyPlural = L10n.cashbackPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCashbackSnippetPlural() {
        // given
        let oneForm = "\(one) балл"
        let fewForm = "\(few) балла"
        let manyForm = "\(many) баллов"

        // when
        let onePlural = L10n.cashbackSnippetPlural(one)
        let fewPlural = L10n.cashbackSnippetPlural(few)
        let manyPlural = L10n.cashbackSnippetPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCheckoutPaymentTypeCreditUnavailableItems() {
        // given
        let oneForm = "Недоступно для \(one) товара в заказе"
        let fewForm = "Недоступно для \(few) товаров в заказе"
        let manyForm = "Недоступно для \(many) товаров в заказе"

        // when
        let onePlural = L10n.checkoutPaymentTypeCreditUnavailableItems(one)
        let fewPlural = L10n.checkoutPaymentTypeCreditUnavailableItems(few)
        let manyPlural = L10n.checkoutPaymentTypeCreditUnavailableItems(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testPlainProductPlural() {
        // given
        let oneForm = "\(one) товар"
        let fewForm = "\(few) товара"
        let manyForm = "\(many) товаров"

        // when
        let onePlural = L10n.ybmPlainProductPlural(one)
        let fewPlural = L10n.ybmPlainProductPlural(few)
        let manyPlural = L10n.ybmPlainProductPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testProductPlural() {
        // given
        let oneForm = "\(one) вариант"
        let fewForm = "\(few) варианта"
        let manyForm = "\(many) вариантов"

        // when
        let onePlural = L10n.ybmProductPlural(one)
        let fewPlural = L10n.ybmProductPlural(few)
        let manyPlural = L10n.ybmProductPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testRatingCountPlural() {
        // given
        let oneForm = "\(one) оценка"
        let fewForm = "\(few) оценки"
        let manyForm = "\(many) оценок"

        // when
        let onePlural = L10n.ratingCount(one)
        let fewPlural = L10n.ratingCount(few)
        let manyPlural = L10n.ratingCount(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testDaysPlural() {
        // given
        let oneForm = "\(one) день"
        let fewForm = "\(few) дня"
        let manyForm = "\(many) дней"

        // when
        let onePlural = L10n.ybmDaysPlural(one)
        let fewPlural = L10n.ybmDaysPlural(few)
        let manyPlural = L10n.ybmDaysPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testReviewsPlural() {
        // given
        let zeroForm = "Нет отзывов"
        let oneForm = "\(one) отзыв"
        let fewForm = "\(few) отзыва"
        let manyForm = "\(many) отзывов"

        // when
        let zeroPlural = L10n.ybmReviewsPlural(zero)
        let onePlural = L10n.ybmReviewsPlural(one)
        let fewPlural = L10n.ybmReviewsPlural(few)
        let manyPlural = L10n.ybmReviewsPlural(many)

        // then
        XCTAssertEqual(zeroPlural, zeroForm)
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testOutletCountPlural() {
        // given
        let oneForm = "\(one) пункт"
        let fewForm = "\(few) пункта"
        let manyForm = "\(many) пунктов"

        // when
        let onePlural = L10n.ybmOutletCountPlural(one)
        let fewPlural = L10n.ybmOutletCountPlural(few)
        let manyPlural = L10n.ybmOutletCountPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCartBucketsCountPlural() {
        // given
        let oneForm = "в \(one) посылке"
        let fewForm = "в \(few) посылках"
        let manyForm = "в \(many) посылках"

        // when
        let onePlural = L10n.cartBucketsCount(one)
        let fewPlural = L10n.cartBucketsCount(few)
        let manyPlural = L10n.cartBucketsCount(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testBoughtNTimesReasonToBuyPlural() {
        // given
        let oneForm = "\(one) покупка"
        let fewForm = "\(few) покупки"
        let manyForm = "\(many) покупок"

        // when
        let onePlural = L10n.ybmBoughtNTimesReasonToBuy(one)
        let fewPlural = L10n.ybmBoughtNTimesReasonToBuy(few)
        let manyPlural = L10n.ybmBoughtNTimesReasonToBuy(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testViewedNTimesReasonToBuyPlural() {
        // given
        let oneForm = "\(one) человек интересовался"
        let fewForm = "\(few) человека интересовались"
        let manyForm = "\(many) человек интересовались"

        // when
        let onePlural = L10n.ybmViewedNTimesReasonToBuy(one)
        let fewPlural = L10n.ybmViewedNTimesReasonToBuy(few)
        let manyPlural = L10n.ybmViewedNTimesReasonToBuy(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testShowMoreOpinionsPlural() {
        // given
        let oneForm = "Показать \(one) ответ"
        let fewForm = "Показать \(few) ответа"
        let manyForm = "Показать \(many) ответов"

        // when
        let onePlural = L10n.ybmShowMoreOpinionsCountTitle(one)
        let fewPlural = L10n.ybmShowMoreOpinionsCountTitle(few)
        let manyPlural = L10n.ybmShowMoreOpinionsCountTitle(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testShipmentTitle() {
        // given
        let oneForm = "\(one) посылки"
        let fewForm = "\(few) посылок"
        let manyForm = "\(many) посылок"

        // when
        let onePlural = L10n.shipmentTitle(one)
        let fewPlural = L10n.shipmentTitle(few)
        let manyPlural = L10n.shipmentTitle(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testQnAShowMoreAnswersPlural() {
        // given
        let oneForm = "Показать \(one) ответ"
        let fewForm = "Показать \(few) ответа"
        let manyForm = "Показать \(many) ответов"

        // when
        let onePlural = L10n.qnAShowMoreAnswers(one)
        let fewPlural = L10n.qnAShowMoreAnswers(few)
        let manyPlural = L10n.qnAShowMoreAnswers(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testQnAComments() {
        // given
        let oneForm = "\(one) комментарий"
        let fewForm = "\(few) комментария"
        let manyForm = "\(many) комментариев"

        // when
        let onePlural = L10n.qnAComments(one)
        let fewPlural = L10n.qnAComments(few)
        let manyPlural = L10n.qnAComments(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testQnAQuestionsCount() {
        // given
        let zeroForm = "Нет вопросов"
        let oneForm = "\(one) вопрос"
        let fewForm = "\(few) вопроса"
        let manyForm = "\(many) вопросов"

        // when
        let zeroPlural = L10n.qnAQuestionsCount(zero)
        let onePlural = L10n.qnAQuestionsCount(one)
        let fewPlural = L10n.qnAQuestionsCount(few)
        let manyPlural = L10n.qnAQuestionsCount(many)

        // then
        XCTAssertEqual(zeroPlural, zeroForm)
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCartCounterSkuBig() {
        // given
        let zeroForm = "Товар удалён"
        let oneForm = "\(one) товар в корзине"
        let fewForm = "\(few) товара в корзине"
        let manyForm = "\(many) товаров в корзине"

        // when
        let zeroPlural = L10n.cartCounterSkuBig(zero)
        let onePlural = L10n.cartCounterSkuBig(one)
        let fewPlural = L10n.cartCounterSkuBig(few)
        let manyPlural = L10n.cartCounterSkuBig(many)

        // then
        XCTAssertEqual(zeroPlural, zeroForm)
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCartCounterSkuSet() {
        // given
        let zeroForm = "Комплект удалён"
        let oneForm = "\(one) комплект в корзине"
        let fewForm = "\(few) комплекта в корзине"
        let manyForm = "\(many) комплектов в корзине"

        // when
        let zeroPlural = L10n.cartCounterSkuSet(zero)
        let onePlural = L10n.cartCounterSkuSet(one)
        let fewPlural = L10n.cartCounterSkuSet(few)
        let manyPlural = L10n.cartCounterSkuSet(many)

        // then
        XCTAssertEqual(zeroPlural, zeroForm)
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCartItemCounter() {
        // given
        let oneForm = "В наличии \(one) штука"
        let fewForm = "В наличии \(few) штуки"
        let manyForm = "В наличии \(many) штук"

        // when
        let onePlural = L10n.cartItemCounter(one)
        let fewPlural = L10n.cartItemCounter(few)
        let manyPlural = L10n.cartItemCounter(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCashbackUserAmountPlural() {
        // given
        let oneForm = "У вас \(one) балл "
        let fewForm = "У вас \(few) балла "
        let manyForm = "У вас \(many) баллов "

        // when
        let onePlural = L10n.cashbackUserAmountPlural(one)
        let fewPlural = L10n.cashbackUserAmountPlural(few)
        let manyPlural = L10n.cashbackUserAmountPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testCheckoutFinishedHelpIsNearPlural() {
        // given
        let oneForm = "\(one) перечислен в «Помощь рядом»"
        let fewForm = "\(few) перечислены в «Помощь рядом»"
        let manyForm = "\(many) перечислены в «Помощь рядом»"

        // when
        let onePlural = L10n.checkoutFinishedHelpIsNear(one)
        let fewPlural = L10n.checkoutFinishedHelpIsNear(few)
        let manyPlural = L10n.checkoutFinishedHelpIsNear(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testOrdersBnplUserStatisticsPaymentsLeftPlural() {
        // given
        let oneForm = "Остался \(one) платёж по оплате частями"
        let fewForm = "Осталось \(few) платежа по оплате частями"
        let manyForm = "Осталось \(many) платежей по оплате частями"

        // when
        let onePlural = L10n.ordersBNPLUserStatisticsPaymentsLeft(one)
        let fewPlural = L10n.ordersBNPLUserStatisticsPaymentsLeft(few)
        let manyPlural = L10n.ordersBNPLUserStatisticsPaymentsLeft(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testReferralProgramBonusGotPlural() {
        // given
        let oneForm = "из \(one) балла получено"
        let fewForm = "из \(few) баллов получено"
        let manyForm = "из \(many) баллов получено"

        // when
        let onePlural = L10n.referralProgramBonusAlreadyGot(one)
        let fewPlural = L10n.referralProgramBonusAlreadyGot(few)
        let manyPlural = L10n.referralProgramBonusAlreadyGot(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testReferralProgramBonusWaitingPlural() {
        // given
        let oneForm = "балл в ожидании"
        let fewForm = "балла в ожидании"
        let manyForm = "баллов в ожидании"

        // when
        let onePlural = L10n.referralProgramBonusWaiting(one)
        let fewPlural = L10n.referralProgramBonusWaiting(few)
        let manyPlural = L10n.referralProgramBonusWaiting(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testReferralProgramFriendsInvitedPlural() {
        // given
        let oneForm = "друг сделал заказ"
        let fewForm = "друга сделали заказ"
        let manyForm = "друзей сделали заказ"

        // when
        let onePlural = L10n.referralProgramFriendsInvited(one)
        let fewPlural = L10n.referralProgramFriendsInvited(few)
        let manyPlural = L10n.referralProgramFriendsInvited(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }

    func testOrdersCountPlural() {
        // given
        let oneForm = "\(one) заказ"
        let fewForm = "\(few) заказа"
        let manyForm = "\(many) заказов"

        // when
        let onePlural = L10n.ordersCountPlural(one)
        let fewPlural = L10n.ordersCountPlural(few)
        let manyPlural = L10n.ordersCountPlural(many)

        // then
        XCTAssertEqual(onePlural, oneForm)
        XCTAssertEqual(fewPlural, fewForm)
        XCTAssertEqual(manyPlural, manyForm)
    }
}
