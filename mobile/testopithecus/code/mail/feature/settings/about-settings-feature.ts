import { Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export interface AboutSettings {
  openAboutSettings(): Throwing<void>

  closeAboutSettings(): Throwing<void>

  isAppVersionValid(): Throwing<boolean>

  isCopyrightValid(): Throwing<boolean>
}

export class AboutSettingsFeature extends Feature<AboutSettings> {
  public static get: AboutSettingsFeature = new AboutSettingsFeature()

  private constructor() {
    super('AboutSettings', 'Экран с информацией о приложении. В iOS и Android открывается из Root Settings')
  }
}
