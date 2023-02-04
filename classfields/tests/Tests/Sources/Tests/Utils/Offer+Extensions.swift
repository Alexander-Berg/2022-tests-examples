//
//  Offer+Extensions.swift
//  Tests
//
//  Created by Alexander Kolovatov on 17.10.2021.
//

import Foundation
import AutoRuProtoModels

extension Auto_Api_Offer {
    private static let timestamp = UInt64(1608306504000) // 2020-12-18T15:48:24+00:00

    enum Tag: String {
        case goodDeal = "good_price"
        case greatDeal = "excellent_price"
        case priceChange = "history_discount"
        case allowedForCredit = "allowed_for_credit"
        case hasDiscount = "discount_options"
    }

    func mutate(_ mutate: (inout Self) -> Void) -> Self {
        var copy = self
        mutate(&copy)
        return copy
    }

    @discardableResult
    func addTag(_ tag: Tag) -> Auto_Api_Offer {
        var copy = self
        copy.tags.append(tag.rawValue)
        return copy
    }

    @discardableResult
    func addTags(_ tags: Tag...) -> Auto_Api_Offer {
        var copy = self
        copy.tags.append(contentsOf: tags.map { $0.rawValue })
        return copy
    }

    // MARK: -

    @discardableResult
    func setNew() -> Auto_Api_Offer {
        var copy = self
        copy.section = .new
        return copy
    }

    @discardableResult
    func setUsed() -> Auto_Api_Offer {
        var copy = self
        copy.section = .used
        return copy
    }

    @discardableResult
    func setDealer() -> Auto_Api_Offer {
        var copy = self
        copy.sellerType = .commercial
        return copy
    }

    @discardableResult
    func setHighlightedPriceVAS() -> Auto_Api_Offer {
        var copy = self
        copy.services.append(
            Auto_Api_PaidService.populate { service in
                service.service = "all_sale_color"
            }
        )
        return copy
    }

    @discardableResult
    func addPriceHistory(price: Int, date: Date = Date()) -> Auto_Api_Offer {
        var copy = self

        copy.priceHistory.append(
            Auto_Api_PriceInfo.populate { priceInfo in
                priceInfo.price = Float(price)
                priceInfo.currency = "RUR"
                priceInfo.createTimestamp = Self.timestamp
                priceInfo.rurPrice = Float(price)
                priceInfo.dprice = Double(price)
                priceInfo.rurDprice = Double(price)
            }
        )

        return copy
    }

    func configureUserInfo(_ sellerName: String) -> Self {
        return mutate { offer in
            offer.seller.name = sellerName
            offer.sellerType = .commercial
            offer.seller.chatsEnabled = true
            offer.seller.location.regionInfo.name = "Санкт-Петербург"
            offer.seller.location.metro = [
                .with { metro in
                    metro.name = "Театральная"
                    metro.lines = [.with { line in
                        line.color = "#FFAA00"
                        line.name = "Оранжевая"
                    }]
                }
            ]
        }
    }

    @discardableResult
    func addSharkInfo() -> Auto_Api_Offer {
        var copy = self
        copy.sharkInfo.precondition.monthlyPayment = 61_100
        return copy
    }

    @discardableResult
    func setMaxDiscount() -> Auto_Api_Offer {
        var copy = self
        copy.setDiscount(
            tradein: 170_000,
            insurance: 50000,
            credit: 100_000,
            max: 320_000
        )
        return copy
    }

    @discardableResult
    mutating func setDiscount(tradein: Int? = nil, insurance: Int? = nil, credit: Int? = nil, max: Int? = nil) -> Self {
        tradein.flatMap { val in
            discountOptions.tradein = Int32(val)
        }
        tradein.flatMap { val in
            discountOptions.tradein = Int32(val)
        }
        credit.flatMap { val in
            discountOptions.credit = Int32(val)
        }
        insurance.flatMap { val in
            discountOptions.insurance = Int32(val)
        }
        max.flatMap { val in
            discountOptions.maxDiscount = Int32(val)
        }
        return self
    }

    @discardableResult
    func setBookedByMe(_ isMe: Bool = true) -> Auto_Api_Offer {
        var copy = self
        copy.additionalInfo.booking = Auto_Api_AdditionalInfo.Booking.populate { booking in
            booking.allowed = true
            booking.state = Auto_Api_AdditionalInfo.Booking.State.populate { state in
                state.booked = Auto_Api_AdditionalInfo.Booking.State.Booked.populate { info in
                    info.bookedBy = .itsMe(true)
                    info.itsMe = isMe
                }
            }
        }
        return copy
    }

    @discardableResult
    func setBookingAllowed(_ allowed: Bool = true) -> Auto_Api_Offer {
        var copy = self
        copy.additionalInfo.booking = Auto_Api_AdditionalInfo.Booking.populate { booking in
            booking.allowed = allowed
        }
        return copy
    }

    @discardableResult
    func addDeliveryInfo() -> Auto_Api_Offer {
        var copy = self
        copy.seller.location.regionInfo.genitive = "Балашихи"
        copy.deliveryInfo = .with { deliveryInfo in
            deliveryInfo.deliveryRegions = [.with { _ in } ]
        }
        return copy
    }
}
