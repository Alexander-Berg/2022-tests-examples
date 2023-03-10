// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM feature/fill-3ds-feature.ts >>>

import Foundation

open class Fill3dsFeature: Feature<Fill3ds> {
  public static var `get`: Fill3dsFeature = Fill3dsFeature()
  private init() {
    super.init("Fill3dsFeature", "Feature to enter 3ds on bank site")
  }

}

public protocol Fill3ds {
  @discardableResult
  func waitFor3dsPage(_ mSec: Int32) throws -> Bool
  @discardableResult
  func fill3dsCode(_ code: String) throws -> Void
  @discardableResult
  func close3dsPage() throws -> Void
}

