import { Throwing } from '../../../../../../common/ys'
import { Feature } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'

export class UndoFeature extends Feature<Undo> {
  public static get: UndoFeature = new UndoFeature()

  private constructor() {
    super('Undo', 'Отмена удаления, архивирования, отправки в спам, отправки письма')
  }
}

export interface Undo {
  undoDelete(): Throwing<void>

  undoArchive(): Throwing<void>

  undoSpam(): Throwing<void>

  undoSending(): Throwing<void>

  isUndoDeleteToastShown(): Throwing<UndoState>

  isUndoArchiveToastShown(): Throwing<UndoState>

  isUndoSpamToastShown(): Throwing<UndoState>

  isUndoSendingToastShown(): Throwing<UndoState>
}

export enum UndoState {
  shown,
  notShown,
  undefined,
}
