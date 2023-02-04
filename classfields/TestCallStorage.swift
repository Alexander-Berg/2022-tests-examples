#if DEBUG

import Combine
import Foundation

public final class TestCallStorage: ObservableObject {
    private static let presetsKey = "TestCalls"
    private static let lastRedirectIDKey = "lastRedirectID"

    private static let defaults = UserDefaults.standard
    @Published public private(set) var presets: [TestCallPreset] = []

    public var lastRedirectID: String? {
        Self.defaults.string(forKey: Self.lastRedirectIDKey)
    }

    public init() {
        guard let data = Self.defaults.data(forKey: Self.presetsKey),
              let presets = try? JSONDecoder().decode([TestCallPreset].self, from: data)
        else {
            self.presets = [TestCallPreset(id: UUID(), name: "Call preset")]
            return
        }

        self.presets = presets
    }

    public func remove(atOffsets indexSet: IndexSet) {
        presets = presets.enumerated().filter { !indexSet.contains($0.offset) }.map(\.element)
        saveToDefaults()
    }

    public func duplicate(_ preset: TestCallPreset) {
        guard let index = presets.firstIndex(where: { $0.id == preset.id }) else { return }
        var newPreset = preset
        newPreset.id = UUID()
        presets.insert(newPreset, at: index + 1)
        saveToDefaults()
    }

    public func addNew() {
        let newPreset = TestCallPreset(
            id: UUID(),
            name: "Call preset \(presets.count)"
        )

        presets.append(newPreset)
        saveToDefaults()
    }

    public func update(_ preset: TestCallPreset) {
        guard let index = presets.firstIndex(where: { $0.id == preset.id }) else { return }
        presets[index] = preset

        saveToDefaults()
    }

    public static func setLastRedirectID(_ id: String) {
        defaults.set(id, forKey: lastRedirectIDKey)
    }

    private func saveToDefaults() {
        guard let data = try? JSONEncoder().encode(presets) else { return }

        Self.defaults.set(data, forKey: Self.presetsKey)
    }
}

#endif
