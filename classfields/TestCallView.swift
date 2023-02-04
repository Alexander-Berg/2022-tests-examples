#if DEBUG

import SwiftUI
import AutoRuCalls

public struct TestCallView: View {
    @ObservedObject var storage: TestCallStorage
    let testCallService: TestCallService

    public init(storage: TestCallStorage, testCallService: TestCallService) {
        self.storage = storage
        self.testCallService = testCallService
    }

    public var body: some View {
        List {
            ForEach(storage.presets) { preset in
                NavigationLink(
                    preset.name,
                    destination: TestCallPresetView(
                        preset: preset,
                        storage: storage,
                        testCallService: testCallService
                    )
                )
                .contextMenu(menuItems: {
                    Button("Duplicate") {
                        storage.duplicate(preset)
                    }
                })
            }
            .onDelete { indexSet in
                storage.remove(atOffsets: indexSet)
            }
        }
        .navigationBarTitle("Test call", displayMode: .large)
        .navigationBarItems(
            trailing:
                Button {
                    storage.addNew()
                } label: {
                    Image(systemName: "plus")
                }
        )
    }
}

#endif
