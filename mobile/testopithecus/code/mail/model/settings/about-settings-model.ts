import { Throwing } from '../../../../../../common/ys'
import { AboutSettings } from '../../feature/settings/about-settings-feature'

export class AboutSettingsModel implements AboutSettings {
  public constructor() {}

  public openAboutSettings(): Throwing<void> {}

  public closeAboutSettings(): Throwing<void> {}

  public isAppVersionValid(): Throwing<boolean> {
    return true
  }

  public isCopyrightValid(): Throwing<boolean> {
    return true
  }
}
