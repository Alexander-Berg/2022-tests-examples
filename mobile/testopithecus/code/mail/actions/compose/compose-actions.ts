import { Int32, Throwing } from '../../../../../../common/ys'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { requireNonNull, TestopithecusConstants } from '../../../../../testopithecus-common/code/utils/utils'
import { ComposeComponent } from '../../components/compose-component'
import {
  Compose,
  ComposeBody,
  ComposeBodyFeature,
  ComposeFeature,
  ComposeSenderSuggest,
  ComposeSenderSuggestFeature,
  ComposeRecipientFields,
  ComposeRecipientFieldsFeature,
  ComposeRecipientFieldType,
  ComposeSubject,
  ComposeSubjectFeature,
  ComposeRecipientSuggest,
  ComposeRecipientSuggestFeature,
} from '../../feature/compose/compose-features'

export class ComposeOpenAction extends BaseSimpleAction<Compose, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeOpenAction'
  public constructor() {
    super(ComposeOpenAction.type)
  }

  public canBePerformedImpl(model: Compose): Throwing<boolean> {
    return !model.isComposeOpened()
  }

  public requiredFeature(): Feature<Compose> {
    return ComposeFeature.get
  }

  public performImpl(modelOrApplication: Compose, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.openCompose()
    return new ComposeComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeCloseAction implements MBTAction {
  public static readonly type: MBTActionType = 'ComposeCloseAction'

  public constructor(private readonly saveDraft: boolean) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ComposeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return ComposeFeature.get.forceCast(model).isComposeOpened()
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ComposeFeature.get.forceCast(model).closeCompose(this.saveDraft)
    ComposeFeature.get.forceCast(application).closeCompose(this.saveDraft)
    return requireNonNull(history.previousDifferentComponent, 'There is no previous different component')
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'ComposeCloseAction'
  }

  public getActionType(): string {
    return ComposeCloseAction.type
  }
}

export class ComposeSendAction implements MBTAction {
  public static readonly type: MBTActionType = 'ComposeSendAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ComposeFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const composeModel = ComposeFeature.get.forceCast(model)
    const isComposeOpened = composeModel.isComposeOpened()
    const isSendButtonEnabled = composeModel.isSendButtonEnabled()
    return isComposeOpened && isSendButtonEnabled
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ComposeFeature.get.forceCast(model).sendMessage()
    ComposeFeature.get.forceCast(application).sendMessage()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous different component')
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'ComposeSendAction'
  }

  public getActionType(): string {
    return ComposeSendAction.type
  }
}

export class ComposeTapOnRecipientFieldAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnRecipientFieldAction'
  public constructor(private readonly field: ComposeRecipientFieldType) {
    super(ComposeTapOnRecipientFieldAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    return this.field === ComposeRecipientFieldType.to ? true : model.isExtendedRecipientFormShown()
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnRecipientField(this.field)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeSetRecipientFieldAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeSetRecipientFieldAction'
  public constructor(
    private readonly field: ComposeRecipientFieldType,
    private readonly value: string,
    private readonly generateYabble: boolean = true,
  ) {
    super(ComposeSetRecipientFieldAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    return this.field === ComposeRecipientFieldType.to ? true : model.isExtendedRecipientFormShown()
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.setRecipientField(this.field, this.value, this.generateYabble)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposePasteToRecipientFieldAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposePasteToRecipientFieldAction'
  public constructor(
    private readonly field: ComposeRecipientFieldType,
    private readonly value: string,
    private readonly generateYabble: boolean = true,
  ) {
    super(ComposePasteToRecipientFieldAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    return this.field === ComposeRecipientFieldType.to ? true : model.isExtendedRecipientFormShown()
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.pasteToRecipientField(this.field, this.value, this.generateYabble)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeGenerateYabbleByTapOnEnterAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeGenerateYabbleByTapOnEnterAction'
  public constructor() {
    super(ComposeGenerateYabbleByTapOnEnterAction.type)
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.generateYabbleByTapOnEnter()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnRecipientAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnRecipientAction'
  public constructor(private readonly field: ComposeRecipientFieldType, private readonly index: Int32) {
    super(ComposeTapOnRecipientAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    const isRecipientExists = model.getRecipientFieldValue(this.field).length > this.index
    const isFieldShown = this.field === ComposeRecipientFieldType.to ? true : model.isExtendedRecipientFormShown()
    return isRecipientExists && isFieldShown
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnRecipient(this.field, this.index)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeDeleteRecipientByTapOnCrossAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeDeleteRecipientByTapOnCrossAction'
  public constructor(private readonly field: ComposeRecipientFieldType, private readonly index: Int32) {
    super(ComposeDeleteRecipientByTapOnCrossAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    const isRecipientExists = model.getRecipientFieldValue(this.field).length > this.index
    const isFieldShown = this.field === ComposeRecipientFieldType.to ? true : model.isExtendedRecipientFormShown()
    return isRecipientExists && isFieldShown
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.deleteRecipientByTapOnCross(this.field, this.index)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeDeleteLastRecipientByTapOnBackspaceAction extends BaseSimpleAction<
  ComposeRecipientFields,
  MBTComponent
> {
  public static readonly type: MBTActionType = 'ComposeDeleteLastRecipientByTapOnBackspaceAction'
  public constructor(private readonly field: ComposeRecipientFieldType) {
    super(ComposeDeleteLastRecipientByTapOnBackspaceAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    const isRecipientExists = model.getRecipientFieldValue(this.field).length > 0
    const isFieldShown = this.field === ComposeRecipientFieldType.to ? true : model.isExtendedRecipientFormShown()
    return isRecipientExists && isFieldShown
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.deleteLastRecipientByTapOnBackspace(this.field)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnSenderFieldAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnSenderFieldAction'
  public constructor() {
    super(ComposeTapOnSenderFieldAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    return model.isExtendedRecipientFormShown()
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnSenderField()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeExpandExtendedRecipientFormAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeExpandExtendedRecipientFormAction'
  public constructor() {
    super(ComposeExpandExtendedRecipientFormAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    return !model.isExtendedRecipientFormShown()
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.expandExtendedRecipientForm()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeMinimizeExtendedRecipientFormAction extends BaseSimpleAction<ComposeRecipientFields, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeMinimizeExtendedRecipientFormAction'
  public constructor() {
    super(ComposeMinimizeExtendedRecipientFormAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientFields): Throwing<boolean> {
    return model.isExtendedRecipientFormShown()
  }

  public requiredFeature(): Feature<ComposeRecipientFields> {
    return ComposeRecipientFieldsFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientFields,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.minimizeExtendedRecipientForm()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnRecipientSuggestByEmailAction extends BaseSimpleAction<ComposeRecipientSuggest, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnRecipientSuggestByEmailAction'
  public constructor(
    private readonly email: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(ComposeTapOnRecipientSuggestByEmailAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientSuggest): Throwing<boolean> {
    return model
      .getRecipientSuggest()
      .map((suggest) => suggest.email)
      .includes(this.email)
  }

  public requiredFeature(): Feature<ComposeRecipientSuggest> {
    return ComposeRecipientSuggestFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientSuggest,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnRecipientSuggestByEmail(this.email)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnRecipientSuggestByIndexAction extends BaseSimpleAction<ComposeRecipientSuggest, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnRecipientSuggestByIndexAction'
  public constructor(private readonly index: Int32) {
    super(ComposeTapOnRecipientSuggestByIndexAction.type)
  }

  public canBePerformedImpl(model: ComposeRecipientSuggest): Throwing<boolean> {
    return model.getRecipientSuggest().length > this.index
  }

  public requiredFeature(): Feature<ComposeRecipientSuggest> {
    return ComposeRecipientSuggestFeature.get
  }

  public performImpl(
    modelOrApplication: ComposeRecipientSuggest,
    currentComponent: MBTComponent,
  ): Throwing<MBTComponent> {
    modelOrApplication.tapOnRecipientSuggestByIndex(this.index)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnSenderSuggestByEmailAction extends BaseSimpleAction<ComposeSenderSuggest, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnSenderSuggestByEmailAction'
  public constructor(
    private readonly email: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(ComposeTapOnSenderSuggestByEmailAction.type)
  }

  public canBePerformedImpl(model: ComposeSenderSuggest): Throwing<boolean> {
    return model.getSenderSuggest().includes(this.email)
  }

  public requiredFeature(): Feature<ComposeSenderSuggest> {
    return ComposeSenderSuggestFeature.get
  }

  public performImpl(modelOrApplication: ComposeSenderSuggest, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSenderSuggestByEmail(this.email)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnSenderSuggestByIndexAction extends BaseSimpleAction<ComposeSenderSuggest, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnSenderSuggestByIndexAction'
  public constructor(private readonly index: Int32) {
    super(ComposeTapOnSenderSuggestByIndexAction.type)
  }

  public canBePerformedImpl(model: ComposeSenderSuggest): Throwing<boolean> {
    return model.getSenderSuggest().length > this.index
  }

  public requiredFeature(): Feature<ComposeSenderSuggest> {
    return ComposeSenderSuggestFeature.get
  }

  public performImpl(modelOrApplication: ComposeSenderSuggest, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSenderSuggestByIndex(this.index)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeSetSubjectAction extends BaseSimpleAction<ComposeSubject, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeSetSubjectAction'
  public constructor(
    private readonly subject: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(ComposeSetSubjectAction.type)
  }

  public requiredFeature(): Feature<ComposeSubject> {
    return ComposeSubjectFeature.get
  }

  public performImpl(modelOrApplication: ComposeSubject, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setSubject(this.subject)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeClearSubjectAction extends BaseSimpleAction<ComposeSubject, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeClearSubjectAction'
  public constructor() {
    super(ComposeClearSubjectAction.type)
  }

  public requiredFeature(): Feature<ComposeSubject> {
    return ComposeSubjectFeature.get
  }

  public performImpl(modelOrApplication: ComposeSubject, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setSubject('')
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnSubjectFieldAction extends BaseSimpleAction<ComposeSubject, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnSubjectFieldAction'
  public constructor() {
    super(ComposeTapOnSubjectFieldAction.type)
  }

  public requiredFeature(): Feature<ComposeSubject> {
    return ComposeSubjectFeature.get
  }

  public performImpl(modelOrApplication: ComposeSubject, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnSubjectField()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeSetBodyAction extends BaseSimpleAction<ComposeBody, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeSetBodyAction'
  public constructor(
    private readonly body: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(ComposeSetBodyAction.type)
  }

  public requiredFeature(): Feature<ComposeBody> {
    return ComposeBodyFeature.get
  }

  public performImpl(modelOrApplication: ComposeBody, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setBody(this.body)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposePasteBodyAction extends BaseSimpleAction<ComposeBody, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposePasteBodyAction'
  public constructor(
    private readonly body: string,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(ComposeSetBodyAction.type)
  }

  public requiredFeature(): Feature<ComposeBody> {
    return ComposeBodyFeature.get
  }

  public performImpl(modelOrApplication: ComposeBody, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.pasteBody(this.body)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeClearBodyAction extends BaseSimpleAction<ComposeBody, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeClearBodyAction'
  public constructor() {
    super(ComposeClearBodyAction.type)
  }

  public requiredFeature(): Feature<ComposeBody> {
    return ComposeBodyFeature.get
  }

  public performImpl(modelOrApplication: ComposeBody, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.clearBody()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class ComposeTapOnBodyFieldAction extends BaseSimpleAction<ComposeBody, MBTComponent> {
  public static readonly type: MBTActionType = 'ComposeTapOnBodyFieldAction'
  public constructor() {
    super(ComposeTapOnBodyFieldAction.type)
  }

  public requiredFeature(): Feature<ComposeBody> {
    return ComposeBodyFeature.get
  }

  public performImpl(modelOrApplication: ComposeBody, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnBodyField()
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}
