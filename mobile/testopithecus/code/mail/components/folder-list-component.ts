import { Int32, Nullable, Throwing } from '../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../testopithecus-common/code/utils/assert'
import { keysArray, requireNonNull } from '../../../../testopithecus-common/code/utils/utils'
import {
  ClearFolderInFolderListFeature,
  FolderName,
  FolderNavigatorFeature,
  LabelNavigatorFeature,
} from '../feature/folder-list-features'
import { MultiAccountFeature } from '../feature/login-features'
import { TabBarComponent } from './tab-bar-component'

export class FolderListComponent implements MBTComponent {
  public static readonly type: string = 'FolderListComponent'

  public getComponentType(): string {
    return FolderListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const multiAccountModel = MultiAccountFeature.get.castIfSupported(model)
    const multiAccountApplication = MultiAccountFeature.get.castIfSupported(application)

    if (multiAccountModel !== null && multiAccountApplication !== null) {
      const currAppAcc = multiAccountApplication.getCurrentAccount()
      const currModelAcc = multiAccountModel.getCurrentAccount()
      assertStringEquals(currModelAcc, currAppAcc, 'User Email is incorrect')

      const accNumApp = multiAccountApplication.getNumberOfAccounts()
      const accNumModel = multiAccountModel.getNumberOfAccounts()
      assertInt32Equals(accNumModel, accNumApp, 'The number of accounts is incorrect')
    }

    const folderNavigatorModel = FolderNavigatorFeature.get.castIfSupported(model)
    const folderNavigatorApplication = FolderNavigatorFeature.get.castIfSupported(application)

    if (folderNavigatorModel !== null && folderNavigatorApplication !== null) {
      const folderListModel = this.formatFolderNameToCounterList(folderNavigatorModel.getFoldersList())
      const folderNamesModel = keysArray(folderListModel)
      const folderListApplication = this.formatFolderNameToCounterList(
        folderNavigatorApplication.getFoldersList(),
        folderNamesModel,
      )
      const folderNamesApplication = keysArray(folderListApplication)
      assertInt32Equals(folderNamesModel.length, folderNamesApplication.length, 'Different number of folders')

      for (const folderName of folderNamesApplication) {
        assertTrue(folderNamesModel.includes(folderName), `There is no folder ${folderName} in model`)
      }

      for (const folderName of folderNamesApplication) {
        assertInt32Equals(
          folderListModel.get(folderName)!,
          folderListApplication.get(folderName)!,
          `Folder ${folderName} has incorrect unread counter`,
        )
      }

      const modelCurrentContainer = folderNavigatorModel.getCurrentContainer()
      const appCurrentContainer = folderNavigatorApplication.getCurrentContainer()
      assertTrue(
        modelCurrentContainer === appCurrentContainer,
        `Different current folder. Expected: ${modelCurrentContainer}. Actual: ${appCurrentContainer}`,
      )
    }

    const labelNavigatorModel = LabelNavigatorFeature.get.castIfSupported(model)
    const labelNavigatorApplication = LabelNavigatorFeature.get.castIfSupported(application)

    if (labelNavigatorModel !== null && labelNavigatorApplication !== null) {
      const labelListModel = labelNavigatorModel.getLabelList()
      const labelListApplication = labelNavigatorApplication.getLabelList()

      assertInt32Equals(labelListModel.length, labelListApplication.length, 'Different number of labels')

      for (const label of labelListApplication) {
        const labelName = requireNonNull(
          this.getNotTrimmedContainerNameIfNeeded(label, labelListModel),
          `There is no label started with ${label}`,
        )
        assertTrue(labelListModel.includes(labelName), `There is no label ${label}`)
      }
    }

    const clearFolderModel = ClearFolderInFolderListFeature.get.castIfSupported(model)
    const clearFolderApp = ClearFolderInFolderListFeature.get.castIfSupported(application)
    if (clearFolderModel !== null && clearFolderApp !== null) {
      assertBooleanEquals(
        clearFolderModel.doesClearTrashButtonExist(),
        clearFolderApp.doesClearTrashButtonExist(),
        'Different clear trash button existence state',
      )
      assertBooleanEquals(
        clearFolderModel.doesClearSpamButtonExist(),
        clearFolderApp.doesClearSpamButtonExist(),
        'Different clear spam button existence state',
      )
    }

    await new TabBarComponent().assertMatches(model, application)
  }

  private getFolderNameFromFullFolderName(fullFolderName: FolderName): FolderName {
    return fullFolderName.split('|').reverse()[0]
  }

  private formatFolderNameToCounterList(
    folderList: Map<FolderName, Int32>,
    notTrimmedFolderNames: Nullable<FolderName[]> = null,
  ): Map<FolderName, Int32> {
    const result: Map<FolderName, Int32> = new Map()
    for (const fullFolderName of folderList.keys()) {
      const folderName = this.getFolderNameFromFullFolderName(fullFolderName)
      const formattedFolderName =
        notTrimmedFolderNames === null
          ? folderName
          : requireNonNull(
              this.getNotTrimmedContainerNameIfNeeded(folderName, notTrimmedFolderNames),
              `There is no folder started with ${folderName}`,
            )
      result.set(formattedFolderName, folderList.get(fullFolderName)!)
    }
    return result
  }

  private getNotTrimmedContainerNameIfNeeded(
    containerName: string,
    notTrimmedContainerNames: string[],
  ): Nullable<string> {
    if (!containerName.includes('...')) {
      return containerName
    }
    const containerNameWithoutDots = containerName.substring(0, containerName.length - 4)
    for (const notTrimmedContainerName of notTrimmedContainerNames) {
      if (notTrimmedContainerName.includes(containerNameWithoutDots)) {
        return notTrimmedContainerName
      }
    }
    return null
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class FolderListActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    return actions
  }
}
