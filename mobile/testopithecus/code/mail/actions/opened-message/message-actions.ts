import { Int32, int64, Throwing } from '../../../../../../common/ys'
import { Eventus } from '../../../../../eventus/code/events/eventus'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MaillistComponent } from '../../components/maillist-component'
import { MessageComponent } from '../../components/message-component'
import {
  MessageViewer,
  MessageViewerAndroid,
  MessageViewerAndroidFeature,
  MessageViewerFeature,
} from '../../feature/mail-view-features'
import { GroupModeFeature } from '../../feature/message-list/group-mode-feature'
import { MessageListDisplayFeature } from '../../feature/message-list/message-list-display-feature'

export class OpenMessageAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenMessageAction'

  public constructor(private order: Int32) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return MessageViewerFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isInGroupMode = GroupModeFeature.get.castIfSupported(model)!.isInGroupMode()
    return !isInGroupMode
  }

  public async perform(model: App, application: App, _history: MBTHistory): Throwing<Promise<MBTComponent>> {
    MessageViewerFeature.get.forceCast(model).openMessage(this.order)
    MessageViewerFeature.get.forceCast(application).openMessage(this.order)
    return new MessageComponent()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageListEvents.openMessage(this.order, int64(-1))]
  }

  public tostring(): string {
    return `${OpenMessageAction.type}(${this.order})`
  }

  public getActionType(): MBTActionType {
    return OpenMessageAction.type
  }
}

export class MessageViewBackToMailListAction extends BaseSimpleAction<MessageViewer, MBTComponent> {
  public static readonly type: MBTActionType = 'MessageViewBackToMailListAction'

  public constructor() {
    super(MessageViewBackToMailListAction.type)
  }

  public requiredFeature(): Feature<MessageViewer> {
    return MessageViewerFeature.get
  }

  public performImpl(modelOrApplication: MessageViewer, _currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeMessage()
    return new MaillistComponent()
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.backToMailList()]
  }
}

export class MessageViewDeleteMessageByIconAction extends BaseSimpleAction<MessageViewerAndroid, MessageComponent> {
  public static readonly type: MBTActionType = 'MessageViewDeleteMessageByIconAction'

  public constructor() {
    super(MessageViewDeleteMessageByIconAction.type)
  }

  public events(): EventusEvent[] {
    return [Eventus.messageViewEvents.deleteMessage()]
  }

  public performImpl(
    modelOrApplication: MessageViewerAndroid,
    currentComponent: MessageComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.deleteMessageByIcon()
    return new MaillistComponent()
  }

  public requiredFeature(): Feature<MessageViewerAndroid> {
    return MessageViewerAndroidFeature.get
  }
}

export class ArrowDownClickAction extends BaseSimpleAction<MessageViewer, MessageComponent> {
  public static readonly type: MBTActionType = 'ArrowDownClickAction'

  public constructor(private order: Int32) {
    super(ArrowDownClickAction.type)
  }

  public events(): EventusEvent[] {
    return []
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListDisplayModel.getMessageList(this.order + 1)
    return this.order < messages.length
  }

  public performImpl(modelOrApplication: MessageViewer, currentComponent: MessageComponent): Throwing<MBTComponent> {
    modelOrApplication.arrowDownClick()
    return new MessageComponent()
  }

  public requiredFeature(): Feature<MessageViewer> {
    return MessageViewerFeature.get
  }
}

export class ArrowUpClickAction extends BaseSimpleAction<MessageViewer, MessageComponent> {
  public static readonly type: MBTActionType = 'ArrowUpClickAction'

  public constructor() {
    super(ArrowUpClickAction.type)
  }

  public events(): EventusEvent[] {
    return []
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const openedMessage = MessageViewerFeature.get.forceCast(model).getOpenedMessage().head.timestamp
    const messageListDisplayModel = MessageListDisplayFeature.get.forceCast(model)
    const messages = messageListDisplayModel.getMessageList(10)[0].timestamp
    return openedMessage !== messages
  }

  public performImpl(modelOrApplication: MessageViewer, currentComponent: MessageComponent): Throwing<MBTComponent> {
    modelOrApplication.arrowUpClick()
    return new MessageComponent()
  }

  public requiredFeature(): Feature<MessageViewer> {
    return MessageViewerFeature.get
  }
}
