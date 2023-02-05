import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { Nullable, Throwing } from '../../../../common/ys'
import { PersonalInformationField } from '../model/personal-information-model'

export class PersonalInformationFeature extends Feature<PersonalInformation> {
  public static get: PersonalInformationFeature = new PersonalInformationFeature()

  private constructor() {
    super('PersonalInformationFeature', 'Allows to fill and get personal information data')
  }
}

export interface PersonalInformation {
  tapOnField(field: PersonalInformationField): Throwing<void>

  setFieldValue(field: PersonalInformationField, value: string): Throwing<void>

  pasteFieldValue(field: PersonalInformationField, value: string): Throwing<void>

  getFieldValue(field: PersonalInformationField): Throwing<string>

  getFocusedField(): Nullable<PersonalInformationField>
}
