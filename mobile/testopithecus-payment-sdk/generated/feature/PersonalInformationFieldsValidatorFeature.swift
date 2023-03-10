// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM feature/personal-information-fields-validator-feature.ts >>>

import Foundation

open class PersonalInformationFieldsValidatorFeature: Feature<PersonalInformationFieldsValidator> {
  public static var `get`: PersonalInformationFieldsValidatorFeature = PersonalInformationFieldsValidatorFeature()
  private init() {
    super.init("PersonalInformationFieldsValidatorFeature", "Validate personal information fields")
  }

}

public protocol PersonalInformationFieldsValidator {
  @discardableResult
  func getPhoneNumberErrorText() throws -> String
  @discardableResult
  func getEmailErrorText() throws -> String
}

