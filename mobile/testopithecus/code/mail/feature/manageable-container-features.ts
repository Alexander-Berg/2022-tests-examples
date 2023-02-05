import { Int32, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName, LabelName } from './folder-list-features'

export class ManageableFolderFeature extends Feature<ManageableFolder> {
  public static get: ManageableFolderFeature = new ManageableFolderFeature()

  private constructor() {
    super('ManageableFolder', 'Создание/изменение/удаление папок. Открывается из списка папок.')
  }
}

export interface ManageableFolder {
  openFolderManager(): Throwing<void>

  closeFolderManager(): Throwing<void>

  openCreateFolderScreen(): Throwing<void>

  closeCreateFolderScreen(): Throwing<void>

  enterNameForNewFolder(folderName: FolderName): Throwing<void>

  getCurrentNewFolderName(): Throwing<FolderName>

  getCurrentParentFolderForNewFolder(): Throwing<string>

  submitNewFolder(): Throwing<void>

  openEditFolderScreen(folderName: FolderName, parentFolders: FolderName[]): Throwing<void>

  closeEditFolderScreen(): Throwing<void>

  enterNameForEditedFolder(folderName: FolderName): Throwing<void>

  getCurrentEditedFolderName(): Throwing<FolderName>

  getCurrentParentFolderForEditedFolder(): Throwing<string>

  submitEditedFolder(): Throwing<void>

  selectParentFolder(parentFolders: FolderName[]): Throwing<void>

  openFolderLocationScreen(): Throwing<void>

  getFolderListForFolderLocationScreen(): Throwing<FolderName[]>

  closeFolderLocationScreen(): Throwing<void>

  deleteFolder(
    folderDisplayName: FolderName,
    parentFolders: FolderName[],
    deletionMethod: ContainerDeletionMethod,
  ): Throwing<void>

  getFolderListForManageFolderScreen(): Throwing<FolderName[]>
}

export class CreatableFolderFeature extends Feature<CreatableFolder> {
  public static get: CreatableFolderFeature = new CreatableFolderFeature()

  private constructor() {
    super('CreatableFolder', 'TODO: добрый человек, напиши тут, про что эта фича')
  }
}

export interface CreatableFolder {
  createFolder(folderDisplayName: string): Throwing<void>
}

export class ManageableLabelFeature extends Feature<ManageableLabel> {
  public static get: ManageableLabelFeature = new ManageableLabelFeature()

  private constructor() {
    super('ManageableLabel', 'Создание/изменение/удаление меток. Открывается из списка папок.')
  }
}

export interface ManageableLabel {
  openLabelManager(): Throwing<void>

  closeLabelManager(): Throwing<void>

  openCreateLabelScreen(): Throwing<void>

  closeCreateLabelScreen(): Throwing<void>

  enterNameForNewLabel(labelName: LabelName): Throwing<void>

  getCurrentNewLabelName(): Throwing<LabelName>

  setNewLabelColor(index: Int32): Throwing<void>

  getCurrentNewLabelColorIndex(): Throwing<Int32>

  submitNewLabel(): Throwing<void>

  openEditLabelScreen(labelName: LabelName): Throwing<void>

  closeEditLabelScreen(): Throwing<void>

  enterNameForEditedLabel(labelName: LabelName): Throwing<void>

  getCurrentEditedLabelName(): Throwing<LabelName>

  getCurrentEditedLabelColorIndex(): Throwing<Int32>

  setEditedLabelColor(index: Int32): Throwing<void>

  submitEditedLabel(): Throwing<void>

  deleteLabel(labelName: LabelName, deletionMethod: ContainerDeletionMethod): Throwing<void>

  getLabelList(): Throwing<LabelName[]>
}

export class CreatableLabelFeature extends Feature<CreatableLabel> {
  public static get: CreatableLabelFeature = new CreatableLabelFeature()

  public constructor() {
    super('CreatableLabel', 'Фича для создания и удаления метки из списка меток. Открывается через меню письма.')
  }
}

export interface CreatableLabel {
  createLabel(labelName: string): Throwing<void>

  removeLabel(labelName: string): Throwing<void>
}

export enum ContainerDeletionMethod {
  longSwipe,
  shortSwipe,
  tap,
}
