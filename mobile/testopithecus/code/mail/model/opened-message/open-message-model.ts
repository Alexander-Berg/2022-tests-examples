import { Int32, int64, Nullable, Throwing } from '../../../../../../common/ys'
import { MarkableImportant } from '../../feature/base-action-features'
import { LabelName } from '../../feature/folder-list-features'
import {
  FullMessageView,
  MessageViewer,
  MessageViewerAndroid,
  ThreadViewNavigator,
} from '../../feature/mail-view-features'
import { MessageContainerType } from '../../feature/message-list/container-getter-feature'
import { ExpandableThreads } from '../../feature/message-list/expandable-threads-feature'
import { LanguageName } from '../../feature/translator-features'
import { ArchiveMessageModel } from '../base-models/archive-message-model'
import { DeleteMessageModel } from '../base-models/delete-message-model'
import { LabelModel } from '../base-models/label-model'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'
import { MessageListDisplayModel } from '../messages-list/message-list-display-model'
import { fail } from '../../../../../testopithecus-common/code/utils/error-thrower'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { MessageListDatabaseFilter } from '../supplementary/message-list-database'
import {
  TranslatorBarModel,
  TranslatorLanguageName,
  TranslatorLanguageListModel,
  TranslatorSettingsModel,
} from '../translator-models'
import { QuickReplyModel, SmartReplyModel } from './quick-reply-models'

export class OpenMessageModel implements MessageViewer, ThreadViewNavigator, MessageViewerAndroid {
  public openedMessage: MessageId = int64(-1)
  private order: Nullable<Int32> = null

  public constructor(
    private markableImportant: MarkableImportant,
    private expandableThreads: ExpandableThreads,
    private messageListDisplay: MessageListDisplayModel,
    private creatableLabel: LabelModel,
    private deleteMessageModel: DeleteMessageModel,
    private accHandler: MailAppModelHandler,
    private archiveMessageModel: ArchiveMessageModel,
    private translatorBarModel: TranslatorBarModel,
    private translatorLanguageListModel: TranslatorLanguageListModel,
    private translatorSettingsModel: TranslatorSettingsModel,
    private smartReplyModel: SmartReplyModel,
    private quickReplyModel: QuickReplyModel,
  ) {}

  public closeMessage(): Throwing<void> {
    this.openedMessage = int64(-1)
    this.quickReplyModel.setMidOfOpenedMessage(this.openedMessage)
    this.order = null
    this.translatorBarModel.setMessageTranslateStatus(false)
  }

  public deleteCurrentMessage(): Throwing<void> {
    const openedMessage = this.openedMessage
    if (openedMessage === int64(-1)) {
      fail('No opened message!')
    }
    this.closeMessage() // TODO: bug, если в треде больше одного письма, то просмотр писем не закрывается
    this.deleteMessageModel.deleteOpenedMessage(openedMessage)
  }

  public deleteMessageByIcon(): Throwing<void> {
    const openedMessage = this.openedMessage
    if (openedMessage === int64(-1)) {
      fail('No opened message!')
    }
    this.deleteMessageModel.deleteOpenedMessage(openedMessage)
  }

  public getDefaultSourceLanguage(): Throwing<LanguageName> {
    return this.getOpenedMessage().lang
  }

  public deleteCurrentThread(): Throwing<void> {
    this.deleteMessageModel.deleteMessage(this.getMessageOrder())
    this.closeMessage()
  }

  public archiveCurrentThread(): Throwing<void> {
    this.archiveMessageModel.archiveMessage(this.getMessageOrder())
    this.closeMessage()
  }

  public isMessageOpened(): boolean {
    return this.openedMessage !== int64(-1)
  }

  public openMessage(order: Int32): Throwing<void> {
    this.order = order
    if (this.messageListDisplay.getCurrentContainer().type === MessageContainerType.search) {
      this.accHandler.getCurrentAccount().zeroSuggest.unshift(this.messageListDisplay.getCurrentContainer().name)
    }
    this.expandableThreads.markThreadMessageAsRead(order, 0)
    this.openedMessage = this.messageListDisplay.getMessageId(order)
    this.quickReplyModel.setMidOfOpenedMessage(this.openedMessage)

    const lang = this.getOpenedMessage().lang
    const defaultLanguage = this.translatorLanguageListModel.getDefaultTargetLanguage()
    this.translatorLanguageListModel.setSourceLanguage(lang)
    this.translatorLanguageListModel.setDeterminedAutomaticallySourceLanguage(lang)
    this.translatorLanguageListModel.setTargetLanguage(defaultLanguage, false)
    this.translatorBarModel.setSourceLanguage(TranslatorLanguageName.auto)
    this.translatorBarModel.setTranslateBarState(
      !(lang === defaultLanguage || this.translatorSettingsModel.isLanguageIgnored(lang)),
    )

    this.smartReplyModel.setSmartReplies(this.getOpenedMessage().smartReplies)
    this.quickReplyModel.setQuickReplyShown(this.getOpenedMessage().quickReply)
  }

  public getOrder(): Nullable<Int32> {
    return this.order
  }

  public getOpenedMessage(): Throwing<FullMessageView> {
    return this.accHandler
      .getCurrentAccount()
      .messagesDB.storedMessage(
        this.openedMessage,
        this.translatorBarModel.isMessageTranslated() ? this.translatorBarModel.getTargetLanguage() : null,
      )
  }

  public checkIfRead(): Throwing<boolean> {
    return this.accHandler.getCurrentAccount().messagesDB.storedMessage(this.openedMessage).head.read
  }

  public getLabels(): Throwing<Set<string>> {
    return this.creatableLabel.getMessageLabels(this.openedMessage)
  }

  public checkIfImportant(): Throwing<boolean> {
    return this.accHandler.getCurrentAccount().messagesDB.storedMessage(this.openedMessage).head.important
  }

  public checkIfSpam(): Throwing<boolean> {
    if (!this.accHandler.getCurrentAccount().messagesDB.getFolderList().includes(DefaultFolderName.spam)) {
      return false
    }
    return this.accHandler
      .getCurrentAccount()
      .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withFolder(DefaultFolderName.spam))
      .includes(this.openedMessage)
  }

  public deleteLabelsFromHeader(labels: LabelName[]): Throwing<void> {
    // todo нужно ли как-то иначе это реализовать?
    this.creatableLabel.removeLabelsFromMessages(new Set([this.openedMessage]), labels)
  }

  public markAsUnimportantFromHeader(): Throwing<void> {
    this.markableImportant.markAsUnimportant(this.getMessageOrder())
  }

  public arrowDownClick(): Throwing<void> {
    this.openMessage(this.getMessageOrder() + 1)
  }

  public arrowUpClick(): Throwing<void> {
    this.openMessage(this.getMessageOrder() - 1)
  }

  private getMessageOrder(): Throwing<Int32> {
    return requireNonNull(this.order, 'No opened message!')
  }
}
