import { assertStringEquals } from '../../../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../../../common/ys'
import { TranslatorSettingsFeature } from '../../feature/translator-features'

export class SettingsDefaultLanguageListComponent implements MBTComponent {
  public static readonly type: string = 'SettingsDefaultLanguageListComponent'

  public getComponentType(): MBTComponentType {
    return SettingsDefaultLanguageListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const modelTranslatorSettings = TranslatorSettingsFeature.get.castIfSupported(model)
    const appTranslatorSettings = TranslatorSettingsFeature.get.castIfSupported(application)

    if (modelTranslatorSettings !== null && appTranslatorSettings !== null) {
      const modelDefaultTranslationLanguage = modelTranslatorSettings.getDefaultTranslationLanguage()
      const appDefaultTranslationLanguage = appTranslatorSettings.getDefaultTranslationLanguage()

      assertStringEquals(
        modelDefaultTranslationLanguage,
        appDefaultTranslationLanguage,
        'Default translation language is incorrect',
      )
    }
  }

  public tostring(): string {
    return this.getComponentType()
  }
}
