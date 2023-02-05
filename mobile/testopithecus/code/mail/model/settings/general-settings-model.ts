import { Throwing } from '../../../../../../common/ys'
import {
  ActionOnSwipe,
  AndroidGeneralSettings,
  Browser,
  CancelSendingOption,
  GeneralSettings,
  IosGeneralSettings,
  Language,
} from '../../feature/settings/general-settings-feature'
import { PinModel } from './pin-model'

export class GeneralSettingsModel implements GeneralSettings, IosGeneralSettings, AndroidGeneralSettings {
  public constructor(private readonly pin: PinModel) {}

  private actionOnSwipe: ActionOnSwipe = ActionOnSwipe.delete
  private cancelSendingOption: CancelSendingOption = CancelSendingOption.threeSeconds
  private browserToOpenLinks: Browser = Browser.safari
  private voiceControlLanguage: Language = Language.english
  private adsEnabled: boolean = true
  private systemThemeSyncEnabled: boolean = true
  private darkThemeEnabled: boolean = false
  private compactModeEnabled: boolean = false
  private smartRepliesEnabled: boolean = true
  private voiceControlEnabled: boolean = true
  private iconBadgeEnabledForActiveAccount: boolean = false
  private doNotDisturbMode: boolean = false

  public openGeneralSettings(): Throwing<void> {}

  public closeGeneralSettings(): Throwing<void> {}

  public clearCache(): Throwing<void> {}

  public isDoNotDisturbModeEnabled(): Throwing<boolean> {
    return this.doNotDisturbMode
  }

  public switchDoNotDisturbMode(): Throwing<void> {
    this.doNotDisturbMode = !this.doNotDisturbMode
  }

  public tapToClearCacheAndCancel(): Throwing<void> {}

  public setActionOnSwipe(action: ActionOnSwipe): Throwing<void> {
    this.actionOnSwipe = action
  }

  public getActionOnSwipe(): Throwing<ActionOnSwipe> {
    return this.actionOnSwipe
  }

  public getCancelSendingEmail(): Throwing<CancelSendingOption> {
    return this.cancelSendingOption
  }

  public getSelectedBrowser(): Throwing<Browser> {
    return this.browserToOpenLinks
  }

  public getVoiceControlLanguage(): Throwing<Language> {
    return this.voiceControlLanguage
  }

  public isIconBadgeForActiveAccountEnabled(): Throwing<boolean> {
    return this.iconBadgeEnabledForActiveAccount
  }

  public isLoginUsingPasswordEnabled(): Throwing<boolean> {
    return this.pin.isLoginUsingPasswordEnabled()
  }

  public switchIconBadgeForActiveAccount(): Throwing<void> {
    this.iconBadgeEnabledForActiveAccount = !this.iconBadgeEnabledForActiveAccount
  }

  public isAdsEnabled(): Throwing<boolean> {
    return this.adsEnabled
  }

  public isCompactModeEnabled(): Throwing<boolean> {
    return this.compactModeEnabled
  }

  public isDarkThemeEnabled(): Throwing<boolean> {
    return this.darkThemeEnabled
  }

  public isSmartRepliesEnabled(): Throwing<boolean> {
    return this.smartRepliesEnabled
  }

  public isSystemThemeSyncEnabled(): Throwing<boolean> {
    return this.systemThemeSyncEnabled
  }

  public isVoiceControlEnabled(): Throwing<boolean> {
    return this.voiceControlEnabled
  }

  public openLinksIn(browser: Browser): Throwing<void> {
    this.browserToOpenLinks = browser
  }

  public setCancelSendingEmail(option: CancelSendingOption): Throwing<void> {
    this.cancelSendingOption = option
  }

  public setVoiceControlLanguage(language: Language): Throwing<void> {
    this.voiceControlLanguage = language
  }

  public switchAds(): Throwing<void> {
    this.adsEnabled = !this.adsEnabled
  }

  public switchCompactMode(): Throwing<void> {
    this.compactModeEnabled = !this.compactModeEnabled
  }

  public switchDarkTheme(): Throwing<void> {
    this.darkThemeEnabled = !this.darkThemeEnabled
  }

  public switchSmartReplies(): Throwing<void> {
    this.smartRepliesEnabled = !this.smartRepliesEnabled
  }

  public switchSystemThemeSync(): Throwing<void> {
    this.systemThemeSyncEnabled = !this.systemThemeSyncEnabled
  }

  public switchVoiceControl(): Throwing<void> {
    this.voiceControlEnabled = !this.voiceControlEnabled
  }
}
