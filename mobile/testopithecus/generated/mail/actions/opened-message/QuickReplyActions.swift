// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/opened-message/quick-reply-actions.ts >>>

import Foundation

open class QuickReplyTapOnTextFieldAction: BaseSimpleAction<QuickReply, MBTComponent> {
  public static let type: MBTActionType = "QuickReplyTapOnTextFieldAction"
  public init() {
    super.init(QuickReplyTapOnTextFieldAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<QuickReply> {
    return QuickReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: QuickReply) throws -> Bool {
    return (try model.isQuickReplyShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: QuickReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnTextField())
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "QuickReplyTapOnTextFieldAction"
  }

}

open class QuickReplySetTextFieldAction: BaseSimpleAction<QuickReply, MBTComponent> {
  public static let type: MBTActionType = "QuickReplySetTextFieldAction"
  private let text: String
  public init(_ text: String, _ unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE) {
    self.text = text
    super.init(QuickReplySetTextFieldAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<QuickReply> {
    return QuickReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: QuickReply) throws -> Bool {
    return (try model.isQuickReplyShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: QuickReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.setTextFieldValue(self.text))
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "QuickReplySetTextFieldAction"
  }

}

open class QuickReplyPasteToTextFieldAction: BaseSimpleAction<QuickReply, MBTComponent> {
  public static let type: MBTActionType = "QuickReplyPasteToTextFieldAction"
  private let text: String
  public init(_ text: String, _ unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE) {
    self.text = text
    super.init(QuickReplyPasteToTextFieldAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<QuickReply> {
    return QuickReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: QuickReply) throws -> Bool {
    return (try model.isQuickReplyShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: QuickReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.pasteTextFieldValue(self.text))
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "QuickReplyPasteToTextFieldAction"
  }

}

open class QuickReplyTapOnComposeButtonAction: BaseSimpleAction<QuickReply, MBTComponent> {
  public static let type: MBTActionType = "QuickReplyTapOnComposeButtonAction"
  public init() {
    super.init(QuickReplyTapOnComposeButtonAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<QuickReply> {
    return QuickReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: QuickReply) throws -> Bool {
    return (try model.isQuickReplyShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: QuickReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnComposeButton())
    return ComposeComponent()
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "QuickReplyTapOnComposeButtonAction"
  }

}

open class QuickReplyTapOnSendButtonAction: BaseSimpleAction<QuickReply, MBTComponent> {
  public static let type: MBTActionType = "QuickReplyTapOnSendButtonAction"
  public init() {
    super.init(QuickReplyTapOnSendButtonAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<QuickReply> {
    return QuickReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: QuickReply) throws -> Bool {
    let isSendButtonEnabled = (try model.isSendButtonEnabled())
    return (try model.isQuickReplyShown()) && isSendButtonEnabled
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: QuickReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnSendButton())
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "QuickReplyTapOnSendButtonAction"
  }

}

open class SmartReplyTapOnSmartReplyAction: BaseSimpleAction<SmartReply, MBTComponent> {
  public static let type: MBTActionType = "SmartReplyTapOnSmartReplyAction"
  private let order: Int32
  public init(_ order: Int32, _ unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE) {
    self.order = order
    super.init(SmartReplyTapOnSmartReplyAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<SmartReply> {
    return SmartReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: SmartReply) throws -> Bool {
    let smartRepliesCount = (try model.getSmartReplies()).length
    return (try model.isSmartRepliesShown()) && self.order < smartRepliesCount
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: SmartReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnSmartReply(self.order))
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "SmartReplyTapOnSmartReplyAction"
  }

}

open class SmartReplyCloseSmartReplyAction: BaseSimpleAction<SmartReply, MBTComponent> {
  public static let type: MBTActionType = "SmartReplyCloseSmartReplyAction"
  private let order: Int32
  public init(_ order: Int32, _ unusedValue: String = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE) {
    self.order = order
    super.init(SmartReplyCloseSmartReplyAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<SmartReply> {
    return SmartReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: SmartReply) throws -> Bool {
    return (try model.isSmartRepliesShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: SmartReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.closeSmartReply(self.order))
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "SmartReplyCloseSmartReplyAction"
  }

}

open class SmartReplyCloseAllSmartRepliesAction: BaseSimpleAction<SmartReply, MBTComponent> {
  public static let type: MBTActionType = "SmartReplyCloseAllSmartRepliesAction"
  public init() {
    super.init(SmartReplyCloseAllSmartRepliesAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<SmartReply> {
    return SmartReplyFeature.`get`
  }

  @discardableResult
  open override func canBePerformedImpl(_ model: SmartReply) throws -> Bool {
    return (try model.isSmartRepliesShown())
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: SmartReply, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.closeAllSmartReplies())
    return currentComponent
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open override func tostring() -> String {
    return "SmartReplyCloseAllSmartRepliesAction"
  }

}

