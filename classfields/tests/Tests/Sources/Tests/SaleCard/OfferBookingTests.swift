@testable import AutoRuOfferBooking
import AutoRuUtils
import Foundation
import XCTest

final class OfferBookingTests: BaseUnitTest {
    func test_bannerFormatting() {
        let cal = Calendar.russian

        do {
            let jun20EndDate = DateComponents(calendar: cal, year: 2_030, month: 6, day: 20, hour: 12, minute: 0, second: 0).date!

            XCTAssertEqual(
                OfferBookingFormatter.bannerTitle(endDate: jun20EndDate, isOwner: false, byMe: false),
                "Забронирован"
            )
            XCTAssertEqual(
                OfferBookingFormatter.bannerTitle(endDate: jun20EndDate, isOwner: false, byMe: true),
                "Забронирован вами"
            )
            XCTAssertEqual(
                OfferBookingFormatter.bannerTitle(endDate: jun20EndDate, isOwner: true, byMe: false),
                "Ваш авто забронирован"
            )

            XCTAssertEqual(
                OfferBookingFormatter.bannerSubtitle(endDate: jun20EndDate, byMe: false),
                "До 20 июня"
            )
            XCTAssertEqual(
                OfferBookingFormatter.bannerSubtitle(endDate: jun20EndDate, byMe: true),
                "До 20 июня"
            )
        }

        do {
            var todayEndDate = Date()
            todayEndDate = cal.date(bySetting: .hour, value: 23, of: todayEndDate)!
            todayEndDate = cal.date(bySetting: .minute, value: 59, of: todayEndDate)!

            XCTAssertEqual(
                OfferBookingFormatter.bannerTitle(endDate: todayEndDate, isOwner: false, byMe: false),
                "Забронирован"
            )
            XCTAssertEqual(
                OfferBookingFormatter.bannerTitle(endDate: todayEndDate, isOwner: false, byMe: true),
                "Сегодня последний день бронирования"
            )

            XCTAssertEqual(
                OfferBookingFormatter.bannerSubtitle(endDate: todayEndDate, byMe: false),
                "До \(todayEndDate.format(.dayAndMonth))"
            )
            XCTAssertEqual(
                OfferBookingFormatter.bannerSubtitle(endDate: todayEndDate, byMe: true),
                nil
            )
        }
    }

    func test_statusFormatting() {
        let cal = Calendar.russian

        do {
            let jun20EndDate = DateComponents(calendar: cal, year: 2_030, month: 6, day: 20, hour: 12, minute: 0, second: 0).date!

            XCTAssertEqual(
                BookingStatusFormatter.title(endDate: jun20EndDate, isMyOffer: false, byMe: false),
                "Авто забронировано до 20 июня"
            )
            XCTAssertEqual(
                BookingStatusFormatter.title(endDate: jun20EndDate, isMyOffer: false, byMe: true),
                "Вы забронировали этот автомобиль до 20 июня"
            )

            XCTAssertEqual(
                BookingStatusFormatter.subtitle(endDate: jun20EndDate, isMyOffer: false, byMe: false),
                "Другой пользователь оформил бронь на эту машину. Если сделка не состоится до 20 июня, вы сможете купить этот автомобиль. Или\(String.nbsp)выбрать другой."
            )
            XCTAssertEqual(
                BookingStatusFormatter.subtitle(endDate: jun20EndDate, isMyOffer: false, byMe: true),
                "Можно ехать в дилерский центр и выкупать машину. В автосалоне покажите смс с подтверждением бронирования, которое мы отправили на ваш номер. Для отмены бронирования свяжитесь с дилером или обратитесь в техподдержку."
            )
        }

        do {
            var todayEndDate = Date()
            todayEndDate = cal.date(bySetting: .hour, value: 23, of: todayEndDate)!
            todayEndDate = cal.date(bySetting: .minute, value: 59, of: todayEndDate)!

            XCTAssertEqual(
                BookingStatusFormatter.title(endDate: todayEndDate, isMyOffer: false, byMe: false),
                "Авто забронировано до \(todayEndDate.format(.dayAndMonth))"
            )
            XCTAssertEqual(
                BookingStatusFormatter.title(endDate: todayEndDate, isMyOffer: false, byMe: true),
                "Сегодня последний день бронирования"
            )
            XCTAssertEqual(
                BookingStatusFormatter.title(endDate: todayEndDate, isMyOffer: true, byMe: false),
                "Ваш автомобиль забронирован"
            )

            XCTAssertEqual(
                BookingStatusFormatter.subtitle(endDate: todayEndDate, isMyOffer: false, byMe: false),
                "Другой пользователь оформил бронь на эту машину. Если сделка не состоится до \(todayEndDate.format(.dayAndMonth)), вы сможете купить этот автомобиль. Или\(String.nbsp)выбрать другой."
            )
            XCTAssertEqual(
                BookingStatusFormatter.subtitle(endDate: todayEndDate, isMyOffer: false, byMe: true),
                "Выкупить автомобиль нужно сегодня, иначе бронь сгорит. Покажите в автосалоне смс с подтверждением бронирования, которое мы отправили на ваш номер."
            )
            XCTAssertEqual(
                BookingStatusFormatter.subtitle(endDate: todayEndDate, isMyOffer: true, byMe: false),
                "Свяжитесь быстрее с покупателем и обновите статус заявки в личном кабинете."
            )
        }
    }
}
