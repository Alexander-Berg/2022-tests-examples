// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/actions/base-actions/delete-message-action.ts >>>

import Foundation

open class BaseDeleteMessageAction: MBTAction {
  public var order: Int32
  public init(_ order: Int32) {
    self.order = order
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return (MessageListDisplayFeature.`get`.included(modelFeatures) && ContainerGetterFeature.`get`.included(modelFeatures) && DeleteMessageFeature.`get`.includedAll(modelFeatures, applicationFeatures) && LongSwipeFeature.`get`.includedAll(modelFeatures, applicationFeatures))
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    let messageListModel = MessageListDisplayFeature.`get`.forceCast(model)
    let messages = (try messageListModel.getMessageList(10))
    let actionOnSwipe = (try GeneralSettingsFeature.`get`.forceCast(model).getActionOnSwipe())
    let currentContainer = (try ContainerGetterFeature.`get`.forceCast(model).getCurrentContainer())
    return (self.order < messages.length && (actionOnSwipe == ActionOnSwipe.delete || currentContainer.name == DefaultFolderName.archive))
  }

  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    (try DeleteMessageFeature.`get`.forceCast(model).deleteMessage(self.order))
    (try DeleteMessageFeature.`get`.forceCast(application).deleteMessage(self.order))
    return MaillistComponent()
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray(Eventus.messageListEvents.deleteMessage(self.order, fakeMid()))
  }

  @discardableResult
  open func tostring() -> String {
    return "DeleteMessageByLongSwipe(#\(self.order))"
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    fatalError("Must be overridden in subclasses")
  }

}

open class DeleteMessageAction: BaseDeleteMessageAction {
  public static let type: MBTActionType = "DeleteMessage"
  public override init(_ order: Int32) {
    super.init(order)
  }

  @discardableResult
  open func performImpl(_ modelOrApplication: DeleteMessage) throws -> Void {
    return (try modelOrApplication.deleteMessage(self.order))
  }

  @discardableResult
  open override func getActionType() -> MBTActionType {
    return DeleteMessageAction.type
  }

}

