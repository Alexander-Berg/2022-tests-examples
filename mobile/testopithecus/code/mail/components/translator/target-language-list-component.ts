import {
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../../common/ys'
import { TranslatorLanguageListFeature } from '../../feature/translator-features'

export class TargetLanguageListComponent implements MBTComponent {
  public static readonly type: string = 'TranslatorTargetLanguageListComponent'

  public getComponentType(): MBTComponentType {
    return TargetLanguageListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelTranslatorLanguageList = TranslatorLanguageListFeature.get.castIfSupported(model)
    const appTranslatorLanguageList = TranslatorLanguageListFeature.get.castIfSupported(application)

    if (modelTranslatorLanguageList !== null && appTranslatorLanguageList !== null) {
      const modelCurrentTargetLanguage = modelTranslatorLanguageList.getCurrentTargetLanguage()
      const appCurrentTargetLanguage = appTranslatorLanguageList.getCurrentTargetLanguage()
      assertTrue(modelCurrentTargetLanguage === appCurrentTargetLanguage, 'Current target language is incorrect')

      const modelDefaultTargetLanguage = modelTranslatorLanguageList.getDefaultTargetLanguage()
      const appDefaultTargetLanguage = appTranslatorLanguageList.getDefaultTargetLanguage()
      assertStringEquals(modelDefaultTargetLanguage, appDefaultTargetLanguage, 'Default target language is incorrect')

      const modelRecentTargetLanguages = modelTranslatorLanguageList.getRecentTargetLanguages()
      const appRecentTargetLanguages = appTranslatorLanguageList.getRecentTargetLanguages()
      assertInt32Equals(
        modelRecentTargetLanguages.length,
        appRecentTargetLanguages.length,
        'Incorrect number of recent languages',
      )
      for (const modelLanguage of modelRecentTargetLanguages) {
        assertTrue(
          appRecentTargetLanguages.includes(modelLanguage),
          `Application list of recent target languages not include ${modelLanguage}`,
        )
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
