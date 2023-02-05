import { Throwing } from '../../../../../../common/ys'
import { assertBooleanEquals } from '../../../../../testopithecus-common/code/utils/assert'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { CloseAboutSettingsAction } from '../../actions/settings/about-settings-actions'
import { AboutSettingsFeature } from '../../feature/settings/about-settings-feature'

export class AboutSettingsComponent implements MBTComponent {
  public static readonly type: string = 'AboutSettingsComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const aboutSettingsModel = AboutSettingsFeature.get.castIfSupported(model)
    const aboutSettingsApplication = AboutSettingsFeature.get.castIfSupported(application)
    if (aboutSettingsModel === null || aboutSettingsApplication === null) {
      return
    }

    assertBooleanEquals(
      aboutSettingsApplication.isAppVersionValid(),
      aboutSettingsModel.isAppVersionValid(),
      'Application version is incorrect',
    )

    assertBooleanEquals(
      aboutSettingsApplication.isCopyrightValid(),
      aboutSettingsModel.isCopyrightValid(),
      'Copyright is incorrect',
    )
  }

  public tostring(): string {
    return 'AboutSettingsComponent'
  }

  public getComponentType(): string {
    return AboutSettingsComponent.type
  }
}

export class AllAboutSettingsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new CloseAboutSettingsAction())
    return actions
  }
}
