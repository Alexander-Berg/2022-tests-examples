// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM component/sample-app-component.ts >>>

import Foundation

open class SampleAppComponent: MBTComponent {
  public static let type: String = "SampleAppComponent"
  public init() {
  }

  @discardableResult
  open func assertMatches(_ model: App, _ application: App) throws -> Void {
    let sampleApp = SampleAppFeature.`get`.forceCast(application)
    (try assertTrue((try sampleApp.waitForAppReady()), "Unable to open Sample app"))
  }

  @discardableResult
  open func getComponentType() -> MBTComponentType {
    return SampleAppComponent.type
  }

  @discardableResult
  open func tostring() -> String {
    return self.getComponentType()
  }

}

