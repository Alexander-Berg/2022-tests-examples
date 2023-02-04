import XCTest

class PhoneCallsTests: BackendStatefulTests {

    private let disabledApp2AppUserDefaultsSettings: [String: Any] = ["app2appCallsAreEnabled": false]
    
    func testTapCallMetricCars() {
        testCallEvent(
            #"Тап по кнопке "Позвонить" (авто)"#,
            with: ["С пробегом": ["Частник": "Краснодар"], "Источник": "Карточка объявления", "Тег": "Не_растаможен"]
        )
    }

    func testTapCallMetricsMoto() {
        state.modifiers.offers[.bmw3g20, default: []].append { offer, _ in
            offer.category = .moto
        }

        testCallEvent(
            #"Тап по кнопке "Позвонить" (мото)"#,
            with: ["С пробегом": ["Частник": "Краснодар"], "Источник": "Карточка объявления"]
        )
    }

    func testTapCallMetricsTrucks() {
        state.modifiers.offers[.bmw3g20, default: []].append { offer, _ in
            offer.category = .trucks
        }

        testCallEvent(
            #"Тап по кнопке "Позвонить" (коммерческий транспорт)"#,
            with: ["С пробегом": ["Частник": "Краснодар"], "Источник": "Карточка объявления"]
        )
    }

    private func testCallEvent(_ eventName: String, with properties: [String: Any]) {
        launchMain(
            options: .init(
                overrideAppSettings: disabledApp2AppUserDefaultsSettings
            )
        )
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.tap(.offerCell(.alias(.bmw3g20)))
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen.focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                    $0.tap(.callButton)
                }
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .tap(.phoneOption)
            }
            .shouldEventBeReported(eventName, with: properties)
    }
}
