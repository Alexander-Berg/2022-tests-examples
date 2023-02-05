import { requireNonNull } from '../../../testopithecus-common/code/utils/utils'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'
import { LicenseAgreementComponent } from '../component/license-agreement-component'
import { LicenseAgreement, LicenseAgreementFeature } from '../feature/license-agreement-feature'

export class OpenFullLicenseAgreementAction extends BaseSimpleAction<LicenseAgreement, MBTComponent> {
  public static readonly type: MBTActionType = 'OpenFullLicenseAgreementAction'

  public constructor() {
    super(OpenFullLicenseAgreementAction.type)
  }

  public requiredFeature(): Feature<LicenseAgreement> {
    return LicenseAgreementFeature.get
  }

  public canBePerformedImpl(model: LicenseAgreement): Throwing<boolean> {
    return model.isLicenseAgreementShown()
  }

  public performImpl(modelOrApplication: LicenseAgreement, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.openFullLicenseAgreement()
    return new LicenseAgreementComponent()
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class CloseFullLicenseAgreementAction implements MBTAction {
  public static readonly type: MBTActionType = 'CloseFullLicenseAgreementAction'

  public constructor() {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return LicenseAgreementFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelKeyboard = LicenseAgreementFeature.get.forceCast(model)
    const appKeyboard = LicenseAgreementFeature.get.forceCast(application)
    modelKeyboard.closeFullLicenseAgreement()
    appKeyboard.closeFullLicenseAgreement()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous different component')
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'CloseFullLicenseAgreementAction'
  }

  public getActionType(): string {
    return CloseFullLicenseAgreementAction.type
  }
}
