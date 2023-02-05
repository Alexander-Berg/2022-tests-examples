import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { requireNonNull, TestopithecusConstants } from '../../../../testopithecus-common/code/utils/utils'
import { Throwing } from '../../../../../common/ys'
import { MessageComponent } from '../components/message-component'
import { GeneralSettingsComponent } from '../components/settings/general-settings-component'
import { SearchLanguageComponent } from '../components/translator/search-language-component'
import { SettingsDefaultLanguageListComponent } from '../components/translator/settings-default-language-list-component'
import { SettingsIgnoredLanguageListComponent } from '../components/translator/settings-ignored-language-list-component'
import { SourceLanguageListComponent } from '../components/translator/source-language-list-component'
import { TargetLanguageListComponent } from '../components/translator/target-language-list-component'
import {
  LanguageName,
  TranslatorBar,
  TranslatorBarFeature,
  TranslatorLanguageList,
  TranslatorLanguageListFeature,
  TranslatorLanguageListSearch,
  TranslatorLanguageListSearchFeature,
  TranslatorSettings,
  TranslatorSettingsFeature,
} from '../feature/translator-features'

export class TranslatorBarTapOnSourceLanguageAction extends BaseSimpleAction<TranslatorBar, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorBarTapOnSourceLanguageAction'

  public constructor() {
    super(TranslatorBarTapOnSourceLanguageAction.type)
  }

  public requiredFeature(): Feature<TranslatorBar> {
    return TranslatorBarFeature.get
  }

  public canBePerformedImpl(model: TranslatorBar): Throwing<boolean> {
    return model.isTranslatorBarShown()
  }

  public performImpl(modelOrApplication: TranslatorBar, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSourceLanguage()
    return new SourceLanguageListComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorBarTapOnSourceLanguageAction'
  }
}

export class TranslatorBarTapOnTargetLanguageAction extends BaseSimpleAction<TranslatorBar, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorBarTapOnTargetLanguageAction'

  public constructor() {
    super(TranslatorBarTapOnTargetLanguageAction.type)
  }

  public requiredFeature(): Feature<TranslatorBar> {
    return TranslatorBarFeature.get
  }

  public canBePerformedImpl(model: TranslatorBar): Throwing<boolean> {
    return model.isTranslatorBarShown()
  }

  public performImpl(modelOrApplication: TranslatorBar, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnTargetLanguage()
    return new TargetLanguageListComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorBarTapOnTargetLanguageAction'
  }
}

export class TranslatorBarTapOnCloseButtonAction extends BaseSimpleAction<TranslatorBar, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorBarTapOnCloseButtonAction'

  public constructor(
    private readonly hideTranslatorForThisLanguage: boolean,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(TranslatorBarTapOnCloseButtonAction.type)
  }

  public requiredFeature(): Feature<TranslatorBar> {
    return TranslatorBarFeature.get
  }

  public canBePerformedImpl(model: TranslatorBar): Throwing<boolean> {
    return model.isTranslatorBarShown()
  }

  public performImpl(modelOrApplication: TranslatorBar, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnCloseBarButton(this.hideTranslatorForThisLanguage)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorBarTapOnCloseButtonAction'
  }
}

export class TranslatorBarTapOnTranslateButtonAction extends BaseSimpleAction<TranslatorBar, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorBarTapOnTranslateButtonAction'

  public constructor() {
    super(TranslatorBarTapOnTranslateButtonAction.type)
  }

  public requiredFeature(): Feature<TranslatorBar> {
    return TranslatorBarFeature.get
  }

  public canBePerformedImpl(model: TranslatorBar): Throwing<boolean> {
    return model.isTranslatorBarShown()
  }

  public performImpl(modelOrApplication: TranslatorBar, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnTranslateButton()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorBarTapOnTranslateButtonAction'
  }
}

export class TranslatorBarTapOnRevertButtonAction extends BaseSimpleAction<TranslatorBar, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorBarTapOnRevertButtonAction'

  public constructor() {
    super(TranslatorBarTapOnRevertButtonAction.type)
  }

  public requiredFeature(): Feature<TranslatorBar> {
    return TranslatorBarFeature.get
  }

  public canBePerformedImpl(model: TranslatorBar): Throwing<boolean> {
    return model.isTranslatorBarShown()
  }

  public performImpl(modelOrApplication: TranslatorBar, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnRevertButton()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorBarTapOnRevertButtonAction'
  }
}

export class TranslatorSetTargetLanguageAction extends BaseSimpleAction<
  TranslatorLanguageList,
  TargetLanguageListComponent
> {
  public static readonly type: MBTActionType = 'TranslatorSetTargetLanguageAction'

  public constructor(
    private readonly language: LanguageName,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(TranslatorSetTargetLanguageAction.type)
  }

  public requiredFeature(): Feature<TranslatorLanguageList> {
    return TranslatorLanguageListFeature.get
  }

  public canBePerformedImpl(model: TranslatorLanguageList): Throwing<boolean> {
    return model.getAllTargetLanguages().includes(this.language)
  }

  public performImpl(
    modelOrApplication: TranslatorLanguageList,
    currentComponent: TargetLanguageListComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.setTargetLanguage(this.language, true)
    return new MessageComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `TranslatorSetTargetLanguageAction(language=${this.language})`
  }
}

export class TranslatorSetSourceLanguageAction extends BaseSimpleAction<
  TranslatorLanguageList,
  SourceLanguageListComponent
> {
  public static readonly type: MBTActionType = 'TranslatorSetSourceLanguageAction'

  public constructor(
    private readonly language: LanguageName,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(TranslatorSetSourceLanguageAction.type)
  }

  public requiredFeature(): Feature<TranslatorLanguageList> {
    return TranslatorLanguageListFeature.get
  }

  public canBePerformedImpl(model: TranslatorLanguageList): Throwing<boolean> {
    return model.getAllSourceLanguages().includes(this.language)
  }

  public performImpl(
    modelOrApplication: TranslatorLanguageList,
    currentComponent: SourceLanguageListComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.setSourceLanguage(this.language)
    return new MessageComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `TranslatorSetSourceLanguageAction(language=${this.language})`
  }
}

export class TranslatorTapOnSearchTextFieldAction extends BaseSimpleAction<TranslatorLanguageListSearch, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorTapOnSearchTextFieldAction'

  public constructor() {
    super(TranslatorTapOnSearchTextFieldAction.type)
  }

  public requiredFeature(): Feature<TranslatorLanguageListSearch> {
    return TranslatorLanguageListSearchFeature.get
  }

  public performImpl(
    modelOrApplication: TranslatorLanguageListSearch,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnSearchTextField()
    return new SearchLanguageComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorTapOnSearchTextFieldAction'
  }
}

export class TranslatorTapOnSearchCancelButtonAction implements MBTAction {
  public static readonly type: MBTActionType = 'TranslatorTapOnSearchCancelButtonAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return TranslatorLanguageListSearchFeature.get.included(modelFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return TranslatorLanguageListSearchFeature.get.forceCast(model).isSearchTextFieldFocused()
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelTranslatorLanguageListSearch = TranslatorLanguageListSearchFeature.get.forceCast(model)
    const appTranslatorLanguageListSearch = TranslatorLanguageListSearchFeature.get.forceCast(application)
    modelTranslatorLanguageListSearch.tapOnCancelButton()
    appTranslatorLanguageListSearch.tapOnCancelButton()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous screen')
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorTapOnSearchCancelButtonAction'
  }

  public getActionType(): string {
    return TranslatorTapOnSearchCancelButtonAction.type
  }
}

export class TranslatorEnterSearchQueryAction extends BaseSimpleAction<TranslatorLanguageListSearch, MBTComponent> {
  public static readonly type: MBTActionType = 'TranslatorEnterSearchQueryAction'

  public constructor(
    private readonly query: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(TranslatorEnterSearchQueryAction.type)
  }

  public requiredFeature(): Feature<TranslatorLanguageListSearch> {
    return TranslatorLanguageListSearchFeature.get
  }

  public canBePerformedImpl(model: TranslatorLanguageListSearch): Throwing<boolean> {
    return model.isSearchTextFieldFocused()
  }

  public performImpl(
    modelOrApplication: TranslatorLanguageListSearch,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.enterSearchQuery(this.query)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return `TranslatorEnterSearchQueryAction(query=${this.query})`
  }
}

export class TranslatorTapOnClearSearchButtonAction extends BaseSimpleAction<
  TranslatorLanguageListSearch,
  MBTComponent
> {
  public static readonly type: MBTActionType = 'TranslatorTapOnClearSearchButtonAction'

  public constructor() {
    super(TranslatorTapOnClearSearchButtonAction.type)
  }

  public requiredFeature(): Feature<TranslatorLanguageListSearch> {
    return TranslatorLanguageListSearchFeature.get
  }

  public canBePerformedImpl(model: TranslatorLanguageListSearch): Throwing<boolean> {
    return model.getSearchQuery() !== ''
  }

  public performImpl(
    modelOrApplication: TranslatorLanguageListSearch,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnClearSearchFieldButton()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TranslatorTapOnClearSearchButtonAction'
  }
}

export class SettingsSwitchTranslatorAction extends BaseSimpleAction<TranslatorSettings, MBTComponent> {
  public static readonly type: MBTActionType = 'SettingsSwitchTranslatorAction'

  public constructor() {
    super(SettingsSwitchTranslatorAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.switchTranslator()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsSwitchTranslatorAction'
  }
}

export class SettingsOpenIgnoredTranslationLanguageListAction extends BaseSimpleAction<
  TranslatorSettings,
  MBTComponent
> {
  public static readonly type: MBTActionType = 'SettingsOpenIgnoredTranslationLanguageListAction'

  public constructor() {
    super(SettingsOpenIgnoredTranslationLanguageListAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public canBePerformedImpl(model: TranslatorSettings): Throwing<boolean> {
    const hasIgnoredLanguages = model.getIgnoredTranslationLanguages().length > 0
    return model.isTranslatorEnabled() && hasIgnoredLanguages
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.openIgnoredTranslationLanguageList()
    return new SettingsIgnoredLanguageListComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsOpenIgnoredTranslationLanguageListAction'
  }
}

export class SettingsDeleteLanguageFromIgnoredAction extends BaseSimpleAction<TranslatorSettings, MBTComponent> {
  public static readonly type: MBTActionType = 'SettingsDeleteLanguageFromIgnoredAction'

  public constructor(
    private readonly language: LanguageName,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(SettingsDeleteLanguageFromIgnoredAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.removeTranslationLanguageFromIgnored(this.language)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsDeleteLanguageFromIgnoredAction'
  }
}

export class SettingsCloseIgnoredTranslationLanguageListAction extends BaseSimpleAction<
  TranslatorSettings,
  MBTComponent
> {
  public static readonly type: MBTActionType = 'SettingsCloseIgnoredTranslationLanguageListAction'

  public constructor() {
    super(SettingsCloseIgnoredTranslationLanguageListAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeIgnoredTranslationLanguageList()
    return new GeneralSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsCloseIgnoredTranslationLanguageListAction'
  }
}

export class SettingsOpenDefaultTranslationLanguageListAction extends BaseSimpleAction<
  TranslatorSettings,
  MBTComponent
> {
  public static readonly type: MBTActionType = 'SettingsOpenDefaultTranslationLanguageListAction'

  public constructor() {
    super(SettingsOpenDefaultTranslationLanguageListAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public canBePerformedImpl(model: TranslatorSettings): Throwing<boolean> {
    return model.isTranslatorEnabled()
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.openDefaultTranslationLanguageList()
    return new SettingsDefaultLanguageListComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsOpenDefaultTranslationLanguageListAction'
  }
}

export class SettingsSetDefaultTranslationLanguageAction extends BaseSimpleAction<TranslatorSettings, MBTComponent> {
  public static readonly type: MBTActionType = 'SettingsSetDefaultTranslationLanguageAction'

  public constructor(
    private readonly language: LanguageName,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(SettingsSetDefaultTranslationLanguageAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setDefaultTranslationLanguage(this.language)
    return new GeneralSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsSetDefaultTranslationLanguageAction'
  }
}

export class SettingsCloseDefaultTranslationLanguageListAction extends BaseSimpleAction<
  TranslatorSettings,
  MBTComponent
> {
  public static readonly type: MBTActionType = 'SettingsCloseDefaultTranslationLanguageListAction'

  public constructor() {
    super(SettingsCloseDefaultTranslationLanguageListAction.type)
  }

  public requiredFeature(): Feature<TranslatorSettings> {
    return TranslatorSettingsFeature.get
  }

  public performImpl(modelOrApplication: TranslatorSettings, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.closeDefaultTranslationLanguageList()
    return new GeneralSettingsComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'SettingsCloseDefaultTranslationLanguageListAction'
  }
}
