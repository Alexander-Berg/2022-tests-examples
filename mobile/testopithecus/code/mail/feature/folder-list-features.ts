import { Int32, Nullable, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export type FolderName = string
export type LabelName = string

export class FolderNavigatorFeature extends Feature<FolderNavigator> {
  public static get: FolderNavigatorFeature = new FolderNavigatorFeature()

  private constructor() {
    super('FolderNavigator', 'Фича списка папок: открыть/закрыть список, переход в папку')
  }
}

export interface FolderNavigator {
  openFolderList(): Throwing<void>

  closeFolderList(): Throwing<void>

  getFoldersList(): Throwing<Map<FolderName, Int32>>

  goToFolder(folderDisplayName: string, parentFolders: string[]): Throwing<void>

  isInTabsMode(): Throwing<boolean>

  ptrFoldersList(): Throwing<void>

  getCurrentContainer(): Throwing<Nullable<string>>
}

export class FilterNavigatorFeature extends Feature<FilterNavigator> {
  public static get: FilterNavigatorFeature = new FilterNavigatorFeature()

  public constructor() {
    super('FilterNavigator', 'Навигатор по фильтрам из сайдбара.')
  }
}

export interface FilterNavigator {
  goToFilterImportant(): Throwing<void>

  goToFilterUnread(): Throwing<void>

  goToFilterWithAttachments(): Throwing<void>
}

export class LabelNavigatorFeature extends Feature<LabelNavigator> {
  public static get: LabelNavigatorFeature = new LabelNavigatorFeature()

  public constructor() {
    super('LabelNavigator', 'Фича для простмотра списка писем с соответствующей меткой. Открывается через сайдбар.')
  }
}

export interface LabelNavigator {
  getLabelList(): Throwing<LabelName[]>

  goToLabel(labelName: string): Throwing<void>
}

export class ClearFolderInFolderListFeature extends Feature<ClearFolderInFolderList> {
  public static get: ClearFolderInFolderListFeature = new ClearFolderInFolderListFeature()

  public constructor() {
    super(
      'ClearFolderInFolderList',
      'Фича очистки папок Спам и Удаленные через список писем, от confirmDeletionIfNeeded зависит, подтвердим ли мы удаление или нет',
    )
  }
}

export interface ClearFolderInFolderList {
  clearSpam(confirmDeletionIfNeeded: boolean): Throwing<void>

  doesClearSpamButtonExist(): Throwing<boolean>

  clearTrash(confirmDeletionIfNeeded: boolean): Throwing<void>

  doesClearTrashButtonExist(): Throwing<boolean>
}
