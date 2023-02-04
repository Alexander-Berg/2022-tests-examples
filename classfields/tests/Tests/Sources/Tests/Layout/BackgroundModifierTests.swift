//
//  BackgroundModifierTests.swift
//  Tests
//
//  Created by Aleksey Gotyanov on 12/14/20.
//

import AutoRuModernLayout
import XCTest
import AutoRuYogaLayout
import Snapshots
import AutoRuAttributedStringBuilder

class BackgroundModifierTests: BaseUnitTest {
    func testBackgroundNegativeMargin() {
        let layout = VStackLayout {
            "Hello world!".attributed()
                .background(UIColor.red.margin(-10))
        }
        .padding(20)

        Snapshot.compareWithSnapshot(layout: layout, backgroundColor: .white, interfaceStyle: [.light])
    }

    func testBackgroundLayout() {
        struct Rect: ModernLayout {
            var body: LayoutConvertible {
                UIColor.blue.frame(width: 20, height: 20)
            }
        }

        let layout =
            VStackLayout {
                SpacerLayout()
                    .background(UIColor.red)
            }
            .padding(10)
            .frame(width: 50, height: 50)
            .background(
                VStackLayout {
                    HStackLayout {
                        Rect()
                        SpacerLayout()
                        Rect()
                    }

                    SpacerLayout()

                    HStackLayout {
                        Rect()
                        SpacerLayout()
                        Rect()
                    }
                }
            )

        Snapshot.compareWithSnapshot(layout: layout, backgroundColor: .white, interfaceStyle: [.light])
    }

    func testOverlayNegativeMargin() {
        let layout = VStackLayout {
            "Hello world!".attributed()
                .overlay(UIColor.red.withAlphaComponent(0.5).margin(-10))
        }
        .padding(20)

        Snapshot.compareWithSnapshot(layout: layout, backgroundColor: .white, interfaceStyle: [.light])
    }

    func testOverlayLayout() {
        struct Rect: ModernLayout {
            var body: LayoutConvertible {
                UIColor.blue.frame(width: 20, height: 20)
            }
        }

        let layout =
            VStackLayout {
                SpacerLayout()
                    .overlay(UIColor.red)
            }
            .padding(10)
            .frame(width: 50, height: 50)
            .overlay(
                VStackLayout {
                    HStackLayout {
                        Rect()
                        SpacerLayout()
                        Rect()
                    }

                    SpacerLayout()

                    HStackLayout {
                        Rect()
                        SpacerLayout()
                        Rect()
                    }
                }
            )

        Snapshot.compareWithSnapshot(layout: layout, backgroundColor: .white, interfaceStyle: [.light])
    }
}
