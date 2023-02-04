#if DEBUG

import SwiftUI
import AutoRuCalls

struct TestCallPresetView: View {
    @State var preset: TestCallPreset
    @ObservedObject var storage: TestCallStorage
    let testCallService: TestCallService
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        List {
            Section(header: Text(verbatim: "Preset name")) {
                TextField("Name", text: $preset.name).disableAutocorrection(true)
            }

            Section(header: Text(verbatim: "Vox username")) {
                TextField("Vox username", text: $preset.voxUsername).disableAutocorrection(true)
            }

            Section(header: Text(verbatim: "Payload")) {
                Picker("", selection: $preset.mode) {
                    ForEach(TestPayloadMode.allCases, id: \.self) { mode in
                        Text(verbatim: mode.rawValue).tag(mode)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())

                switch preset.mode {
                case .offer:
                    OfferPayloadView(payload: $preset.offerPayload, storage: storage)

                case .generic:
                    GenericPayloadView(payload: $preset.genericPayload, storage: storage)

                case .raw:
                    RawPayloadView(payload: $preset.rawPayload)
                }
            }

            Section {
                Button {
                    storage.update(preset)
                    testCallService.makeCall(preset)
                } label: {
                    HStack {
                        Image(systemName: "phone.arrow.up.right")
                        Text(verbatim: "Позвонить")
                    }
                }

            }
        }
        .listStyle(GroupedListStyle())
        .navigationBarTitle("", displayMode: .inline)
        .onDisappear {
            storage.update(preset)
        }
    }
}

struct OfferPayloadView: View {
    @Binding var payload: TestOfferPayload
    let storage: TestCallStorage

    var body: some View {
        HStack {
            TextSettingView(title: "redirect_id", value: $payload.redirectID)
            Button("last") {
                payload.redirectID = storage.lastRedirectID ?? ""
            }
        }

        Group {
            TextSettingView(title: "alias", value: $payload.alias)
            TextSettingView(title: "aliasAndSubject", value: $payload.aliasAndSubject)
            TextSettingView(title: "user_pic", value: $payload.userPic)
            TextSettingView(title: "offer_mark", value: $payload.mark)
            TextSettingView(title: "offer_model", value: $payload.model)
            TextSettingView(title: "offer_generation", value: $payload.generation)
            TextSettingView(title: "offer_year", value: $payload.year)
            TextSettingView(title: "offer_pic", value: $payload.pic)
            TextSettingView(title: "offer_link", value: $payload.link)
            TextSettingView(title: "offer_price", value: $payload.price)
        }

        Group {
            TextSettingView(title: "offer_price_currency", value: $payload.currency)
        }
    }
}

struct GenericPayloadView: View {
    @Binding var payload: TestGenericPayload
    let storage: TestCallStorage

    var body: some View {
        HStack {
            TextSettingView(title: "redirect_id", value: $payload.redirectID)
            Button("last") {
                payload.redirectID = storage.lastRedirectID ?? ""
            }
        }

        Group {
            TextSettingView(title: "alias", value: $payload.alias)
            TextSettingView(title: "aliasAndSubject", value: $payload.aliasAndSubject)
            TextSettingView(title: "userPic", value: $payload.userPic)
            TextSettingView(title: "image", value: $payload.image)
            TextSettingView(title: "url", value: $payload.url)
            TextSettingView(title: "line1", value: $payload.line1)
            TextSettingView(title: "line2", value: $payload.line2)
            TextSettingView(title: "handle", value: $payload.handle)
            TextSettingView(title: "subjectType", value: $payload.subjectType)
        }
    }
}

struct RawPayloadView: View {
    @Binding var payload: TestRawPayload

    var body: some View {
        let items = itemsWithEmptyItem()

        ForEach(items.indices, id: \.self) { index in
            HeaderAndTextSettingView(
                header: $payload.items[index, orAppend: .init()].header,
                value: $payload.items[index, orAppend: .init()].value
            )
        }
        .onDelete(perform: { indexSet in
            payload.items.remove(atOffsets: indexSet)
        })
    }

    private func itemsWithEmptyItem() -> [TestRawPayload.Item] {
        var items = payload.items

        if let last = items.last {
            if !last.header.isEmpty || !last.value.isEmpty {
                items.append(TestRawPayload.Item())
            }
        } else {
            items.append(TestRawPayload.Item())
        }

        return items
    }
}

struct TextSettingView: View {
    let title: String
    @Binding var value: String

    var body: some View {
        HStack {
            Text(title)
            TextField(title, text: $value)
                .multilineTextAlignment(.trailing)
                .disableAutocorrection(true)
                .autocapitalization(.none)
        }
    }
}

struct HeaderAndTextSettingView: View {
    @Binding var header: String
    @Binding var value: String

    var body: some View {
        HStack {
            TextField("Header", text: $header)
                .disableAutocorrection(true)
                .autocapitalization(.none)
            TextField("Value", text: $value)
                .multilineTextAlignment(.trailing)
                .disableAutocorrection(true)
                .autocapitalization(.none)
        }
    }
}

extension Array {
    subscript(_ index: Index, orAppend element: Element) -> Element {
        get {
            if indices.contains(index) {
                return self[index]
            }

            return element
        }
        set {
            if indices.contains(index) {
                self[index] = newValue
            } else if endIndex == index {
                self.append(newValue)
            } else {
                preconditionFailure()
            }
        }
    }
}

#endif
