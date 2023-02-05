import { Nullable, Throwing } from '../../../../../common/ys'
import {
  LanguageCode,
  LanguageName,
  TranslatorBar,
  TranslatorLanguageList,
  TranslatorLanguageListSearch,
  TranslatorSettings,
} from '../feature/translator-features'
import { MailAppModelHandler } from './mail-model'

export class TranslatorLanguageName {
  public static readonly auto: LanguageName = 'auto'
  public static readonly select: LanguageName = 'select'
  public static readonly english: LanguageName = 'English'
  public static readonly russian: LanguageName = 'Russian'
  public static readonly afrikaans: LanguageName = 'Afrikaans'
  public static readonly albanian: LanguageName = 'Albanian'
  public static readonly amharic: LanguageName = 'Amharic'
  public static readonly arabic: LanguageName = 'Arabic'
}

export class TranslatorLanguageCode {
  public static readonly english: LanguageCode = 'en'
  public static readonly russian: LanguageCode = 'ru'
}

export class TranslatorSubmitButtonLabel {
  public static readonly translate: string = 'Translate'
  public static readonly revert: string = 'Revert'
}

export class TranslatorBarModel implements TranslatorBar {
  private sourceLanguage: LanguageName = TranslatorLanguageName.auto
  private targetLanguage: LanguageName = TranslatorLanguageName.english
  private messageTranslated: boolean = false
  private translateBarShown: boolean = true
  private isBarForced: boolean = false

  public constructor(private readonly translatorSettingsModel: TranslatorSettingsModel) {}

  public setSourceLanguage(language: LanguageName): void {
    this.sourceLanguage = language
  }

  public setTargetLanguage(language: LanguageName): void {
    this.targetLanguage = language
  }

  public getSourceLanguage(): Throwing<LanguageName> {
    return this.sourceLanguage
  }

  public getTargetLanguage(): Throwing<LanguageName> {
    return this.targetLanguage
  }

  public forceShowBar(): Throwing<void> {
    this.isBarForced = true
  }

  public isTranslatorBarShown(): Throwing<boolean> {
    return this.isBarForced || (this.sourceLanguage !== this.targetLanguage && this.translateBarShown)
  }

  public setTranslateBarState(shown: boolean): void {
    this.translateBarShown = shown
  }

  public tapOnCloseBarButton(hideTranslatorForThisLanguage: boolean): Throwing<void> {
    this.setTranslateBarState(false)
    if (this.sourceLanguage !== TranslatorLanguageName.auto && hideTranslatorForThisLanguage) {
      this.translatorSettingsModel.addTranslationLanguageToIgnored(this.sourceLanguage)
    }
  }

  public tapOnRevertButton(): Throwing<void> {
    this.messageTranslated = false
  }

  public tapOnSourceLanguage(): Throwing<void> {
    // do nothing
  }

  public tapOnTargetLanguage(): Throwing<void> {
    // do nothing
  }

  public tapOnTranslateButton(): Throwing<void> {
    this.messageTranslated = true
  }

  public getSubmitButtonLabel(): Throwing<string> {
    return this.messageTranslated ? TranslatorSubmitButtonLabel.revert : TranslatorSubmitButtonLabel.translate
  }

  public setMessageTranslateStatus(status: boolean): void {
    this.messageTranslated = status
  }

  public isMessageTranslated(): Throwing<boolean> {
    return this.messageTranslated
  }
}

export class TranslatorLanguageListModel implements TranslatorLanguageList {
  public constructor(
    private readonly translatorSettingsModel: TranslatorSettingsModel,
    private readonly mailAppModelHandler: MailAppModelHandler,
    private readonly translatorBarModel: TranslatorBarModel,
  ) {
    this.defaultTargetLanguage = this.translatorSettingsModel.getDefaultTranslationLanguage()
  }

  private recentSourceLanguages: LanguageName[] = []
  private recentTargetLanguages: LanguageName[] = []
  private currentSourceLanguage: LanguageName = TranslatorLanguageName.english
  private currentTargetLanguage: LanguageName = TranslatorLanguageName.english
  private defaultTargetLanguage: LanguageName
  private determinedAutomaticallySourceLanguage: LanguageName = TranslatorLanguageName.english

  public getAllLanguages(): LanguageName[] {
    return this.mailAppModelHandler.getCurrentAccount().translationLangs
  }

  public getAllSourceLanguages(): LanguageName[] {
    return this.getAllLanguages()
  }

  public getAllTargetLanguages(): LanguageName[] {
    return this.getAllLanguages()
  }

  public getCurrentSourceLanguage(): Throwing<Nullable<LanguageName>> {
    return this.currentSourceLanguage === this.determinedAutomaticallySourceLanguage ? null : this.currentSourceLanguage
  }

  public getCurrentTargetLanguage(): Throwing<Nullable<LanguageName>> {
    return this.currentTargetLanguage === this.defaultTargetLanguage ? null : this.currentTargetLanguage
  }

  public getDefaultTargetLanguage(): Throwing<LanguageName> {
    const newDefaultLanguage = this.translatorSettingsModel.getDefaultTranslationLanguage()
    if (this.defaultTargetLanguage !== newDefaultLanguage) {
      this.defaultTargetLanguage = newDefaultLanguage
    }
    return this.defaultTargetLanguage
  }

  public getDeterminedAutomaticallySourceLanguage(): Throwing<LanguageName> {
    return this.determinedAutomaticallySourceLanguage
  }

  public getRecentSourceLanguages(): Throwing<LanguageName[]> {
    return this.recentSourceLanguages
      .filter(
        (language) => ![this.currentSourceLanguage, this.determinedAutomaticallySourceLanguage].includes(language),
      )
      .slice(0, 3)
  }

  public getRecentTargetLanguages(): Throwing<LanguageName[]> {
    return this.recentTargetLanguages
      .filter((language) => ![this.currentTargetLanguage, this.defaultTargetLanguage].includes(language))
      .slice(0, 3)
  }

  public setSourceLanguage(language: LanguageName): Throwing<void> {
    this.currentSourceLanguage = language
    this.translatorBarModel.setSourceLanguage(language)
    this.recentSourceLanguages.unshift(language)
  }

  public setTargetLanguage(language: LanguageName, addToRecent: boolean): Throwing<void> {
    this.currentTargetLanguage = language
    this.translatorBarModel.setTargetLanguage(language)
    if (addToRecent) {
      this.recentTargetLanguages.unshift(language)
    }
  }

  public setDeterminedAutomaticallySourceLanguage(language: LanguageName): Throwing<void> {
    this.determinedAutomaticallySourceLanguage = language
  }
}

export class TranslatorLanguageListSearchModel implements TranslatorLanguageListSearch {
  public constructor(private readonly translatorLanguageListModel: TranslatorLanguageListModel) {}

  private searchQuery: string = ''
  private searchTextFieldFocused: boolean = false

  public enterSearchQuery(query: string): Throwing<void> {
    this.searchQuery = query
  }

  public getSearchQuery(): Throwing<string> {
    return this.searchQuery
  }

  public tapOnCancelButton(): Throwing<void> {
    this.searchTextFieldFocused = false
    this.searchQuery = ''
  }

  public tapOnClearSearchFieldButton(): Throwing<void> {
    this.searchQuery = ''
  }

  public tapOnSearchTextField(): Throwing<void> {
    this.searchTextFieldFocused = true
  }

  public isSearchTextFieldFocused(): Throwing<boolean> {
    return this.searchTextFieldFocused
  }

  public getSearchedLanguageList(): Throwing<LanguageName[]> {
    return this.translatorLanguageListModel.getAllLanguages().filter((language) => language.includes(this.searchQuery))
  }
}

export class TranslatorSettingsModel implements TranslatorSettings {
  private translatorEnabled: boolean = true
  private defaultTranslationLanguage: LanguageName = TranslatorLanguageName.english
  private ignoredTranslationLanguages: LanguageName[] = []

  public closeDefaultTranslationLanguageList(): Throwing<void> {
    // do nothing
  }

  public closeIgnoredTranslationLanguageList(): Throwing<void> {
    // do nothing
  }

  public getDefaultTranslationLanguage(): LanguageName {
    return this.defaultTranslationLanguage
  }

  public getIgnoredTranslationLanguages(): Throwing<LanguageName[]> {
    return this.ignoredTranslationLanguages
  }

  public isLanguageIgnored(lang: LanguageName): boolean {
    return this.ignoredTranslationLanguages.includes(lang)
  }

  public isTranslatorEnabled(): Throwing<boolean> {
    return this.translatorEnabled
  }

  public openDefaultTranslationLanguageList(): Throwing<void> {
    // do nothing
  }

  public openIgnoredTranslationLanguageList(): Throwing<void> {
    // do nothing
  }

  public addTranslationLanguageToIgnored(language: LanguageName): Throwing<void> {
    this.ignoredTranslationLanguages.push(language)
  }

  public removeTranslationLanguageFromIgnored(language: LanguageName): Throwing<void> {
    const index = this.ignoredTranslationLanguages.lastIndexOf(language)
    if (index !== -1) {
      this.ignoredTranslationLanguages.splice(index, 1)
    }
  }

  public setDefaultTranslationLanguage(language: LanguageName): Throwing<void> {
    this.defaultTranslationLanguage = language
  }

  public switchTranslator(): Throwing<void> {
    this.translatorEnabled = !this.translatorEnabled
  }

  public getDefaultTranslationLanguageFromGeneralSettingsPage(): LanguageName {
    return this.defaultTranslationLanguage
  }

  public isIgnoredLanguageCellShown(): Throwing<boolean> {
    return this.ignoredTranslationLanguages.length > 0
  }
}
