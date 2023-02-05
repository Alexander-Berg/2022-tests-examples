import { Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export interface AndroidGeneralSettings {
  isDoNotDisturbModeEnabled(): Throwing<boolean>

  switchDoNotDisturbMode(): Throwing<void>

  tapToClearCacheAndCancel(): Throwing<void>

  switchAds(): Throwing<void>

  isAdsEnabled(): Throwing<boolean>
}

export class AndroidGeneralSettingsFeature extends Feature<AndroidGeneralSettings> {
  public static get: AndroidGeneralSettingsFeature = new AndroidGeneralSettingsFeature()

  private constructor() {
    super('AndroidGeneralSettings', 'Специфичные для Android основные настройки приложения.')
  }
}

export interface IosGeneralSettings {
  switchIconBadgeForActiveAccount(): Throwing<void>

  isIconBadgeForActiveAccountEnabled(): Throwing<boolean>

  switchSystemThemeSync(): Throwing<void>

  isSystemThemeSyncEnabled(): Throwing<boolean>

  openLinksIn(browser: Browser): Throwing<void>

  getSelectedBrowser(): Throwing<Browser>
}

export class IosGeneralSettingsFeature extends Feature<IosGeneralSettings> {
  public static get: IosGeneralSettingsFeature = new IosGeneralSettingsFeature()

  private constructor() {
    super('IosGeneralSettings', 'Специфичные для iOS основные настройки приложения.')
  }
}

export interface GeneralSettings {
  openGeneralSettings(): Throwing<void>

  closeGeneralSettings(): Throwing<void>

  setActionOnSwipe(action: ActionOnSwipe): Throwing<void>

  getActionOnSwipe(): Throwing<ActionOnSwipe>

  isLoginUsingPasswordEnabled(): Throwing<boolean>

  switchCompactMode(): Throwing<void>

  isCompactModeEnabled(): Throwing<boolean>

  switchDarkTheme(): Throwing<void>

  isDarkThemeEnabled(): Throwing<boolean>

  switchVoiceControl(): Throwing<void>

  isVoiceControlEnabled(): Throwing<boolean>

  setVoiceControlLanguage(language: Language): Throwing<void>

  getVoiceControlLanguage(): Throwing<Language>

  switchSmartReplies(): Throwing<void>

  isSmartRepliesEnabled(): Throwing<boolean>

  clearCache(): Throwing<void>

  setCancelSendingEmail(option: CancelSendingOption): Throwing<void>

  getCancelSendingEmail(): Throwing<CancelSendingOption>
}

export class GeneralSettingsFeature extends Feature<GeneralSettings> {
  public static get: GeneralSettingsFeature = new GeneralSettingsFeature()

  private constructor() {
    super(
      'GeneralSettings',
      'Основные настройки приложения, не зависящие от аккаунта.' +
        'Доступ в iOS открывается по тапу на кнопку General settings в Root settings' +
        'в Android - сразу при попадании в Root setting',
    )
  }
}

export enum ActionOnSwipe {
  archive = 'Archive',
  delete = 'Delete',
}

export enum Language {
  russian = 'Russian',
  english = 'English',
  ukrainian = 'Ukrainian',
  turkish = 'Turkish',
}

export enum Browser {
  yandexBrowser = 'Yandex Browser',
  safari = 'Safari',
  builtIn = 'Built-in browser',
}

export enum CancelSendingOption {
  turnOff = 'Turn off',
  threeSeconds = '3 seconds',
  fiveSeconds = '5 seconds',
  tenSeconds = '10 seconds',
}
