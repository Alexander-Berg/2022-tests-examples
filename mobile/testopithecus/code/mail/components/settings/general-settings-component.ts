import { Throwing } from '../../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { assertBooleanEquals, assertStringEquals } from '../../../../../testopithecus-common/code/utils/assert'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { CloseAboutSettingsAction } from '../../actions/settings/about-settings-actions'
import { ClearCacheAction } from '../../actions/settings/general-settings-actions'
import { PinFeature } from '../../feature/pin-feature'
import {
  AndroidGeneralSettingsFeature,
  GeneralSettingsFeature,
  IosGeneralSettingsFeature,
} from '../../feature/settings/general-settings-feature'
import { TranslatorSettingsFeature } from '../../feature/translator-features'

export class GeneralSettingsComponent implements MBTComponent {
  public static readonly type: string = 'GeneralSettingsComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const generalSettingsModel = GeneralSettingsFeature.get.castIfSupported(model)
    const generalSettingsApplication = GeneralSettingsFeature.get.castIfSupported(application)

    const androidGeneralSettingsModel = AndroidGeneralSettingsFeature.get.castIfSupported(model)
    const androidGeneralSettingsApplication = AndroidGeneralSettingsFeature.get.castIfSupported(application)

    const iOSGeneralSettingsModel = IosGeneralSettingsFeature.get.castIfSupported(model)
    const iOSGeneralSettingsApplication = IosGeneralSettingsFeature.get.castIfSupported(application)

    const pinModel = PinFeature.get.castIfSupported(model)
    const pinApplication = PinFeature.get.castIfSupported(application)

    if (generalSettingsModel !== null && generalSettingsApplication !== null) {
      assertStringEquals(
        generalSettingsModel.getActionOnSwipe().toString(),
        generalSettingsApplication.getActionOnSwipe().toString(),
        'Action on swipe is incorrect',
      )

      if (pinModel !== null && pinApplication !== null) {
        assertBooleanEquals(
          generalSettingsModel.isLoginUsingPasswordEnabled(),
          generalSettingsApplication.isLoginUsingPasswordEnabled(),
          'Pin status is incorrect',
        )
      }

      assertBooleanEquals(
        generalSettingsModel.isCompactModeEnabled(),
        generalSettingsApplication.isCompactModeEnabled(),
        'Compact mode status is incorrect',
      )

      assertBooleanEquals(
        generalSettingsModel.isDarkThemeEnabled(),
        generalSettingsApplication.isDarkThemeEnabled(),
        'Dark theme status is incorrect',
      )

      assertBooleanEquals(
        generalSettingsModel.isVoiceControlEnabled(),
        generalSettingsApplication.isVoiceControlEnabled(),
        'Voice control status is incorrect',
      )

      assertStringEquals(
        generalSettingsModel.getVoiceControlLanguage().toString(),
        generalSettingsApplication.getVoiceControlLanguage().toString(),
        'Voice control language is incorrect',
      )

      assertStringEquals(
        generalSettingsModel.getCancelSendingEmail().toString(),
        generalSettingsModel.getCancelSendingEmail().toString(),
        'Cancel sending email option is incorrect',
      )
    }

    if (androidGeneralSettingsModel !== null && androidGeneralSettingsApplication !== null) {
      // Пока только для аккаутов привязанных к стафу и не доступно для обычных аккаунтов
      // assertBooleanEquals(
      //   androidGeneralSettingsModel.isSyncCalendarEnabled(),
      //   androidGeneralSettingsApplication.isSyncCalendarEnabled(),
      //   'Sync calendar status is incorrect',
      // )

      assertBooleanEquals(
        androidGeneralSettingsModel.isDoNotDisturbModeEnabled(),
        androidGeneralSettingsApplication.isDoNotDisturbModeEnabled(),
        'Do not disturb mode status is incorrect',
      )

      assertBooleanEquals(
        androidGeneralSettingsModel.isAdsEnabled(),
        androidGeneralSettingsModel.isAdsEnabled(),
        'Ads status is incorrect',
      )
    }

    if (iOSGeneralSettingsModel !== null && iOSGeneralSettingsApplication !== null) {
      assertBooleanEquals(
        iOSGeneralSettingsModel.isIconBadgeForActiveAccountEnabled(),
        iOSGeneralSettingsApplication.isIconBadgeForActiveAccountEnabled(),
        'Icon badge for active account state is incorrect',
      )

      assertBooleanEquals(
        iOSGeneralSettingsModel.isSystemThemeSyncEnabled(),
        iOSGeneralSettingsApplication.isSystemThemeSyncEnabled(),
        'System theme sync status is incorrect',
      )

      assertStringEquals(
        iOSGeneralSettingsModel.getSelectedBrowser().toString(),
        iOSGeneralSettingsApplication.getSelectedBrowser().toString(),
        'Selected browser is incorrect',
      )

      // TODO: Вернуть обратно в общий для обеих платформ if, когда обновится приложение Android
      assertBooleanEquals(
        generalSettingsModel!.isSmartRepliesEnabled(),
        generalSettingsApplication!.isSmartRepliesEnabled(),
        'Smart replies status is incorrect',
      )
    }

    const modelTranslatorSettings = TranslatorSettingsFeature.get.castIfSupported(model)
    const appTranslatorSettings = TranslatorSettingsFeature.get.castIfSupported(application)

    if (modelTranslatorSettings !== null && appTranslatorSettings !== null) {
      const modelTranslatorEnabled = modelTranslatorSettings.isTranslatorEnabled()
      const appTranslatorEnabled = appTranslatorSettings.isTranslatorEnabled()

      assertBooleanEquals(modelTranslatorEnabled, appTranslatorEnabled, 'Translator enable status is incorrect')

      const modelIgnoredLanguageCellShown = modelTranslatorSettings.isIgnoredLanguageCellShown()
      const appIgnoredLanguageCellShown = appTranslatorSettings.isIgnoredLanguageCellShown()

      assertBooleanEquals(
        modelIgnoredLanguageCellShown,
        appIgnoredLanguageCellShown,
        'Ignored language cell show status is incorrect',
      )

      const modelDefaultTranslationLanguage = modelTranslatorSettings.getDefaultTranslationLanguageFromGeneralSettingsPage()
      const appDefaultTranslationLanguage = appTranslatorSettings.getDefaultTranslationLanguageFromGeneralSettingsPage()

      assertStringEquals(
        modelDefaultTranslationLanguage,
        appDefaultTranslationLanguage,
        'Default language is incorrect',
      )
    }
  }

  public tostring(): string {
    return 'GeneralSettingsComponent'
  }

  public getComponentType(): string {
    return GeneralSettingsComponent.type
  }
}

export class AllGeneralSettingsActions implements MBTComponentActions {
  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new CloseAboutSettingsAction())
    actions.push(new ClearCacheAction())
    return actions
  }
}
