// Copyright 2022 Yandex LLC. All rights reserved.

#if PREVIEW

import SwiftUI

protocol PureViewModifier: ViewModifier {
  init()
}

struct IdentityViewModifier: PureViewModifier {
  func body(content: Content) -> some View {
    content
  }
}

#endif
