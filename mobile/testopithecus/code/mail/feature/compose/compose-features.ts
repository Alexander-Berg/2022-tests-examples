import { stringReplaceAll } from '../../../../../common/code/utils/strings'
import { Contact } from '../../../../../mapi/code/api/entities/contact/contact'
import { Int32, Int64, Nullable, range, Throwing } from '../../../../../../common/ys'
import { Log } from '../../../../../common/code/logging/logger'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { copyArray, currentTimeMs } from '../../../../../testopithecus-common/code/utils/utils'
import { getRandomInt32 } from '../../../utils/mail-utils'
import { FullMessage, Message, MessageAttach } from '../../model/mail-model'
import { AttachmentView } from '../mail-view-features'

export class WysiwygFeature extends Feature<WYSIWIG> {
  public static get: WysiwygFeature = new WysiwygFeature()

  private constructor() {
    super('WYSIWYG', 'Фича написания письма с форматированием (What You See Is What You Get).')
  }
}

export interface WYSIWIG {
  setStrong(from: Int32, to: Int32): Throwing<void>

  setItalic(from: Int32, to: Int32): Throwing<void>

  clearFormatting(from: Int32, to: Int32): Throwing<void>

  appendText(index: Int32, text: string): Throwing<void>
}

export interface DraftView {
  to: Set<string>
  subject: Nullable<string>

  getWysiwyg(): Throwing<WysiwygView>

  tostring(): string
}

export interface WysiwygView {
  getText(): Throwing<string>

  getStyles(i: Int32): Throwing<Set<string>>

  getRichBody(): Throwing<string>
}

export class ComposeEmailProvider {
  public static readonly instance: ComposeEmailProvider = new ComposeEmailProvider()
  public readonly emailToReceiveFwdMessage: string = 'yndx-test-acc-to-receive-email@yandex.ru'
  public readonly emailWithLatinAndCyrillicLetters: string = 'yandex-team-user@штаны.админкапдд.рф'

  // TODO: add more valid emails
  public readonly validEmails: string[] = [
    'account name<test.email@yandex.ru>',
    'name <test@test.tt>',
    '"test@test.com"',
    'yndx-test-acc-to-receive-email@yandex.ru',
    'yandex-team-user@штаны.админкапдд.рф',
  ]

  // TODO: add more invalid emails
  public readonly invalidEmails: string[] = ['a', '3', 'ц@', '@', '@.', 'te st@te st.te st']

  public getRandomValidEmail(): string {
    return this.validEmails[getRandomInt32(this.validEmails.length)]
  }

  public getRandomInvalidEmail(): string {
    return this.invalidEmails[getRandomInt32(this.invalidEmails.length)]
  }
}

export enum YabbleType {
  suggested = 'suggested',
  manual = 'manual',
  invalid = 'invalid',
  new = 'new',
}

export enum ComposeRecipientFieldType {
  to = 'to',
  cc = 'cc',
  bcc = 'bcc',
}

export enum ComposeFieldType {
  to = 'to',
  cc = 'cc',
  bcc = 'bcc',
  from = 'from',
  subject = 'subject',
  body = 'body',
}

export class Draft {
  public constructor(
    public to: Yabble[] = [],
    public cc: Yabble[] = [],
    public bcc: Yabble[] = [],
    public from: string = '',
    public subject: string = '',
    public body: string = '',
    public attachments: ComposeAttachment[] = [],
    public timestamp: Int64 = currentTimeMs(),
  ) {}

  public static matches(first: Draft, second: Draft): boolean {
    return (
      this.isRecipientsEqual(first.to, second.to) &&
      this.isRecipientsEqual(first.cc, second.cc) &&
      this.isRecipientsEqual(first.bcc, second.bcc) &&
      first.from === second.from &&
      first.subject === second.subject &&
      first.body === second.body &&
      this.isAttachmentsEqual(first.attachments, second.attachments)
    )
  }

  private static isAttachmentsEqual(attachments1: ComposeAttachment[], attachments2: ComposeAttachment[]): boolean {
    if (attachments1.length !== attachments2.length) {
      Log.error(`Different number of attachments. Attachments1: ${attachments1}, Attachments2: ${attachments2}`)
      return false
    }
    for (const i of range(0, attachments1.length)) {
      if (!ComposeAttachment.matches(attachments1[i], attachments2[i])) {
        Log.error(`Different attachment: ${attachments1[i]}, ${attachments2[i]}`)
        return false
      }
    }
    return true
  }

  private static isRecipientsEqual(recipients1: Yabble[], recipients2: Yabble[]): boolean {
    if (recipients1.length !== recipients2.length) {
      Log.error(`Different number of yabbles. Recipients1: ${recipients1}, Recipients2: ${recipients2}`)
      return false
    }
    for (const i of range(0, recipients1.length)) {
      if (!Yabble.matches(recipients1[i], recipients2[i])) {
        Log.error(`Different yabbles: ${recipients1[i]}, ${recipients2[i]}`)
        return false
      }
    }
    return true
  }

  public copy(): Draft {
    return new Draft(
      copyArray(this.to),
      copyArray(this.cc),
      copyArray(this.bcc),
      this.from,
      this.subject,
      this.body,
      copyArray(this.attachments),
    )
  }

  public toFullMessage(threadCounter: Int32, read: boolean): FullMessage {
    return new FullMessage(
      new Message(
        this.from,
        this.subject === '' ? '(No subject)' : this.subject.slice(0, 767), // backend trim long subject
        this.timestamp,
        this.getFirstline(this.body),
        threadCounter,
        read,
        false,
        this.attachments.map((attachment) => attachment.toMessageAttach()),
        this.to.concat(this.cc).map((to) => to.emailOrName())[0],
      ),
      new Set<string>(this.to.map((to) => to.emailOrName())),
      this.body,
    )
  }

  private getFirstline(body: string): string {
    return stringReplaceAll(body.split('--')[0], '\n', '')
  }

  public tostring(): string {
    return `To: ${this.to}, Cc: ${this.cc}, Bcc: ${this.bcc}, From: ${this.from}, Subject: ${this.subject}, Body: ${this.body}, Attachments: ${this.attachments} `
  }
}

export class ComposeAttachment {
  public constructor(public name: string, public size: string) {}

  public static matches(first: ComposeAttachment, second: ComposeAttachment): boolean {
    return first.name === second.name && first.size === second.size
  }

  public toMessageAttach(): AttachmentView {
    return new MessageAttach(this.name)
  }
}

export interface YabbleView {
  readonly email: string
  readonly name: string
  readonly type: YabbleType
  readonly isActive: boolean
}

export class Yabble implements YabbleView {
  public constructor(
    public email: string,
    public name: string = '',
    public type: YabbleType = YabbleType.manual,
    public isActive: boolean = false,
  ) {}

  public emailOrName(): string {
    return this.type === YabbleType.suggested && !this.isActive && this.name.length > 0 ? this.name : this.email
  }

  public static matches(first: Yabble, second: Yabble): boolean {
    return (
      first.emailOrName() === second.emailOrName() && first.type === second.type && first.isActive === second.isActive
    )
  }

  public copy(): Yabble {
    return new Yabble(this.email, this.name, this.type, this.isActive)
  }

  public tostring(): string {
    return `Email: ${this.email}, name: ${this.name}, type: ${this.type.toString()}, active: ${this.isActive}`
  }
}

export enum ComposeType {
  clean,
  reply,
  replyAll,
  forward,
}

export interface Compose {
  openCompose(): Throwing<void>

  isComposeOpened(): Throwing<boolean>

  closeCompose(saveDraft: boolean): Throwing<void>

  sendMessage(): Throwing<void>

  isSendButtonEnabled(): Throwing<boolean>
}

export class ComposeFeature extends Feature<Compose> {
  public static get: ComposeFeature = new ComposeFeature()

  private constructor() {
    super('Compose', 'Открытие/закрытие компоуза, отправка письма')
  }
}

export class ComposeRecipientFieldsFeature extends Feature<ComposeRecipientFields> {
  public static get: ComposeRecipientFieldsFeature = new ComposeRecipientFieldsFeature()

  private constructor() {
    super(
      'ComposeRecipientFields',
      'Взаимодействие с полями получателей (Кому/Копия/Скрытая копия) и отправителя (От кого) в Компоузе',
    )
  }
}

export interface ComposeRecipientFields {
  tapOnRecipientField(field: ComposeRecipientFieldType): Throwing<void>

  pasteToRecipientField(field: ComposeRecipientFieldType, value: string, generateYabble: boolean): Throwing<void>

  setRecipientField(field: ComposeRecipientFieldType, value: string, generateYabble: boolean): Throwing<void>

  generateYabbleByTapOnEnter(): Throwing<void>

  getRecipientFieldValue(field: ComposeRecipientFieldType): Throwing<Yabble[]>

  getCompactRecipientFieldValue(): Throwing<string>

  tapOnRecipient(field: ComposeRecipientFieldType, index: Int32): Throwing<void>

  deleteRecipientByTapOnCross(field: ComposeRecipientFieldType, index: Int32): Throwing<void>

  deleteLastRecipientByTapOnBackspace(field: ComposeRecipientFieldType): Throwing<void>

  tapOnSenderField(): Throwing<void>

  getSenderFieldValue(): Throwing<string>

  expandExtendedRecipientForm(): Throwing<void>

  minimizeExtendedRecipientForm(): Throwing<void>

  isExtendedRecipientFormShown(): Throwing<boolean>
}

export class ComposeRecipientSuggestFeature extends Feature<ComposeRecipientSuggest> {
  public static get: ComposeRecipientSuggestFeature = new ComposeRecipientSuggestFeature()

  private constructor() {
    super(
      'ComposeRecipientSuggest',
      'Взаимодействие с саджестом получателей для полей Кому/Копия/Скрытая копия в Компоузе',
    )
  }
}

export interface ComposeRecipientSuggest {
  isRecipientSuggestShown(): Throwing<boolean>

  getRecipientSuggest(): Throwing<Contact[]>

  tapOnRecipientSuggestByEmail(email: string): Throwing<void>

  tapOnRecipientSuggestByIndex(index: Int32): Throwing<void>
}

export class ComposeSenderSuggestFeature extends Feature<ComposeSenderSuggest> {
  public static get: ComposeSenderSuggestFeature = new ComposeSenderSuggestFeature()

  private constructor() {
    super('ComposeSenderSuggest', 'Взаимодействие с саджестом адресов отправителя в Компоузе')
  }
}

export interface ComposeSenderSuggest {
  isSenderSuggestShown(): Throwing<boolean>

  getSenderSuggest(): Throwing<string[]>

  tapOnSenderSuggestByEmail(email: string): Throwing<void>

  tapOnSenderSuggestByIndex(index: Int32): Throwing<void>
}

export class ComposeSubjectFeature extends Feature<ComposeSubject> {
  public static get: ComposeSubjectFeature = new ComposeSubjectFeature()

  private constructor() {
    super('ComposeSubject', 'Взаимодействие с полем Тема в Компоузе')
  }
}

export interface ComposeSubject {
  getSubject(): Throwing<string>

  setSubject(subject: string): Throwing<void>

  tapOnSubjectField(): Throwing<void>
}

export class ComposeBodyFeature extends Feature<ComposeBody> {
  public static get: ComposeBodyFeature = new ComposeBodyFeature()

  private constructor() {
    super('ComposeBody', 'Взаимодействие с полем Тело в Компоузе')
  }
}

export interface ComposeBody {
  getBody(): Throwing<string>

  setBody(body: string): Throwing<void>

  pasteBody(body: string): Throwing<void>

  clearBody(): Throwing<void>

  tapOnBodyField(): Throwing<void>
}
