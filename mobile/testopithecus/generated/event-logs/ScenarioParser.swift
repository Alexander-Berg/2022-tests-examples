// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM event-logs/scenario-parser.ts >>>

import Foundation

open class ScenarioParser {
  private var actionParser: ActionParser
  private var jsonSerializer: JSONSerializer
  public init(_ actionParser: ActionParser, _ jsonSerializer: JSONSerializer) {
    self.actionParser = actionParser
    self.jsonSerializer = jsonSerializer
  }

  @discardableResult
  open func parse(_ eventStrings: YSArray<String>) -> TestPlan {
    let testPlan = TestPlan.empty()
    for event in eventStrings {
      testPlan.then(self.actionParser.parseActionFromString(event))
    }
    return testPlan
  }

  @discardableResult
  open func parseFromJsonText(_ text: String) -> TestPlan {
    let actions = self.jsonSerializer.deserialize(text).getValue() as! ArrayJSONItem
    let testPlan = TestPlan.empty()
    for json in actions.asArray() {
      testPlan.then(self.actionParser.parseActionFromJson(json as! MapJSONItem))
    }
    return testPlan
  }

  @discardableResult
  open func parseFromText(_ text: String) -> TestPlan {
    let lines: YSArray<String> = YSArray()
    for line in text.split("\n") {
      lines.push(line.substring(line.search("{") as! Int32, (line.search("}") as! Int32) + 1))
    }
    return self.parse(lines.filter({
      (line) in
      line.length > 0
    }))
  }

}

