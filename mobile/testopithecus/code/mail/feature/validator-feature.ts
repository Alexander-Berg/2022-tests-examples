import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { FolderName } from './folder-list-features'

export class ValidatorFeature extends Feature<Validation> {
  public static get: ValidatorFeature = new ValidatorFeature()

  private constructor() {
    super('Validation', 'Проверяем присутствие различных элементов')
  }
}

export interface Validation {
  validatePossibleActionsAction(folderName: FolderName): Throwing<void>
}
