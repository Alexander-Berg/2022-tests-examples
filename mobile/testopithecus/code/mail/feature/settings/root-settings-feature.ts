import { Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class RootSettingsFeature extends Feature<RootSettings> {
  public static get: RootSettingsFeature = new RootSettingsFeature()

  private constructor() {
    super(
      'RootSettings',
      'Корневой экран настроек, достуный по тапу на кнопку "Настройки".' +
        ' В iOS - отдельный экран с возможностью перехода в General и Account и About settings,' +
        ' в Android - слит воедино с экраном General settings.',
    )
  }
}

export interface RootSettings {
  openRootSettings(): Throwing<void>

  isAboutCellExists(): Throwing<boolean>

  isHelpAndFeedbackCellExists(): Throwing<boolean>

  getAccounts(): Throwing<string[]>

  getTitle(): Throwing<string>

  closeRootSettings(): Throwing<void>
}

export class AndroidRootSettingsFeature extends Feature<AndroidRootSettings> {
  public static get: AndroidRootSettingsFeature = new AndroidRootSettingsFeature()

  private constructor() {
    super('AndroidRootSettingsFeature', 'Специфичные для Android главные настройки.')
  }
}

export interface AndroidRootSettings {
  addAccount(): Throwing<void>

  isAddAccountCellExists(): Throwing<boolean>
}

export class IOSRootSettingsFeature extends Feature<IOSRootSettings> {
  public static get: IOSRootSettingsFeature = new IOSRootSettingsFeature()

  private constructor() {
    super('IOSRootSettingsFeature', 'Специфичные для iOS главные настройки.')
  }
}

export interface IOSRootSettings {
  isGeneralSettingsCellExists(): Throwing<boolean>
}
