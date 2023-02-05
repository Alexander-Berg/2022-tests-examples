import { Int32, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName } from './folder-list-features'

export class ArchiveMessageFeature extends Feature<ArchiveMessage> {
  public static get: ArchiveMessageFeature = new ArchiveMessageFeature()

  private constructor() {
    super('ArchiveMessage', 'Базовая фича архивации сообщения. В мобильных по дефолту выполняется через Short swipe.')
  }
}

export interface ArchiveMessage {
  archiveMessage(order: Int32): Throwing<void>
}

export class DeleteMessageFeature extends Feature<DeleteMessage> {
  public static get: DeleteMessageFeature = new DeleteMessageFeature()

  private constructor() {
    super(
      'DeleteMessage',
      'Дефолтная фича удаления сообщения.' + 'В мобильных реализуется через full swipe, в Лизе через тулбар.',
    )
  }
}

export interface DeleteMessage {
  deleteMessage(order: Int32): Throwing<void>
}

export class MarkableImportantFeature extends Feature<MarkableImportant> {
  public static get: MarkableImportantFeature = new MarkableImportantFeature()

  private constructor() {
    super('MarkableImportant', 'Базовая фича метки важности. В мобильных по дефолту выполняется через Short swipe.')
  }
}

export interface MarkableImportant {
  markAsImportant(order: Int32): Throwing<void>

  markAsUnimportant(order: Int32): Throwing<void>
}

export class MarkableReadFeature extends Feature<MarkableRead> {
  public static get: MarkableReadFeature = new MarkableReadFeature()

  private constructor() {
    // todo почему это фича удаления?
    super(
      'MarkableRead',
      'Дефолтная фича управления статусом прочитанности письма.' +
        'В мобильных реализуется через full swipe, в Лизе через тулбар.',
    )
  }
}

export interface MarkableRead {
  markAsRead(order: Int32): Throwing<void>

  markAsUnread(order: Int32): Throwing<void>
}

export class MovableToFolderFeature extends Feature<MovableToFolder> {
  public static get: MovableToFolderFeature = new MovableToFolderFeature()

  private constructor() {
    super(
      'MovableToFolder',
      'Фича переноса соообщения в другую папку. На мобильных по дефолту реализуется через Short swipe.',
    )
  }
}

export interface MovableToFolder {
  moveMessageToFolder(order: Int32, folderName: FolderName): Throwing<void>
}

export class SpamableFeature extends Feature<Spamable> {
  public static get: SpamableFeature = new SpamableFeature()

  private constructor() {
    super('Spamable', 'Фича пометки письма спамом. По дефолту в мобильных осуществляется через Short swipe.')
  }
}

export interface Spamable {
  moveToSpam(order: Int32): Throwing<void>

  moveFromSpam(order: Int32): Throwing<void>
}
