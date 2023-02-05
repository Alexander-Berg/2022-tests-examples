import { assertInt32Equals, assertTrue } from '../../../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../../common/ys'
import { TranslatorSettingsFeature } from '../../feature/translator-features'

export class SettingsIgnoredLanguageListComponent implements MBTComponent {
  public static readonly type: string = 'SettingsIgnoredLanguageListComponent'

  public getComponentType(): MBTComponentType {
    return SettingsIgnoredLanguageListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelTranslatorSettings = TranslatorSettingsFeature.get.castIfSupported(model)
    const appTranslatorSettings = TranslatorSettingsFeature.get.castIfSupported(application)

    if (modelTranslatorSettings !== null && appTranslatorSettings !== null) {
      const modelIgnoredTranslationLanguages = modelTranslatorSettings.getIgnoredTranslationLanguages()
      const appIgnoredTranslationLanguages = appTranslatorSettings.getIgnoredTranslationLanguages()
      assertInt32Equals(
        modelIgnoredTranslationLanguages.length,
        appIgnoredTranslationLanguages.length,
        'Number of ignored languages is incorrect',
      )

      for (const modelLanguage of modelIgnoredTranslationLanguages) {
        assertTrue(
          appIgnoredTranslationLanguages.includes(modelLanguage),
          `Application list of ignored languages not include ${modelLanguage}`,
        )
      }
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
