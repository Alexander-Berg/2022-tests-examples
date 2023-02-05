import { undefinedToNull } from '../../../../common/ys'
import { Logger } from '../../../common/code/logging/logger'
import { ID, LabelID } from '../../../mapi/code/api/common/id'
import { Folder, isFolderOfTabType, isFolderOfThreadedType } from '../../../mapi/code/api/entities/folder/folder'
import { Label, LabelType } from '../../../mapi/code/api/entities/label/label'
import { MessageMeta } from '../../../mapi/code/api/entities/message/message-meta'
import { TranslationLangsPayload } from '../../../mapi/code/api/entities/translator/translator-response'
import { AppModel, AppModelProvider } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { copyArray, requireNonNull, valuesArray } from '../../../testopithecus-common/code/utils/utils'
import { MailboxClient } from '../client/mailbox-client'
import { display, removeAllNonLetterSymbols } from '../utils/mail-utils'
import { FolderName, LabelName } from './feature/folder-list-features'
import { LanguageCode, LanguageName } from './feature/translator-features'
import { DefaultFolderName, formatFolderNameIfNeeded, isTab } from './model/folder-data-model'
import {
  AccountMailboxData,
  AccountSettingsModel,
  FolderId,
  FullMessage,
  MailAppModelHandler,
  MailboxModel,
  MessageId,
} from './model/mail-model'
import { MessageListDatabase } from './model/supplementary/message-list-database'
import { TranslatorLanguageCode, TranslatorLanguageName } from './model/translator-models'

export class MailboxDownloader implements AppModelProvider {
  public constructor(private clients: MailboxClient[], private logger: Logger) {}

  public async takeAppModel(): Promise<AppModel> {
    this.logger.info('Downloading mailbox started')
    const accountsData: AccountMailboxData[] = []
    for (const client of this.clients) {
      this.logger.info(`Downloading account (${client.oauthAccount.account.login}) started`)
      const filters = client.listFilter().rules
      const settings = client.getSettings().payload!
      const isTabEnabled = settings.userParameters.showFoldersTabs
      const folderList = client
        .getFolderList(isTabEnabled)
        .filter((folder) => folder.name !== DefaultFolderName.outgoing)
      const allLabels = client.getLabelList()
      const labelList = allLabels.filter((label) => label.type === LabelType.user)
      const fidToFolder = new Map<FolderId, Folder>()
      const lidToLabel = new Map<LabelID, Label>()
      folderList.forEach((folder) => fidToFolder.set(folder.fid, folder))
      labelList.forEach((label) => lidToLabel.set(label.lid, label))
      const messages = new Map<MessageId, FullMessage>()
      const messageToFolder = new Map<MessageId, FolderName>()
      const messageToLabels = new Map<MessageId, LabelName[]>()
      const threads = new Map<ID, Set<MessageId>>()
      const defaultEmail = settings.settingsSetup.defaultEmail
      const aliases = settings.accountInformation.emails.map((email) => display(email))
      const replyTo = settings.settingsSetup.replyTo
      const contacts = client.getAllContactsList(1000)
      const zeroSuggest = client.getZeroSuggest().map((suggest) => suggest.show_text)
      const promoteMail360 = settings.settingsSetup.promoteMail360
      const translationLangs = client.getTranslationLangs().translationLangs
      const translationLangNames = translationLangs.map((lang) => lang.name)
      const accountSettings = new AccountSettingsModel(
        settings.settingsSetup.folderThreadView,
        settings.userParameters.showFoldersTabs,
        settings.settingsSetup.mobileSign,
        settings.settingsSetup.signatureTop,
        folderList.map((folder) => folder.name!),
      )
      for (const folder of folderList) {
        // Inbox only for speed
        const messagesDTO = isFolderOfThreadedType(folder.type)
          ? client.getThreadsInFolder(folder.fid, 10, isTabEnabled)
          : client.getMessagesInFolder(folder.fid, 10, isTabEnabled)
        messagesDTO.forEach((messageDTO) => {
          const messageModel = FullMessage.fromMeta(messageDTO)
          const tid = messageDTO.tid
          const threadSize = messageModel.head.threadCounter
          if (tid !== null && threadSize !== null) {
            for (const threadMessageDTO of client.getMessagesInThread(tid, threadSize, isTabEnabled)) {
              const mid = threadMessageDTO.mid
              if (!threads.has(tid)) {
                threads.set(tid, new Set())
              }
              threads.get(tid)!.add(mid)
              const messageBody = client.getMessageBody(mid)
              const langCode = messageBody.lang
              const quickReply = messageBody.quickReply
              const smartReplies = messageBody.smartReplies.map((smartReply) => smartReply.text)
              messages.set(
                mid,
                FullMessage.fromMeta(
                  threadMessageDTO,
                  removeAllNonLetterSymbols(messageBody.body[0].content!),
                  this.getLanguageNameById(translationLangs, langCode),
                  this.getTranslation(client, langCode, mid),
                  quickReply,
                  smartReplies,
                ),
              )
              messageToFolder.set(
                mid,
                requireNonNull(
                  undefinedToNull(fidToFolder.get(threadMessageDTO.fid)),
                  `Folder with fid ${threadMessageDTO.fid} has no folder!`,
                ).name!,
              )
              messageToLabels.set(mid, this.getLabelNames(threadMessageDTO, lidToLabel))
            }
          } else {
            const mid = messageDTO.mid
            const messageBody = client.getMessageBody(mid)
            const langCode = messageBody.lang
            const quickReply = messageBody.quickReply
            const smartReplies = messageBody.smartReplies.map((smartReply) => smartReply.text)
            messages.set(
              mid,
              FullMessage.fromMeta(
                messageDTO,
                removeAllNonLetterSymbols(messageBody.body[0].content!),
                this.getLanguageNameById(translationLangs, langCode),
                this.getTranslation(client, langCode, mid),
                quickReply,
                smartReplies,
              ),
            )
            messageToFolder.set(
              mid,
              requireNonNull(
                undefinedToNull(fidToFolder.get(messageDTO.fid)),
                `Folder with fid ${messageDTO.fid} has no folder!`,
              ).name!,
            )
            messageToLabels.set(mid, this.getLabelNames(messageDTO, lidToLabel))
          }
        })
      }

      const folderToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>()
      const tabsToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>()

      for (const folderDTO of folderList) {
        if (isFolderOfTabType(folderDTO.type)) {
          tabsToMessages.set(formatFolderNameIfNeeded(folderDTO.name!), new Set<MessageId>())
        }
        folderToMessages.set(formatFolderNameIfNeeded(folderDTO.name!), new Set<MessageId>())
      }

      messageToFolder.forEach((folder, msg) => {
        if (isTab(folder)) {
          tabsToMessages.get(formatFolderNameIfNeeded(folder))!.add(msg)
        }
        folderToMessages.get(formatFolderNameIfNeeded(folder))!.add(msg)
      })

      const labelToMessages: Map<LabelName, Set<MessageId>> = new Map<LabelName, Set<MessageId>>()
      for (const labelDTO of labelList) {
        labelToMessages.set(labelDTO.name!, new Set<MessageId>())
      }

      messageToLabels.forEach((labels, msg) => {
        labels.forEach((label) => {
          if (!labelToMessages.has(label)) {
            labelToMessages.set(label, new Set())
          }
          labelToMessages.get(label)!.add(msg)
        })
      })

      const accountData = new AccountMailboxData(
        client,
        new MessageListDatabase(messages, folderToMessages, labelToMessages, tabsToMessages, valuesArray(threads)),
        defaultEmail,
        aliases.concat(replyTo),
        contacts,
        filters,
        accountSettings,
        zeroSuggest,
        translationLangNames,
        promoteMail360,
      )
      accountsData.push(accountData)
      this.logger.info(`Downloading account (${client.oauthAccount.account.login}) finished`)
    }

    this.logger.info('Downloading mailbox finished')
    this.logger.info('\n')

    const accountDataHandler = new MailAppModelHandler(accountsData)
    return new MailboxModel(accountDataHandler)
  }

  private getTranslation(client: MailboxClient, sourceLangCode: LanguageName, mid: ID): Map<LanguageName, string> {
    const targetLangCode: LanguageCode =
      sourceLangCode === TranslatorLanguageCode.russian
        ? TranslatorLanguageCode.english
        : TranslatorLanguageCode.russian
    const targetLangName =
      targetLangCode === TranslatorLanguageCode.english
        ? TranslatorLanguageName.english
        : TranslatorLanguageName.russian
    return new Map<LanguageName, string>().set(
      targetLangName,
      removeAllNonLetterSymbols(client.translateMessage(mid, targetLangCode).body[0].content),
    )
  }

  private getLanguageNameById(translationLangs: TranslationLangsPayload[], lang: string): string {
    const filteredLanguage = translationLangs.filter((language) => language.lang === lang)
    return filteredLanguage.length === 1 ? filteredLanguage[0].name : ''
  }

  private getLabelNames(msg: MessageMeta, lidToLabel: Map<LabelID, Label>): LabelName[] {
    const messageLids: string[] = copyArray(msg.lid)
    return messageLids.filter((lid) => lidToLabel.has(lid)).map((lid) => lidToLabel.get(lid)!.name!)
  }
}
