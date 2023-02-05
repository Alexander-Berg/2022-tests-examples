import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Int32, Throwing } from '../../../../../common/ys'

export class QuickReplyFeature extends Feature<QuickReply> {
  public static get: QuickReplyFeature = new QuickReplyFeature()

  private constructor() {
    super('QuickReply', 'Быстрый ответ в просмотре письма')
  }
}

export interface QuickReply {
  tapOnTextField(): Throwing<void>

  setTextFieldValue(message: string): Throwing<void>

  pasteTextFieldValue(message: string): Throwing<void>

  getTextFieldValue(): Throwing<string>

  isQuickReplyTextFieldExpanded(): Throwing<boolean>

  tapOnComposeButton(): Throwing<void>

  tapOnSendButton(): Throwing<void>

  isSendButtonEnabled(): Throwing<boolean>

  isQuickReplyShown(): Throwing<boolean>
}

export class SmartReplyFeature extends Feature<SmartReply> {
  public static get: SmartReplyFeature = new SmartReplyFeature()

  private constructor() {
    super('SmartReply', 'Варианты быстрого ответа в просмотре письма')
  }
}

export interface SmartReply {
  tapOnSmartReply(order: Int32): Throwing<void>

  getSmartReply(order: Int32): Throwing<string>

  getSmartReplies(): Throwing<string[]>

  closeSmartReply(order: Int32): Throwing<void>

  closeAllSmartReplies(): Throwing<void>

  isSmartRepliesShown(): Throwing<boolean>
}
