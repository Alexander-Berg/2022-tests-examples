import AutoRuProtoModels
import Foundation

extension Auto_Api_Offer {
    private static let timestamp = UInt64(1608306504000) // 2020-12-18T15:48:24+00:00

    enum Tag: String {
        case goodDeal = "good_price", greatDeal = "excellent_price"
        case priceChange = "history_discount"
        case allowedForCredit = "allowed_for_credit"
        case hasDiscount = "discount_options"
    }

    func mutate(_ mutate: (inout Self) -> Void) -> Self {
        var copy = self
        mutate(&copy)
        return copy
    }

    func addTag(_ tag: Tag) -> Self {
        return mutate {
            $0.tags.append(tag.rawValue)
        }
    }

    func addTags(_ tags: Tag...) -> Self {
        return mutate {
            $0.tags.append(contentsOf: tags.map { $0.rawValue })
        }
    }

    func addTags(_ tags: [Tag]) -> Self {
        return mutate {
            $0.tags.append(contentsOf: tags.map { $0.rawValue })
        }
    }

    // MARK: -

    func setNew() -> Self {
        return mutate {
            $0.section = .new
        }
    }

    func setUsed() -> Self {
        return mutate {
            $0.section = .used
        }
    }

    func setDealer() -> Self {
        return mutate {
            $0.sellerType = .commercial
        }
    }

    func setHighlightedPriceVAS() -> Self {
        return mutate {
            $0.services.append(
                Auto_Api_PaidService.with { (service: inout Auto_Api_PaidService) in
                    service.service = "all_sale_color"
                }
            )
        }
    }

    // MARK: -

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

    func addSharkInfo() -> Self {
        return mutate {
            $0.sharkInfo.precondition.monthlyPayment = 61_100
        }
    }

    func addPriceHistory(price: Int, date: Date = Date()) -> Self {
        return mutate {
            $0.priceHistory.append(
                Auto_Api_PriceInfo.with { (priceInfo: inout Auto_Api_PriceInfo) in
                    priceInfo.price = Float(price)
                    priceInfo.currency = "RUR"
                    priceInfo.createTimestamp = Self.timestamp
                    priceInfo.rurPrice = Float(price)
                    priceInfo.dprice = Double(price)
                    priceInfo.rurDprice = Double(price)
                }
            )
        }
    }

    func setPriceHistory(price: Int, date: Date = Date()) -> Self {
        return mutate {
            $0.priceHistory = [
                Auto_Api_PriceInfo.with { (priceInfo: inout Auto_Api_PriceInfo) in
                    priceInfo.price = Float(price)
                    priceInfo.currency = "RUR"
                    priceInfo.createTimestamp = Self.timestamp
                    priceInfo.rurPrice = Float(price)
                    priceInfo.dprice = Double(price)
                    priceInfo.rurDprice = Double(price)
                }
            ]
        }
    }

    func setMaxDiscount() -> Self {
        return mutate {
            $0 = $0.setDiscount(
                tradein: 170_000,
                insurance: 50000,
                credit: 100_000,
                max: 320_000
            )
        }
    }

    func setDiscount(tradein: Int? = nil, insurance: Int? = nil, credit: Int? = nil, max: Int? = nil) -> Self {
        return mutate { offer in
            tradein.flatMap { val in
                offer.discountOptions.tradein = Int32(val)
            }
            tradein.flatMap { val in
                offer.discountOptions.tradein = Int32(val)
            }
            credit.flatMap { val in
                offer.discountOptions.credit = Int32(val)
            }
            insurance.flatMap { val in
                offer.discountOptions.insurance = Int32(val)
            }
            max.flatMap { val in
                offer.discountOptions.maxDiscount = Int32(val)
            }
        }
    }

    func addPanorama() -> Self {
        return mutate {
            $0.state.externalPanorama = .with { externalPanorama in
                externalPanorama.published = .with { panorama in
                    panorama.id = "2440700553-1608235825301-EYkFg"
                    panorama.published = true
                    panorama.status = .completed
                    panorama.picturePng.count = 105
                    panorama.qualityR16X9 = 0.83738555311060103
                    panorama.qualityR4X3 = 0.83738555311060103
                    panorama.videoMp4R16X9.fullURL = "https://autoru-panorama.s3.mdst.yandex.net/artifacts/244/070/2440700553-1608235825301-EYkFg/video_mp4_r16x9/n3/v1/1280.mp4"
                    panorama.videoH264.fullURL = "https://autoru-panorama.s3.mdst.yandex.net/artifacts/244/070/2440700553-1608235825301-EYkFg/video_mp4/n3/v1/1200.mp4"
                    panorama.videoH264.previewURL = "https://autoru-panorama.s3.mdst.yandex.net/artifacts/244/070/2440700553-1608235825301-EYkFg/video_mp4/n3/v1/320.mp4"
                    panorama.preview.data = Data(base64Encoded: #"AAwDAQACEQMRAD8A+vtQ/Z9+DkcEUj6glrpm1g0j6ntIYYI25BB6HIzXxL8R9W+CmjR6zp2oN4l1LWDGEtVsZrfyz6liQMe2ATWL8adR8S/EHWhNoYtbeNLf7Oxn+cMOcn5shTzwVAP4147feGPCVjqLalq2rWwtoSkRs47wyojhRnc4wckhjjiuWhiHVV5P5I2xNBUZWiin+0/8OvCngvwV4dudDOrPe3pjuJWvUxBteItiNggDY+XkMevQYoqP9qv9oW0+KPg/w3odnew3iaXM7gRqflBQKOehHFFdOqXvHM7PVKx7ZbSMIXO452+tfHnjZ2HhXUhuPOq56/7LUUV5uX7yPXzH4keY5J6miiivRPIP/9k="#)!
                    panorama.pictureWebp.fullFirstFrame = "https://autoru-panorama.s3.mdst.yandex.net/artifacts/244/070/2440700553-1608235825301-EYkFg/image_webp/n3/v1/1200_0000.webp"
                }
            }
        }
    }

    func addVideo() -> Self {
        return mutate {
            $0.state.video = .with { video in
                video.url = "//www.youtube.com/watch?v=2Iz13zbQNEE"
                video.previews["full"] = "img.youtube.com/vi/2Iz13zbQNEE/hqdefault.jpg"
                video.previews["small"] = "img.youtube.com/vi/2Iz13zbQNEE/default.jpg"
                video.youtubeID = "2Iz13zbQNEE"
            }
        }
    }

    func setBookedByMe(_ isMe: Bool = true) -> Self {
        return mutate {
            $0.additionalInfo.booking = Auto_Api_AdditionalInfo.Booking.with { booking in
                booking.allowed = true
                booking.state = Auto_Api_AdditionalInfo.Booking.State.with { state in
                    state.booked = Auto_Api_AdditionalInfo.Booking.State.Booked.with { info in
                        info.bookedBy = .itsMe(true)
                        info.itsMe = isMe
                    }
                }
            }
        }
    }

    func setBookingAllowed(_ allowed: Bool = true) -> Self {
        return mutate {
            $0.additionalInfo.booking = Auto_Api_AdditionalInfo.Booking.with { booking in
                booking.allowed = allowed
            }
        }
    }

    func addDeliveryInfo() -> Self {
        return mutate {
            $0.seller.location.regionInfo.genitive = "Балашихи"
            $0.deliveryInfo = .with { deliveryInfo in
                deliveryInfo.deliveryRegions = [.with { _ in } ]
            }
        }
    }
}
