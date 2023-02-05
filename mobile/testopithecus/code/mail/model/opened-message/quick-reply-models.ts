import { copyArray } from '../../../../../testopithecus-common/code/utils/utils'
import { Int32, int64, Throwing } from '../../../../../../common/ys'
import { QuickReply, SmartReply } from '../../feature/quick-reply-features'
import { ComposeModel } from '../compose/compose-model'
import { MessageId } from '../mail-model'
import { GeneralSettingsModel } from '../settings/general-settings-model'

export class QuickReplyModel implements QuickReply {
  public constructor(private composeModel: ComposeModel) {}

  private textFieldValue: string = ''
  private quickReplyShown: boolean = false
  private openedMessageId: MessageId = int64(-1)

  public getTextFieldValue(): Throwing<string> {
    return this.textFieldValue
  }

  public isSendButtonEnabled(): Throwing<boolean> {
    return !this.isTextFieldEmpty()
  }

  public isTextFieldEmpty(): boolean {
    return this.textFieldValue === ''
  }

  public setMidOfOpenedMessage(mid: MessageId): void {
    this.openedMessageId = mid
  }

  public setTextFieldValue(message: string): Throwing<void> {
    this.textFieldValue = message
    this.setDataToComposeModel()
  }

  public isQuickReplyTextFieldExpanded(): Throwing<boolean> {
    return this.textFieldValue.includes('\n')
  }

  public pasteTextFieldValue(message: string): Throwing<void> {
    this.textFieldValue = message
    this.setDataToComposeModel()
  }

  public tapOnComposeButton(): Throwing<void> {
    this.setDataToComposeModel()
  }

  public tapOnSendButton(): Throwing<void> {
    this.composeModel.sendMessage()
    this.textFieldValue = ''
  }

  public tapOnTextField(): Throwing<void> {
    // do nothing
  }

  public setQuickReplyShown(shown: boolean): Throwing<void> {
    this.quickReplyShown = shown
  }

  public isQuickReplyShown(): Throwing<boolean> {
    return this.quickReplyShown
  }

  private setDataToComposeModel(): Throwing<void> {
    this.composeModel.openReplyCompose(this.openedMessageId)
    this.composeModel.setBody(this.textFieldValue)
  }
}

export class SmartReplyModel implements SmartReply {
  private smartReplies: string[] = []

  public constructor(private quickReplyModel: QuickReplyModel, private generalSettingsModel: GeneralSettingsModel) {}

  public setSmartReplies(smartReplies: string[]): Throwing<void> {
    this.smartReplies = copyArray(smartReplies)
  }

  public closeAllSmartReplies(): Throwing<void> {
    this.smartReplies = []
  }

  public closeSmartReply(order: Int32): Throwing<void> {
    this.smartReplies.splice(order, 1)
  }

  public getSmartReply(order: Int32): Throwing<string> {
    return this.smartReplies[order]
  }

  public tapOnSmartReply(order: Int32): Throwing<void> {
    this.quickReplyModel.setTextFieldValue(this.smartReplies[order])
  }

  public isSmartRepliesShown(): Throwing<boolean> {
    const isSmartRepliesEnabledInSettings = this.generalSettingsModel.isSmartRepliesEnabled()
    return this.smartReplies.length > 0 && isSmartRepliesEnabledInSettings && this.quickReplyModel.isTextFieldEmpty()
  }

  public getSmartReplies(): Throwing<string[]> {
    return this.isSmartRepliesShown() ? this.smartReplies : []
  }
}
