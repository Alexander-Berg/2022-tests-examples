import { Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class NewCardFieldsValidatorFeature extends Feature<NewCardFieldsValidator> {
  public static get: NewCardFieldsValidatorFeature = new NewCardFieldsValidatorFeature()

  private constructor() {
    super('NewCardFieldsValidatorFeature', 'Validate new card fields')
  }
}

export interface NewCardFieldsValidator {
  getCardNumberErrorText(): Throwing<string>

  getExpirationDateErrorText(): Throwing<string>

  getCvvErrorText(): Throwing<string>
}
