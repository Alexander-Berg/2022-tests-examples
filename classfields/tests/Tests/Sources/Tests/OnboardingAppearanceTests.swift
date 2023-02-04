import XCTest
import Snapshots
@testable import AutoRuOnboarding
import CoreGraphics

final class OnboardingAppearanceTests: BaseUnitTest {
    private static let devices: [String: (CGSize, OnboardingStyleProvider)] = [
        "iPhone11": (
            CGSize(width: 414, height: 896),
            OnboardingStyleProvider(screenHeight: 896, safeArea: .init(top: 44, left: 0, bottom: 34, right: 0))
        ),
        "iPhoneX": (
            CGSize(width: 375, height: 812),
            OnboardingStyleProvider(screenHeight: 812, safeArea: .init(top: 44, left: 0, bottom: 34, right: 0))
        ),
        "iPhone8Plus": (
            CGSize(width: 414, height: 736),
            OnboardingStyleProvider(screenHeight: 736, safeArea: .zero)
        ),
        "iPhone8": (
            CGSize(width: 375, height: 667),
            OnboardingStyleProvider(screenHeight: 667, safeArea: .zero)
        ),
        "iPhoneSE": (
            CGSize(width: 320, height: 568),
            OnboardingStyleProvider(screenHeight: 568, safeArea: .zero)
        )
    ]

    func test_roleSelection() {
        for (deviceName, device) in Self.devices {
            Step("Проверка экрана выбора роли для девайса '\(deviceName)'") {
                let controller = OnboardingContainerController(frameSize: device.0, styleProvider: device.1)
                Snapshot.compareWithSnapshot(
                    viewController: controller,
                    size: device.0,
                    identifier: "role_selection_\(deviceName)",
                    perPixelTolerance: 0.005,
                    overallTolerance: 0.005
                )
            }
        }
    }

    func test_slideAppearance() {
        let roles = OnboardingContent.roles

        for role in roles {
            for (idx, slide) in role.slides.enumerated() {
                Step("Проверка экрана слайда '\(role.selectionTitle)' -> '\(slide.title)'") {
                    for (deviceName, device) in Self.devices {
                        Step("Проверка экрана на девайсе '\(deviceName)'") {
                            let controller = OnboardingContainerController.showSingleRoleSlide(
                                slide,
                                frameSize: device.0,
                                provider: device.1
                            )

                            Snapshot.compareWithSnapshot(
                                viewController: controller,
                                size: device.0,
                                identifier: "slide_\(role.role.rawValue)_\(idx)_\(deviceName)",
                                overallTolerance: 0.005
                            )
                        }
                    }
                }
            }
        }
    }
}
