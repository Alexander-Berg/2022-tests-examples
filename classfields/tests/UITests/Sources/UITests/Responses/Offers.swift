import AutoRuProtoModels
import Foundation

func makeOffer(_ offer: BackendState.Offer, state: BackendState) -> Auto_Api_Offer {
    var result: Auto_Api_Offer

    switch offer {
    case .bmw3g20:
        result = makeBmw3G20()
    }

    result.id = offer.rawValue

    result.isFavorite = state.user.favorites.offers.contains(offer)

    for modifier in state.modifiers.offers[offer] ?? [] {
        modifier(&result, state)
    }

    return result
}

func makePhones(_ offer: BackendState.Offer) -> Auto_Api_PhoneResponse {
    switch offer {
    case .bmw3g20:
        return makeBmw3G20Phones()
    }
}

private func makeBmw3G20() -> Auto_Api_Offer {
    return .with { msg in
        msg.status = .active
        msg.category = .cars
        msg.section = .used
        msg.priceInfo = .with { msg in
            msg.price = 3990000
            msg.rurPrice = 3990000
            msg.dprice = 3990000
            msg.rurDprice = 3990000
            msg.currency = "RUR"
        }
        msg.colorHex = "040001"

        msg.carInfo = .with { msg in
            msg.bodyType = "SEDAN"
            msg.engineType = "DIESEL"
            msg.transmission = "AUTOMATIC"
            msg.horsePower = 265
            msg.markInfo = .with { msg in
                msg.name = "BMW"
            }
            msg.modelInfo = .with { msg in
                msg.name = "3 серии"
            }
            msg.superGen = .with { msg in
                msg.name = "VII (G2x)"
            }
            msg.techParam = .with { msg in
                msg.id = 21605467
                msg.name = "330"
                msg.nameplate = "330d xDrive"
                msg.displacement = 2993
                msg.engineType = "DIESEL"
                msg.gearType = "ALL_WHEEL_DRIVE"
                msg.transmission = "AUTOMATIC"
                msg.power = 265
                msg.powerKvt = 195
                msg.humanName = "330d xDrive 3.0d AT (265 л.с.) 4WD"
            }
        }
        msg.documents = .with { msg in
            msg.year = 2019
            msg.vinResolution = .ok
        }
        msg.state = .with { msg in
            msg.mileage = 12633
            msg.stateNotBeaten = true
            msg.imageUrls = [
                .with { msg in
                    msg.name = "1"
                    msg.sizes = [
                        "1200x900": "//avatars.mds.yandex.net/get-autoru-vos/5086608/bdf68470d58564240abef07eff063abe/1200x900"
                    ]
                },
                .with { msg in
                    msg.name = "2"
                    msg.sizes = [
                        "1200x900": "//avatars.mds.yandex.net/get-autoru-vos/4119679/29910bda0e50329674636bb04c9f0b41/1200x900"
                    ]
                }
            ]
        }
        msg.seller = .with { msg in
            msg.location = .with { msg in
                msg.regionInfo = .with { msg in
                    msg.name = "Краснодар"
                }
            }
            msg.phones = [
                .with { msg in
                    msg.callHourStart = 9
                    msg.callHourEnd = 19
                }
            ]
        }
        msg.created = .init(date: Date().adding(-10, component: .hour))
        msg.tags = ["vin_offers_history"]
    }
}

private func makeBmw3G20Phones() -> Auto_Api_PhoneResponse {
    return .with { msg in
        msg.phones = [
            .with { msg in
                msg.app2AppCallAvailable = true
                msg.app2AppPayload = [:]
                msg.callHourStart = 9
                msg.callHourEnd = 19
                msg.phone = "+7 123 456 78 90"
            },
            .with { msg in
                msg.app2AppCallAvailable = true
                msg.app2AppPayload = [:]
                msg.phone = "+7 098 765 43 21"
            }
        ]
        msg.redirectPhones = true
        msg.status = .success
    }
}
