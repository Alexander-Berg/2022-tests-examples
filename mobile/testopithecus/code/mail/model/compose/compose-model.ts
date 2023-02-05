import { Int32, int64, Int64, int64ToDouble, Nullable, Throwing, YSError } from '../../../../../../common/ys'
import { stringReplaceAll } from '../../../../../common/code/utils/strings'
import { Contact } from '../../../../../mapi/code/api/entities/contact/contact'
import { fail } from '../../../../../testopithecus-common/code/utils/error-thrower'
import { copyArray, requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { formatLogin } from '../../../utils/mail-utils'
import {
  Compose,
  ComposeAttachment,
  ComposeBody,
  ComposeEmailProvider,
  ComposeFieldType,
  ComposeRecipientFields,
  ComposeRecipientFieldType,
  ComposeRecipientSuggest,
  ComposeSenderSuggest,
  ComposeSubject,
  ComposeType,
  Draft,
  Yabble,
  YabbleType,
} from '../../feature/compose/compose-features'
import { FullMessageView } from '../../feature/mail-view-features'
import { DefaultFolderName } from '../folder-data-model'
import { MailAppModelHandler, MessageId } from '../mail-model'

export class ComposeConstants {
  public static readonly forwardWrapperStartMessage: string = '-------- Beginning of forwarded message -------- '
  public static readonly forwardWrapperEndMessage: string = '-------- End of forwarded message --------'
}

export class ComposeModel
  implements
    Compose,
    ComposeRecipientFields,
    ComposeRecipientSuggest,
    ComposeSenderSuggest,
    ComposeSubject,
    ComposeBody {
  private to: Yabble[] = []
  private cc: Yabble[] = []
  private bcc: Yabble[] = []
  private subject: string = ''
  private body: string = ''
  private from: string = ''
  private contacts: Contact[] = []
  private aliases: string[] = []
  private attachments: ComposeAttachment[] = []
  private reOrFwdContent: string = ''

  private composeType: ComposeType = ComposeType.clean

  private extendedRecipientFormShown: boolean = false
  private recipientSuggestShown: boolean = false
  private senderSuggestShown: boolean = false
  private composeOpened: boolean = false
  private composeEmailProvider: ComposeEmailProvider = ComposeEmailProvider.instance
  private focusedField: Nullable<ComposeFieldType> = ComposeFieldType.to
  private midOfRepliedOrForwarderMessage: Nullable<MessageId> = null

  private isBodyCleared: boolean = false

  private domains: string[] = [
    'yandex.ru',
    'yahoo.com',
    'gmail.com',
    'mail.ru',
    'rambler.ru',
    'icloud.com',
    'qip.ru',
    'bk.ru',
    'inbox.ru',
  ]

  public constructor(private readonly mailAppModelHandler: MailAppModelHandler) {}

  public openCompose(): Throwing<void> {
    this.body = `\n\n${this.getSignature()}`
    this.contacts = this.mailAppModelHandler.getCurrentAccount().contacts
    this.aliases = this.mailAppModelHandler.getCurrentAccount().aliases
    this.from = this.mailAppModelHandler.getCurrentAccount().defaultEmail
    this.composeOpened = true
  }

  public isSendButtonEnabled(): Throwing<boolean> {
    const allRecipients = this.allRecipients()
    const validYabbles = allRecipients.filter((yabble) =>
      [YabbleType.manual, YabbleType.suggested].includes(yabble.type),
    )
    const invalidYabbles = allRecipients.filter((yabble) => yabble.type === YabbleType.invalid)
    return validYabbles.length > 0 && invalidYabbles.length === 0
  }

  private createYabbleFromEmail(fullEmail: string): Yabble {
    if (fullEmail.includes('<')) {
      const email = fullEmail.slice(fullEmail.indexOf('<') + 1, fullEmail.indexOf('>'))
      const name = fullEmail.slice(fullEmail.indexOf('"') + 1, fullEmail.lastIndexOf('"'))
      return new Yabble(email, name, YabbleType.suggested)
    }
    return new Yabble(fullEmail, fullEmail, YabbleType.suggested)
  }

  private parseNumberToDateAndFormat(ts: Int64): string {
    const date = new Date(int64ToDouble(ts))
    const year = date.getFullYear()
    const month = this.formatDatePartIfNeeded(date.getMonth() + 1)
    const day = this.formatDatePartIfNeeded(date.getDate())
    const hours = this.formatDatePartIfNeeded(date.getHours())
    const minutes = this.formatDatePartIfNeeded(date.getMinutes())
    return `${day}.${month}.${year}, ${hours}:${minutes}`
  }

  private formatDatePartIfNeeded(datePart: Int32): string {
    return datePart < 10 ? `0${datePart}` : `${datePart}`
  }

  private setFieldsForReply(mid: MessageId): void {
    const message = this.getMessageByMid(mid)
    const date = this.parseNumberToDateAndFormat(message.head.timestamp)
    const from = message.head.from
    this.reOrFwdContent = `\n\n${date}, ${from}:\n${message.body}`
    this.body = `\n\n${this.getSignature()}${this.reOrFwdContent}`
    this.subject = `Re: ${message.head.subject}`
    this.midOfRepliedOrForwarderMessage = mid
    this.to = [this.createYabbleFromEmail(message.head.from)] // TODO: to, cc
  }

  private getMessageByMid(mid: MessageId): FullMessageView {
    return this.mailAppModelHandler.getCurrentAccount().messagesDB.storedMessage(mid, null)
  }

  public openReplyCompose(mid: MessageId): Throwing<void> {
    this.openCompose()
    this.setFieldsForReply(mid)
    this.composeType = ComposeType.reply
  }

  public openReplyAllCompose(mid: MessageId): Throwing<void> {
    this.openCompose()
    this.setFieldsForReply(mid)
    this.composeType = ComposeType.replyAll
  }

  public openForwardCompose(mid: MessageId): Throwing<void> {
    this.openCompose()
    const message = this.getMessageByMid(mid)
    const date = this.parseNumberToDateAndFormat(message.head.timestamp)
    this.subject = `Fwd: ${message.head.subject}`
    this.reOrFwdContent =
      `\n\n${ComposeConstants.forwardWrapperStartMessage}\n${date}, ${message.head.from}:\n` +
      `${message.body}\n\n\n${ComposeConstants.forwardWrapperEndMessage}`
    this.body = `\n\n${this.getSignature()}${this.reOrFwdContent}`
    this.composeType = ComposeType.forward
    this.midOfRepliedOrForwarderMessage = mid
  }

  public closeCompose(saveDraft: boolean): Throwing<void> {
    if (saveDraft) {
      this.saveMessage()
    }
    this.dropFields()
    this.composeOpened = false
  }

  public isComposeOpened(): Throwing<boolean> {
    return this.composeOpened
  }

  public sendMessage(): Throwing<void> {
    const isReOrFwd = this.composeType !== ComposeType.clean
    if (isReOrFwd) {
      const mid = requireNonNull(this.midOfRepliedOrForwarderMessage, 'Mid should be no null')
      const message = this.getMessageByMid(mid)
      const threadCounter = message.head.threadCounter === null ? 1 : message.head.threadCounter
      const sentMsgMid: MessageId = this.createSentMessage(threadCounter!)
      this.mailAppModelHandler.getCurrentAccount().messagesDB.addThreadMessagesToThreadWithMid([sentMsgMid], mid)
    } else {
      this.createSentMessage(0)
    }
    this.closeCompose(false)
  }

  private saveMessage(): Throwing<void> {
    const isReOrFwd = this.composeType !== ComposeType.clean
    if (isReOrFwd) {
      const mid = requireNonNull(this.midOfRepliedOrForwarderMessage, 'Mid should be non null')
      const message = this.getMessageByMid(mid)
      const threadSize = message.head.threadCounter === null ? 1 : message.head.threadCounter
      const draftMsgMid: MessageId = this.createDraftMessage(threadSize!)
      this.mailAppModelHandler.getCurrentAccount().messagesDB.addThreadMessagesToThreadWithMid([draftMsgMid], mid)
    } else {
      this.createDraftMessage(0)
    }
  }

  private createDraftMessage(threadSize: Int32): MessageId {
    const draftMessage = this.getDraft().toFullMessage(threadSize + 1, true)
    const messagesDB = this.mailAppModelHandler.getCurrentAccount().messagesDB
    const draftMsgMid = int64(messagesDB.getMessages().length + 1)
    messagesDB.addMessage(draftMsgMid, draftMessage, DefaultFolderName.draft)
    return draftMsgMid
  }

  private isMessageToYourself(): boolean {
    const allRecipientEmails = this.allRecipients().map((to) => formatLogin(to.email))
    return (
      this.aliases.filter((alias) => allRecipientEmails.includes(alias)).length > 0 ||
      allRecipientEmails.includes(this.from)
    )
  }

  private createSentMessage(threadSize: Int32): MessageId {
    const sentMessage = this.getDraft().toFullMessage(threadSize + 1, true)
    const messagesDB = this.mailAppModelHandler.getCurrentAccount().messagesDB
    const sentMsgMid = int64(messagesDB.getMessages().length + 1)
    messagesDB.addMessage(sentMsgMid, sentMessage, DefaultFolderName.sent)

    if (this.isMessageToYourself()) {
      const receivedMsgMid = int64(messagesDB.getMessages().length + 1)
      const receivedMessage = this.getDraft().toFullMessage(threadSize + 2, false)
      messagesDB.addMessage(receivedMsgMid, receivedMessage, DefaultFolderName.inbox)
      messagesDB.addThread([sentMsgMid, receivedMsgMid])
    }
    return sentMsgMid
  }

  private dropFields(): void {
    this.to = []
    this.cc = []
    this.bcc = []
    this.subject = ''
    this.body = ''
    this.from = ''
    this.extendedRecipientFormShown = false
    this.recipientSuggestShown = false
    this.senderSuggestShown = false
    this.focusedField = ComposeFieldType.to
    this.reOrFwdContent = ''
    this.midOfRepliedOrForwarderMessage = null
  }

  public getDraft(): Draft {
    return new Draft(this.to, this.cc, this.bcc, this.from, this.subject, this.body, this.attachments)
  }

  public deleteRecipientByTapOnCross(field: ComposeRecipientFieldType, index: Int32): Throwing<void> {
    this.deleteRecipientByIndex(field, index)
  }

  public deleteLastRecipientByTapOnBackspace(field: ComposeRecipientFieldType): Throwing<void> {
    const recipients = this.getRecipientFieldValue(field)
    this.deleteRecipientByIndex(field, recipients.length - 1)
  }

  private deleteRecipientByIndex(field: ComposeRecipientFieldType, index: Int32): void {
    switch (field) {
      case ComposeRecipientFieldType.to:
        this.to.splice(index, 1)
        break
      case ComposeRecipientFieldType.cc:
        this.cc.splice(index, 1)
        break
      case ComposeRecipientFieldType.bcc:
        this.bcc.splice(index, 1)
        break
    }
    this.recipientSuggestShown = false
  }

  public expandExtendedRecipientForm(): Throwing<void> {
    this.extendedRecipientFormShown = true
    this.recipientSuggestShown = false
  }

  public getRecipientFieldValue(field: ComposeRecipientFieldType): Throwing<Yabble[]> {
    switch (field) {
      case ComposeRecipientFieldType.to:
        return this.to
      case ComposeRecipientFieldType.cc:
        return this.cc
      case ComposeRecipientFieldType.bcc:
        return this.bcc
    }
  }

  public getCompactRecipientFieldValue(): Throwing<string> {
    const allEnteredEmails = this.allRecipients()
    const allEnteredEmailsLength = allEnteredEmails.length
    return allEnteredEmailsLength === 0
      ? ''
      : allEnteredEmailsLength === 1
      ? `${allEnteredEmails[0].emailOrName()}`
      : `${allEnteredEmails[0].emailOrName()} and ${allEnteredEmailsLength - 1} more`
  }

  public getFocusedField(): Nullable<ComposeFieldType> {
    return this.focusedField
  }

  public tapOnRecipient(field: ComposeRecipientFieldType, index: Int32): Throwing<void> {
    let recipientsList: Yabble[] = []
    switch (field) {
      case ComposeRecipientFieldType.to:
        recipientsList = this.to
        break
      case ComposeRecipientFieldType.cc:
        recipientsList = this.cc
        break
      case ComposeRecipientFieldType.bcc:
        recipientsList = this.bcc
        break
    }
    if (recipientsList.length <= index) {
      fail(`There is no recipient in field ${field.toString()} with index ${index}`)
      return
    }
    recipientsList[index].isActive = true
  }

  public getSenderFieldValue(): Throwing<string> {
    return this.from
  }

  public isExtendedRecipientFormShown(): Throwing<boolean> {
    return this.extendedRecipientFormShown
  }

  public minimizeExtendedRecipientForm(): Throwing<void> {
    this.extendedRecipientFormShown = false
    this.recipientSuggestShown = false
  }

  public getNotGeneratedYabble(): Throwing<Nullable<Yabble>> {
    const newYabbles = this.allRecipients().filter((yabble) => yabble.type === YabbleType.new)
    const newYabblesLength = newYabbles.length
    if (newYabblesLength > 1) {
      throw new YSError(`There are ${newYabblesLength} new yabbles. Max = 1`)
    }
    return newYabblesLength === 0 ? null : newYabbles[0]
  }

  public pasteToRecipientField(
    field: ComposeRecipientFieldType,
    value: string,
    generateYabble: boolean,
  ): Throwing<void> {
    this.setRecipientField(field, value, generateYabble)
  }

  public setRecipientField(field: ComposeRecipientFieldType, value: string, generateYabble: boolean): Throwing<void> {
    this.addYabble(field, new Yabble(value, '', this.getYabbleType(value, generateYabble)))
  }

  private isEmailValid(value: string): boolean {
    return (
      this.composeEmailProvider.validEmails.includes(value) ||
      this.from === formatLogin(value) ||
      this.aliases.includes(formatLogin(value))
    )
  }

  private getYabbleType(value: string, generateYabble: boolean): YabbleType {
    return !generateYabble ? YabbleType.new : this.isEmailValid(value) ? YabbleType.manual : YabbleType.invalid
  }

  private allRecipients(): Yabble[] {
    return this.to.concat(this.cc).concat(this.bcc)
  }

  private allEmailsEnteredToRecipientsFields(): string[] {
    return this.allRecipients().map((yabble) => yabble.email)
  }

  private generateYabbleIfNeeded(): void {
    const notGeneratedYabbles = this.allRecipients().filter((yabble) => yabble.type === YabbleType.new)
    const notGeneratedYabblesLength = notGeneratedYabbles.length
    if (notGeneratedYabblesLength > 1) {
      fail(`There are ${notGeneratedYabblesLength} new yabbles. Max = 1`)
    }
    notGeneratedYabbles.forEach((yabble) => {
      yabble.type = this.isEmailValid(yabble.email) ? YabbleType.manual : YabbleType.invalid
      yabble.email = this.isEmailValid(yabble.email) ? this.formatValidEmail(yabble.email) : yabble.email
    })
  }

  public generateYabbleByTapOnEnter(): Throwing<void> {
    this.generateYabbleIfNeeded()
    this.recipientSuggestShown = false
  }

  public tapOnRecipientField(field: ComposeRecipientFieldType): Throwing<void> {
    this.recipientSuggestShown = !this.recipientSuggestShown
    if (
      [ComposeFieldType.subject, ComposeFieldType.body, null].includes(this.focusedField) &&
      this.allRecipients().length > 0 &&
      !this.extendedRecipientFormShown &&
      field === ComposeRecipientFieldType.to
    ) {
      this.extendedRecipientFormShown = true
    }
    if (this.focusedField !== this.recipientFieldTypeToComposeFieldType(field)) {
      this.focusedField = this.recipientFieldTypeToComposeFieldType(field)
      this.generateYabbleIfNeeded()
    }
  }

  private recipientFieldTypeToComposeFieldType(field: ComposeRecipientFieldType): ComposeFieldType {
    switch (field) {
      case ComposeRecipientFieldType.to:
        return ComposeFieldType.to
      case ComposeRecipientFieldType.cc:
        return ComposeFieldType.cc
      case ComposeRecipientFieldType.bcc:
        return ComposeFieldType.bcc
    }
  }

  private composeFieldTypeToRecipientFieldType(field: ComposeFieldType): Throwing<ComposeRecipientFieldType> {
    switch (field) {
      case ComposeFieldType.to:
        return ComposeRecipientFieldType.to
      case ComposeFieldType.cc:
        return ComposeRecipientFieldType.cc
      case ComposeFieldType.bcc:
        return ComposeRecipientFieldType.bcc
      default:
        throw new YSError(
          `There is no equivalent for ComposeFieldType.${field.toString()} in ComposeRecipientFieldType enum`,
        )
    }
  }

  public tapOnSenderField(): Throwing<void> {
    this.senderSuggestShown = !this.senderSuggestShown
    this.recipientSuggestShown = false
  }

  // To/Cc/Bcc suggest
  public getRecipientSuggest(): Throwing<Contact[]> {
    const filledRecipients = this.allEmailsEnteredToRecipientsFields()
    const contacts = this.contacts.filter((contact) => !filledRecipients.includes(contact.email))
    const notGeneratedYabble = this.getNotGeneratedYabble()
    if (notGeneratedYabble === null) {
      return contacts.slice(0, 10)
    }
    const contactsInAbookMatchedWithQuery = contacts.filter(
      (contact) => contact.email.includes(notGeneratedYabble.email) || contact.name.includes(notGeneratedYabble.email),
    )
    if (contactsInAbookMatchedWithQuery.length !== 0) {
      return contactsInAbookMatchedWithQuery
    }

    // Alias suggest
    if (notGeneratedYabble.email.includes('@')) {
      const splittedEmail = notGeneratedYabble.email.split('@')
      if (splittedEmail.length === 2) {
        const enteredLogin = splittedEmail[0]
        const enteredDomain = splittedEmail[1]
        let domains = copyArray(this.domains)
        if (enteredDomain !== '') {
          domains = domains.filter((domain) => domain.startsWith(enteredDomain))
        }
        return domains
          .map((domain) => new Contact('', `${enteredLogin}@${domain}`))
          .filter((contact) => !filledRecipients.includes(contact.email))
      }
    }
    return []
  }

  public isRecipientSuggestShown(): Throwing<boolean> {
    const isSuggestEmpty = this.getRecipientSuggest().length === 0
    return this.recipientSuggestShown && !isSuggestEmpty
  }

  public tapOnRecipientSuggestByEmail(email: string): Throwing<void> {
    const contacts = this.getRecipientSuggest().filter((suggestItem) => suggestItem.email === email)
    if (contacts.length < 0) {
      fail(`There is no account with email ${email} in to/cc/bcc suggest`)
    }
    this.addYabbleBySuggest(this.composeFieldTypeToRecipientFieldType(this.focusedField!), contacts[0])
  }

  public tapOnRecipientSuggestByIndex(index: Int32): Throwing<void> {
    const contacts = this.getRecipientSuggest()
    if (contacts.length <= index) {
      fail(`There is no account with index ${index} in to/cc/bcc suggest`)
    }

    this.addYabbleBySuggest(this.composeFieldTypeToRecipientFieldType(this.focusedField!), contacts[index])
  }

  private addYabbleBySuggest(field: ComposeRecipientFieldType, contact: Contact): Throwing<void> {
    const notGeneratedYabble = this.getNotGeneratedYabble()
    if (notGeneratedYabble !== null) {
      notGeneratedYabble!.email = contact.email
      notGeneratedYabble!.name = contact.name
      notGeneratedYabble!.type = YabbleType.suggested
    } else {
      this.addYabble(field, new Yabble(contact.email, contact.name, YabbleType.suggested))
    }
    this.recipientSuggestShown = false
  }

  public formatValidEmail(email: string): string {
    let newEmail = email
    const symbolToDelete: string[] = ["'", '"']
    symbolToDelete.forEach((symbol) => {
      newEmail = stringReplaceAll(newEmail, symbol, '')
    })
    return newEmail
  }

  private addYabble(field: ComposeRecipientFieldType, yabble: Yabble): void {
    if (yabble.type === YabbleType.manual) {
      yabble.email = this.formatValidEmail(yabble.email)
    }
    this.recipientSuggestShown = yabble.type === YabbleType.new
    switch (field) {
      case ComposeRecipientFieldType.to:
        this.to.push(yabble)
        break
      case ComposeRecipientFieldType.cc:
        this.cc.push(yabble)
        break
      case ComposeRecipientFieldType.bcc:
        this.bcc.push(yabble)
        break
    }
  }

  private makeAllYabblesInactive(): void {
    this.allRecipients()
      .filter((yabble) => yabble.isActive)
      .forEach((yabble) => {
        yabble.isActive = false
      })
  }

  // From suggest
  public getSenderSuggest(): Throwing<string[]> {
    return this.aliases
  }

  public isSenderSuggestShown(): Throwing<boolean> {
    return this.senderSuggestShown
  }

  public tapOnSenderSuggestByEmail(email: string): Throwing<void> {
    this.from = email
    this.senderSuggestShown = false
  }

  public tapOnSenderSuggestByIndex(index: Int32): Throwing<void> {
    this.from = this.aliases[index]
    this.senderSuggestShown = false
  }

  // Body
  public getBody(): Throwing<string> {
    return this.body
  }

  public setBody(body: string): Throwing<void> {
    this.body = this.isBodyCleared ? body : `${body}\n\n${this.getSignature()}${this.reOrFwdContent}`
  }

  public pasteBody(body: string): Throwing<void> {
    this.setBody(body)
  }

  private getSignature(): string {
    let signature = this.mailAppModelHandler.getCurrentAccount().accountSettings.signature
    if (signature === '') {
      signature = '--\nSent from Yandex Mail for mobile'
    }
    return stringReplaceAll(signature, '<br>', '\n')
  }

  public clearBody(): Throwing<void> {
    this.body = '\n'
    this.isBodyCleared = true
  }

  public tapOnBodyField(): Throwing<void> {
    this.extendedRecipientFormShown = false
    this.recipientSuggestShown = false
    this.senderSuggestShown = false
    this.focusedField = ComposeFieldType.body
    this.generateYabbleIfNeeded()
    this.makeAllYabblesInactive()
  }

  // Subject
  public getSubject(): Throwing<string> {
    return this.subject
  }

  public setSubject(subject: string): Throwing<void> {
    this.subject = subject
  }

  public tapOnSubjectField(): Throwing<void> {
    this.extendedRecipientFormShown = false
    this.recipientSuggestShown = false
    this.senderSuggestShown = false
    this.focusedField = ComposeFieldType.subject
    this.generateYabbleIfNeeded()
    this.makeAllYabblesInactive()
  }

  // Attachments
  public getAttachments(): Throwing<ComposeAttachment[]> {
    return this.attachments
  }
}
