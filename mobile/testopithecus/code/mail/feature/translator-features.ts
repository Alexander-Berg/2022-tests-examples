import { Nullable, Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export type LanguageName = string
export type LanguageCode = string

export class TranslatorBarFeature extends Feature<TranslatorBar> {
  public static get: TranslatorBarFeature = new TranslatorBarFeature()

  private constructor() {
    super('TranslatorBar', 'Плашка переводчика в просмотре письма')
  }
}

export interface TranslatorBar {
  isTranslatorBarShown(): Throwing<boolean>

  isMessageTranslated(): Throwing<boolean>

  getSourceLanguage(): Throwing<LanguageName>

  tapOnSourceLanguage(): Throwing<void>

  getTargetLanguage(): Throwing<LanguageName>

  tapOnTargetLanguage(): Throwing<void>

  tapOnTranslateButton(): Throwing<void>

  tapOnRevertButton(): Throwing<void>

  getSubmitButtonLabel(): Throwing<string>

  tapOnCloseBarButton(hideTranslatorForThisLanguage: boolean): Throwing<void>
}

export class TranslatorLanguageListFeature extends Feature<TranslatorLanguageList> {
  public static get: TranslatorLanguageListFeature = new TranslatorLanguageListFeature()

  private constructor() {
    super('TranslatorLanguageListFeature', 'Список языков, появляющийся при тапе на язык письма/язык перевода')
  }
}

export interface TranslatorLanguageList {
  getAllSourceLanguages(): LanguageName[]

  setSourceLanguage(language: LanguageName): Throwing<void>

  getCurrentSourceLanguage(): Throwing<Nullable<LanguageName>>

  getDeterminedAutomaticallySourceLanguage(): Throwing<LanguageName>

  getRecentSourceLanguages(): Throwing<LanguageName[]>

  getAllTargetLanguages(): LanguageName[]

  setTargetLanguage(language: LanguageName, addToRecent: boolean): Throwing<void>

  getCurrentTargetLanguage(): Throwing<Nullable<LanguageName>>

  getDefaultTargetLanguage(): Throwing<LanguageName>

  getRecentTargetLanguages(): Throwing<LanguageName[]>
}

export class TranslatorLanguageListSearchFeature extends Feature<TranslatorLanguageListSearch> {
  public static get: TranslatorLanguageListSearchFeature = new TranslatorLanguageListSearchFeature()

  private constructor() {
    super('TranslatorLanguageListSearchFeature', 'Поиск на экране выбора языка в перводчике')
  }
}

export interface TranslatorLanguageListSearch {
  tapOnSearchTextField(): Throwing<void>

  isSearchTextFieldFocused(): Throwing<boolean>

  tapOnCancelButton(): Throwing<void>

  enterSearchQuery(query: string): Throwing<void>

  getSearchQuery(): Throwing<string>

  getSearchedLanguageList(): Throwing<LanguageName[]>

  tapOnClearSearchFieldButton(): Throwing<void>
}

export class TranslatorSettingsFeature extends Feature<TranslatorSettings> {
  public static get: TranslatorSettingsFeature = new TranslatorSettingsFeature()

  private constructor() {
    super('TranslatorSettingsFeature', 'Переводчик в настройках')
  }
}

export interface TranslatorSettings {
  switchTranslator(): Throwing<void>

  isTranslatorEnabled(): Throwing<boolean>

  isIgnoredLanguageCellShown(): Throwing<boolean>

  openIgnoredTranslationLanguageList(): Throwing<void>

  removeTranslationLanguageFromIgnored(language: LanguageName): Throwing<void>

  getIgnoredTranslationLanguages(): Throwing<LanguageName[]>

  closeIgnoredTranslationLanguageList(): Throwing<void>

  openDefaultTranslationLanguageList(): Throwing<void>

  setDefaultTranslationLanguage(language: LanguageName): Throwing<void>

  getDefaultTranslationLanguage(): LanguageName

  getDefaultTranslationLanguageFromGeneralSettingsPage(): LanguageName

  closeDefaultTranslationLanguageList(): Throwing<void>
}
