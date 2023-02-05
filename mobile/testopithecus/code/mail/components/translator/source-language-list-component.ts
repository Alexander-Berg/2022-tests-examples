import {
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../../common/ys'
import { TranslatorLanguageListFeature } from '../../feature/translator-features'

export class SourceLanguageListComponent implements MBTComponent {
  public static readonly type: string = 'TranslatorSourceLanguageListComponent'

  public getComponentType(): MBTComponentType {
    return SourceLanguageListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelTranslatorLanguageList = TranslatorLanguageListFeature.get.castIfSupported(model)
    const appTranslatorLanguageList = TranslatorLanguageListFeature.get.castIfSupported(application)

    if (modelTranslatorLanguageList !== null && appTranslatorLanguageList !== null) {
      const modelCurrentSourceLanguage = modelTranslatorLanguageList.getCurrentSourceLanguage()
      const appCurrentSourceLanguage = appTranslatorLanguageList.getCurrentSourceLanguage()
      assertTrue(modelCurrentSourceLanguage === appCurrentSourceLanguage, 'Current source language is incorrect')

      const modelDeterminedAutomaticallySourceLanguage = modelTranslatorLanguageList.getDeterminedAutomaticallySourceLanguage()
      const appDeterminedAutomaticallySourceLanguage = appTranslatorLanguageList.getDeterminedAutomaticallySourceLanguage()
      assertStringEquals(
        modelDeterminedAutomaticallySourceLanguage,
        appDeterminedAutomaticallySourceLanguage,
        'Determined automatically source language is incorrect',
      )

      const modelRecentSourceLanguages = modelTranslatorLanguageList.getRecentSourceLanguages()
      const appRecentSourceLanguages = appTranslatorLanguageList.getRecentSourceLanguages()
      assertInt32Equals(
        modelRecentSourceLanguages.length,
        appRecentSourceLanguages.length,
        'Incorrect number of recent languages',
      )
      for (const modelLanguage of modelRecentSourceLanguages) {
        assertTrue(
          appRecentSourceLanguages.includes(modelLanguage),
          `Application list of recent source languages not include ${modelLanguage}`,
        )
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
