// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM event-logs/log-generated-test.ts >>>

import Foundation

open class LogGeneratedTest: RegularYandexMailTestBase {
  public var plan: TestPlan
  public init(_ plan: TestPlan) {
    self.plan = plan
    super.init("Test was generated from logs")
  }

  open override func prepareAccount(_ mailbox: MailboxBuilder) -> Void {
    for i in stride(from: 0, to: 15, by: 1) {
      mailbox.nextMessage("subj\(i)")
    }
  }

  @discardableResult
  open override func testScenario(_ _account: UserAccount) -> TestPlan {
    return self.plan
  }

}

