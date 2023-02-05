import { TestopithecusConstants } from '../../../../../testopithecus-common/code/utils/utils'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import { Feature, MBTActionType, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Int32, Throwing } from '../../../../../../common/ys'
import { ComposeComponent } from '../../components/compose-component'
import { QuickReply, QuickReplyFeature, SmartReply, SmartReplyFeature } from '../../feature/quick-reply-features'

export class QuickReplyTapOnTextFieldAction extends BaseSimpleAction<QuickReply, MBTComponent> {
  public static readonly type: MBTActionType = 'QuickReplyTapOnTextFieldAction'

  public constructor() {
    super(QuickReplyTapOnTextFieldAction.type)
  }

  public requiredFeature(): Feature<QuickReply> {
    return QuickReplyFeature.get
  }

  public canBePerformedImpl(model: QuickReply): Throwing<boolean> {
    return model.isQuickReplyShown()
  }

  public performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnTextField()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'QuickReplyTapOnTextFieldAction'
  }
}

export class QuickReplySetTextFieldAction extends BaseSimpleAction<QuickReply, MBTComponent> {
  public static readonly type: MBTActionType = 'QuickReplySetTextFieldAction'

  public constructor(
    private readonly text: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(QuickReplySetTextFieldAction.type)
  }

  public requiredFeature(): Feature<QuickReply> {
    return QuickReplyFeature.get
  }

  public canBePerformedImpl(model: QuickReply): Throwing<boolean> {
    return model.isQuickReplyShown()
  }

  public performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setTextFieldValue(this.text)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'QuickReplySetTextFieldAction'
  }
}

export class QuickReplyPasteToTextFieldAction extends BaseSimpleAction<QuickReply, MBTComponent> {
  public static readonly type: MBTActionType = 'QuickReplyPasteToTextFieldAction'

  public constructor(
    private readonly text: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(QuickReplyPasteToTextFieldAction.type)
  }

  public requiredFeature(): Feature<QuickReply> {
    return QuickReplyFeature.get
  }

  public canBePerformedImpl(model: QuickReply): Throwing<boolean> {
    return model.isQuickReplyShown()
  }

  public performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.pasteTextFieldValue(this.text)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'QuickReplyPasteToTextFieldAction'
  }
}

export class QuickReplyTapOnComposeButtonAction extends BaseSimpleAction<QuickReply, MBTComponent> {
  public static readonly type: MBTActionType = 'QuickReplyTapOnComposeButtonAction'

  public constructor() {
    super(QuickReplyTapOnComposeButtonAction.type)
  }

  public requiredFeature(): Feature<QuickReply> {
    return QuickReplyFeature.get
  }

  public canBePerformedImpl(model: QuickReply): Throwing<boolean> {
    return model.isQuickReplyShown()
  }

  public performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnComposeButton()
    return new ComposeComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'QuickReplyTapOnComposeButtonAction'
  }
}

export class QuickReplyTapOnSendButtonAction extends BaseSimpleAction<QuickReply, MBTComponent> {
  public static readonly type: MBTActionType = 'QuickReplyTapOnSendButtonAction'

  public constructor() {
    super(QuickReplyTapOnSendButtonAction.type)
  }

  public requiredFeature(): Feature<QuickReply> {
    return QuickReplyFeature.get
  }

  public canBePerformedImpl(model: QuickReply): Throwing<boolean> {
    const isSendButtonEnabled = model.isSendButtonEnabled()
    return model.isQuickReplyShown() && isSendButtonEnabled
  }

  public performImpl(modelOrApplication: QuickReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSendButton()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'QuickReplyTapOnSendButtonAction'
  }
}

export class SmartReplyTapOnSmartReplyAction extends BaseSimpleAction<SmartReply, MBTComponent> {
  public static readonly type: MBTActionType = 'SmartReplyTapOnSmartReplyAction'

  public constructor(
    private readonly order: Int32,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(SmartReplyTapOnSmartReplyAction.type)
  }

  public requiredFeature(): Feature<SmartReply> {
    return SmartReplyFeature.get
  }

  public canBePerformedImpl(model: SmartReply): Throwing<boolean> {
    const smartRepliesCount = model.getSmartReplies().length
    return model.isSmartRepliesShown() && this.order < smartRepliesCount
  }

  public performImpl(modelOrApplication: SmartReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSmartReply(this.order)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SmartReplyTapOnSmartReplyAction'
  }
}

export class SmartReplyCloseSmartReplyAction extends BaseSimpleAction<SmartReply, MBTComponent> {
  public static readonly type: MBTActionType = 'SmartReplyCloseSmartReplyAction'

  public constructor(
    private readonly order: Int32,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(SmartReplyCloseSmartReplyAction.type)
  }

  public requiredFeature(): Feature<SmartReply> {
    return SmartReplyFeature.get
  }

  public canBePerformedImpl(model: SmartReply): Throwing<boolean> {
    return model.isSmartRepliesShown()
  }

  public performImpl(modelOrApplication: SmartReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeSmartReply(this.order)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SmartReplyCloseSmartReplyAction'
  }
}

export class SmartReplyCloseAllSmartRepliesAction extends BaseSimpleAction<SmartReply, MBTComponent> {
  public static readonly type: MBTActionType = 'SmartReplyCloseAllSmartRepliesAction'

  public constructor() {
    super(SmartReplyCloseAllSmartRepliesAction.type)
  }

  public requiredFeature(): Feature<SmartReply> {
    return SmartReplyFeature.get
  }

  public canBePerformedImpl(model: SmartReply): Throwing<boolean> {
    return model.isSmartRepliesShown()
  }

  public performImpl(modelOrApplication: SmartReply, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeAllSmartReplies()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SmartReplyCloseAllSmartRepliesAction'
  }
}
