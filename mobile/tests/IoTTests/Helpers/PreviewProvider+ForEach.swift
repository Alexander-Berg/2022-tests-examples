// Copyright 2022 Yandex LLC. All rights reserved.

#if PREVIEW

import SwiftUI

extension View {
  var uuid: UUID {
    .init()
  }
}

extension PreviewProvider where Self: Testable {
  static var previews: some View {
    ForEach(samples, id: \.uuid) { $0 }.modifier(PreviewModifier())
  }
}

#endif
