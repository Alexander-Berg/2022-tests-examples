import XCTest
import SwiftUI
import AutoRuDesignKit
import AutoRuDesignKitTokens
import AutoRuDesignKitSwiftUI
import AutoRuYogaLayout
import AutoRuModernLayout
import Snapshots

final class DesignKitButtonsTest: BaseUnitTest {
    private static let styles: [ButtonToken.Style] = [.primary, .secondary, .tertiary]
    private static let colors: [ButtonToken.Color] = [.green, .red, .blue, .white, .black]
    private static let sizes: [ButtonToken.Size] = [.small, .medium, .large, .extraLarge]

    func test_layout_onlyTitle() {
        for style in Self.styles {
            for color in Self.colors {
                for size in Self.sizes {
                    for isEnabled in [false, true] {
                        let layout = AutoRuDesignKit.ButtonLayout("Кнопка", size: size) { }
                            .buttonAppearance(color: color, style: style)
                            .disabled(!isEnabled)

                        Snapshot.compareWithSnapshot(
                            layout: layout.wrapForTest(),
                            maxWidth: DeviceWidth.iPhone11,
                            identifier: "design_kit_button_\(color)_\(style)_\(size)_\(isEnabled ? "enabled" : "disabled")"
                        )
                    }
                }
            }
        }
    }

    func test_layout_titleWithIcon() {
        for size in Self.sizes {
            let layoutLeftIcon = AutoRuDesignKit.ButtonLayout("Кнопка", icon: "attention", size: size) { }
                .buttonAppearance(color: .blue, style: .primary)

            let layoutRightIcon = AutoRuDesignKit.ButtonLayout(
                "Кнопка",
                icon: .init("attention", position: .right),
                size: size
            ) { }
            .buttonAppearance(color: .blue, style: .primary)

            Snapshot.compareWithSnapshot(
                layout: layoutLeftIcon.wrapForTest(),
                maxWidth: DeviceWidth.iPhone11,
                identifier: "design_kit_button_left_icon_\(size)"
            )

            Snapshot.compareWithSnapshot(
                layout: layoutRightIcon.wrapForTest(),
                maxWidth: DeviceWidth.iPhone11,
                identifier: "design_kit_button_right_icon_\(size)"
            )
        }
    }

    func test_layout_onlyIcon() {
        for size in Self.sizes {
            let layoutLeftIcon = AutoRuDesignKit.ButtonLayout(icon: "attention", size: size) { }
                .buttonAppearance(color: .blue, style: .primary)

            Snapshot.compareWithSnapshot(
                layout: layoutLeftIcon.wrapForTest(),
                maxWidth: DeviceWidth.iPhone11,
                identifier: "design_kit_button_only_icon_\(size)"
            )
        }
    }

    func test_layout_subtitle() {
        let withoutIcon = AutoRuDesignKit.ButtonLayout(
            "Кнопка",
            subtitle: "Подпись",
            appearance: .init(color: .blue, style: .primary),
            size: .large
        ) { }

        let iconLeft = AutoRuDesignKit.ButtonLayout(
            "Кнопка",
            subtitle: "Подпись",
            icon: "attention",
            appearance: .init(color: .blue, style: .primary),
            size: .large
        ) { }

        let iconRight = AutoRuDesignKit.ButtonLayout(
            "Кнопка",
            subtitle: "Подпись",
            icon: .init("attention", position: .right),
            appearance: .init(color: .blue, style: .primary),
            size: .large
        ) { }

        Snapshot.compareWithSnapshot(
            layout: withoutIcon.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_subtitle_without_icon"
        )

        Snapshot.compareWithSnapshot(
            layout: iconLeft.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_subtitle_left_icon"
        )

        Snapshot.compareWithSnapshot(
            layout: iconRight.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_subtitle_right_icon"
        )
    }

    func test_layout_longTitle() {
        let layoutLeftIcon = AutoRuDesignKit.ButtonLayout(
            "Длинный длинный заголовок кнопки, который должен быть обрезан",
            appearance: .init(color: .blue, style: .primary),
            size: .large
        ) { }

        Snapshot.compareWithSnapshot(
            layout: layoutLeftIcon.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_long_title"
        )
    }

    func test_layout_longTitleAndSubtitle() {
        let layoutLeftIcon = AutoRuDesignKit.ButtonLayout(
            "Длинный длинный заголовок кнопки обрезанный",
            subtitle: "Подпись",
            appearance: .init(color: .blue, style: .primary),
            size: .large
        ) { }

        Snapshot.compareWithSnapshot(
            layout: layoutLeftIcon.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_long_title_and_subtitle"
        )
    }

    func test_swiftUI_onlyTitle() {
        for style in Self.styles {
            for color in Self.colors {
                for size in Self.sizes {
                    for isEnabled in [false, true] {
                        let view = AruButton("Кнопка", shouldStretch: false) { }
                            .aruButtonStyle(color: color, style: style)
                            .aruButtonSize(size)
                            .disabled(!isEnabled)
                            .wrapForTest()

                        Snapshot.compareWithSnapshot(
                            view: view,
                            maxWidth: DeviceWidth.iPhone11,
                            identifier: "design_kit_button_swiftui_\(color)_\(style)_\(size)_\(isEnabled ? "enabled" : "disabled")"
                        )
                    }
                }
            }
        }
    }

    func test_swiftUI_titleWithIcon() {
        for size in Self.sizes {
            let leftIcon = AruButton("Кнопка", icon: "attention", shouldStretch: false) { }
                .aruButtonStyle(color: .blue, style: .primary)
                .aruButtonSize(size)
                .wrapForTest()

            let rightIcon = AruButton("Кнопка", icon: .init("attention", position: .right), shouldStretch: false) { }
                .aruButtonStyle(color: .blue, style: .primary)
                .aruButtonSize(size)
                .wrapForTest()

            Snapshot.compareWithSnapshot(
                view: leftIcon,
                maxWidth: DeviceWidth.iPhone11,
                identifier: "design_kit_button_swiftui_left_icon_\(size)"
            )

            Snapshot.compareWithSnapshot(
                view: rightIcon,
                maxWidth: DeviceWidth.iPhone11,
                identifier: "design_kit_button_swiftui_right_icon_\(size)"
            )
        }
    }

    func test_swiftUI_onlyIcon() {
        for size in Self.sizes {
            let leftIcon = AruButton(icon: "attention", shouldStretch: false) { }
                .aruButtonStyle(color: .blue, style: .primary)
                .aruButtonSize(size)

            Snapshot.compareWithSnapshot(
                view: leftIcon.wrapForTest(),
                maxWidth: DeviceWidth.iPhone11,
                identifier: "design_kit_button_swiftui_only_icon_\(size)"
            )
        }
    }

    func test_swiftUI_subtitle() {
        let withoutIcon = AruButton(
            "Кнопка",
            subtitle: "Подпись",
            shouldStretch: false
        ) { }
        .aruButtonStyle(color: .blue, style: .primary)
        .aruButtonSize(.large)

        let iconLeft = AruButton(
            "Кнопка",
            subtitle: "Подпись",
            icon: "attention",
            shouldStretch: false
        ) { }
        .aruButtonStyle(color: .blue, style: .primary)
        .aruButtonSize(.large)

        let iconRight = AruButton(
            "Кнопка",
            subtitle: "Подпись",
            icon: .init("attention", position: .right),
            shouldStretch: false
        ) { }
        .aruButtonStyle(color: .blue, style: .primary)
        .aruButtonSize(.large)

        Snapshot.compareWithSnapshot(
            view: withoutIcon.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_swiftui_subtitle_without_icon"
        )

        Snapshot.compareWithSnapshot(
            view: iconLeft.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_swiftui_subtitle_left_icon"
        )

        Snapshot.compareWithSnapshot(
            view: iconRight.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_swiftui_subtitle_right_icon"
        )
    }

    func test_swiftUI_longTitle() {
        let leftIcon = AruButton(
            "Длинный длинный заголовок кнопки, который должен быть обрезан",
            shouldStretch: false
        ) { }
        .aruButtonStyle(color: .blue, style: .primary)
        .aruButtonSize(.large)

        Snapshot.compareWithSnapshot(
            view: leftIcon.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_swiftui_long_title"
        )
    }

    func test_swiftUI_longTitleAndSubtitle() {
        let leftIcon = AruButton(
            "Длинный длинный заголовок кнопки обрезанный",
            subtitle: "Подпись",
            shouldStretch: false
        ) { }
        .aruButtonStyle(color: .blue, style: .primary)
        .aruButtonSize(.large)

        Snapshot.compareWithSnapshot(
            view: leftIcon.wrapForTest(),
            maxWidth: DeviceWidth.iPhone11,
            identifier: "design_kit_button_swiftui_long_title_and_subtitle"
        )
    }
}

extension LayoutConvertible {
    fileprivate func wrapForTest() -> LayoutConvertible {
        HStackLayout {
            self.margin(16)

            SpacerLayout()
        }
    }
}

private struct ForTest: ViewModifier {
    func body(content: Content) -> some View {
        HStack(alignment: .center, spacing: 0) {
            content.padding()

            Spacer(minLength: 0)
        }
    }
}

extension View {
    fileprivate func wrapForTest() -> some View {
        modifier(ForTest())
    }
}
