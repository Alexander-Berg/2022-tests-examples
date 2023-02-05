import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Throwing } from '../../../../common/ys'

export class PersonalInformationFieldsValidatorFeature extends Feature<PersonalInformationFieldsValidator> {
  public static get: PersonalInformationFieldsValidatorFeature = new PersonalInformationFieldsValidatorFeature()

  private constructor() {
    super('PersonalInformationFieldsValidatorFeature', 'Validate personal information fields')
  }
}

export interface PersonalInformationFieldsValidator {
  getPhoneNumberErrorText(): Throwing<string>

  getEmailErrorText(): Throwing<string>
}
