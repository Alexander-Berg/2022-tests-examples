import { Throwing } from '../../../../../common/ys'
import { FolderName } from '../feature/folder-list-features'
import { Validation } from '../feature/validator-feature'

export class ValidatorModel implements Validation {
  public validatePossibleActionsAction(folderName: FolderName): Throwing<void> {}
}
