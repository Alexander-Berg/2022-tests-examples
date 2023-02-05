// Copyright 2022 Yandex LLC. All rights reserved.

#if PREVIEW
import SwiftUI

protocol Testable {
  associatedtype Sample: View
  associatedtype PreviewModifier: PureViewModifier

  static var samples: [Sample] { get }
  static var testableSamples: [ModifiedContent<Sample, PreviewModifier>] { get }
}

extension Testable {
  static var testableSamples: [ModifiedContent<Sample, PreviewModifier>] {
    samples.map { $0.modifier(PreviewModifier()) }
  }
}
#endif
