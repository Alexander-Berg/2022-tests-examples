import { Int32, Int64, Nullable, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { LabelName } from './folder-list-features'
import { LanguageName } from './translator-features'

export class MessageViewerAndroidFeature extends Feature<MessageViewerAndroid> {
  public static get: MessageViewerAndroidFeature = new MessageViewerAndroidFeature()

  private constructor() {
    super('MessageViewerAndroid', 'Специфичные для андроида действия с открытым письмом.')
  }
}

export interface MessageViewerAndroid {
  deleteMessageByIcon(): Throwing<void>

  getDefaultSourceLanguage(): Throwing<LanguageName>
}

export class MessageViewerFeature extends Feature<MessageViewer> {
  public static get: MessageViewerFeature = new MessageViewerFeature()

  private constructor() {
    super(
      'MessageViewer',
      'Фича для управления открытым письмом. Несколько меток добавляются установкой нескольких чекбоксов в popup',
    )
  }
}

export interface MessageViewer {
  openMessage(order: Int32): Throwing<void>

  isMessageOpened(): boolean

  closeMessage(): Throwing<void>

  getOpenedMessage(): Throwing<FullMessageView>

  checkIfRead(): Throwing<boolean>

  checkIfSpam(): Throwing<boolean>

  checkIfImportant(): Throwing<boolean>

  getLabels(): Throwing<Set<string>>

  deleteLabelsFromHeader(labels: LabelName[]): Throwing<void>

  markAsUnimportantFromHeader(): Throwing<void>

  arrowDownClick(): Throwing<void>

  arrowUpClick(): Throwing<void>
}

export class ThreadViewNavigatorFeature extends Feature<ThreadViewNavigator> {
  public static readonly get: ThreadViewNavigatorFeature = new ThreadViewNavigatorFeature()

  private constructor() {
    super(
      'ThreadViewNavigator',
      'Навигационный тулбар в просмотре письма, можно переключаться между письмами треда и ' +
        'удалить/архивировать (в зависимости от действия по свайпу) тред. Есть во всех Android и в планшетах на IOS',
    )
  }
}

export interface ThreadViewNavigator {
  deleteCurrentThread(): Throwing<void>

  archiveCurrentThread(): Throwing<void>
}

export interface FullMessageView {
  readonly head: MessageView
  readonly to: Set<string>
  readonly body: string
  readonly lang: LanguageName
  readonly quickReply: boolean
  readonly smartReplies: string[]

  tostring(): string
}

export interface MessageView {
  from: string
  readonly to: string
  readonly subject: string
  readonly read: boolean
  readonly important: boolean
  threadCounter: Nullable<Int32>
  readonly attachments: AttachmentView[]
  readonly firstLine: string
  readonly timestamp: Int64

  tostring(): string
}

export interface AttachmentView {
  readonly displayName: string
}
