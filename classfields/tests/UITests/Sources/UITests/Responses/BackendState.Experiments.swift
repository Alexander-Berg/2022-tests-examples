import AutoRuProtoModels
import Foundation

extension BackendState {
    struct Experiments {
        private struct Variant {
            var jsonPayload: String
            var testID: Int
        }

        private var variants: [Variant] = []

        var flags: [String] {
            variants.map(mapToBase64Flag(_:))
        }

        mutating func add<Exp: Encodable>(exp: Exp, testID: Int = .random(in: 0 ... Int.max)) {
            try! variants.append(
                Variant(
                    jsonPayload: String(bytes: JSONEncoder().encode(exp), encoding: .utf8)!,
                    testID: testID
                )
            )
        }

        private func mapToBase64Flag(_ variant: Variant) -> String {
            let exp = [
                [
                    "HANDLER": "AUTORU_APP",
                    "CONTEXT": [
                        "MAIN": [
                            "AUTORU_APP": "$"
                        ]
                    ],
                    "TESTID": ["\(variant.testID)"]
                ]
            ]

            let template = try! String(
                data: JSONSerialization.data(withJSONObject: exp, options: []),
                encoding: .utf8
            )!

            let jsonString = template.replacingOccurrences(of: "\"$\"", with: variant.jsonPayload)

            let base64 = jsonString.data(using: .utf8)!.base64EncodedData()

            return String(data: base64, encoding: .utf8)!
        }
    }
}

extension BackendState.Experiments {
    struct HideApp2AppCallOption: Encodable {
        var app2app_hide_call_option = true
    }

    struct App2AppAskMic: Encodable {
        var app2app_ask_mic = true
    }

    struct CreditNewPromo: Encodable {
        var credit_new_promo = true
    }

    struct HideCreditPercents: Encodable {
        var no_loan_rate_credit_feature = true
    }

    struct ShowCreditBannerInFavourites: Encodable {
        var credit_banner_in_favourites = true
    }

    struct HideCreditBannerInFavourites: Encodable {
        var credit_banner_in_favourites = false
    }

    struct FavoriteButtonFirst: Encodable {
        var favorite_button_first = true
    }
}

extension BackendState.Experiments {
    func toMockSource() -> MockSource<Auto_Api_HelloRequest, Auto_Api_HelloResponse> {
        .model { msg in
            msg.experimentsConfig = .with { msg in
                msg.flags = flags
            }

            msg.status = .success
        }
        .withHeader("x-yandex-expflags", value: flags.joined(separator: ","))
    }
}
