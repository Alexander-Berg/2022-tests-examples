import { Throwing } from '../../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../../testopithecus-common/code/utils/assert'
import { OpenAboutSettingsAction } from '../../actions/settings/about-settings-actions'
import { OpenGeneralSettingsAction } from '../../actions/settings/general-settings-actions'
import { CloseRootSettings, OpenSettingsAction } from '../../actions/settings/root-settings-actions'
import {
  AndroidRootSettingsFeature,
  IOSRootSettingsFeature,
  RootSettingsFeature,
} from '../../feature/settings/root-settings-feature'
import { TabBarComponent } from '../tab-bar-component'

export class RootSettingsComponent implements MBTComponent {
  public static readonly type: string = 'RootSettingsComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const rootSettingsModel = RootSettingsFeature.get.castIfSupported(model)
    const rootSettingsApplication = RootSettingsFeature.get.castIfSupported(application)

    const iOSRootSettingsModel = IOSRootSettingsFeature.get.castIfSupported(model)
    const iOSRootSettingsApplication = IOSRootSettingsFeature.get.castIfSupported(application)

    if (iOSRootSettingsModel !== null && iOSRootSettingsApplication !== null) {
      const generalSettingCellModel = iOSRootSettingsModel.isGeneralSettingsCellExists()
      const generalSettingCellApp = iOSRootSettingsApplication.isGeneralSettingsCellExists()

      assertBooleanEquals(generalSettingCellModel, generalSettingCellApp, 'There is no General settings cell')
    }

    const androidRootSettingsModel = AndroidRootSettingsFeature.get.castIfSupported(model)
    const androidRootSettingsApplication = AndroidRootSettingsFeature.get.castIfSupported(application)

    if (androidRootSettingsModel !== null && androidRootSettingsApplication !== null) {
      const addAccCellModel = androidRootSettingsModel.isAddAccountCellExists()
      const addAccCellApp = androidRootSettingsApplication.isAddAccountCellExists()

      assertBooleanEquals(addAccCellModel, addAccCellApp, 'There is no Add account cell')
    }

    if (rootSettingsModel !== null && rootSettingsApplication !== null) {
      const titleModel = rootSettingsModel.getTitle()
      const titleApp = rootSettingsApplication.getTitle()

      assertStringEquals(titleModel, titleApp, 'Incorrect root settings title')

      const accountsModel = rootSettingsModel.getAccounts()
      const accountsApplication = rootSettingsApplication.getAccounts()

      assertInt32Equals(accountsModel.length, accountsApplication.length, 'There is different number of accounts')

      for (const accountModel of accountsModel) {
        assertTrue(accountsApplication.includes(accountModel), `There is no account with name ${accountModel}`)
      }

      const aboutCellModel = rootSettingsModel.isAboutCellExists()
      const aboutCellApp = rootSettingsApplication.isAboutCellExists()

      assertBooleanEquals(aboutCellModel, aboutCellApp, 'There is no About cell')

      const helpAndFeedbackCellModel = rootSettingsModel.isHelpAndFeedbackCellExists()
      const helpAndFeedbackCellApp = rootSettingsApplication.isHelpAndFeedbackCellExists()

      assertBooleanEquals(helpAndFeedbackCellModel, helpAndFeedbackCellApp, 'There is no Help and feedback cell')
    }

    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return 'RootSettingsComponent'
  }

  public getComponentType(): string {
    return RootSettingsComponent.type
  }
}

export class AllRootSettingsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new CloseRootSettings())
    actions.push(new OpenGeneralSettingsAction())
    actions.push(new OpenSettingsAction())
    actions.push(new OpenAboutSettingsAction())
    return actions
  }
}
