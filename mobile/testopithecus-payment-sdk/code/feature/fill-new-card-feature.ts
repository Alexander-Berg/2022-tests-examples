import { Nullable, Throwing, Int32 } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { NewCardField, NewCardMode } from '../model/fill-new-card-model'

export class FillNewCardFeature extends Feature<FillNewCard> {
  public static get: FillNewCardFeature = new FillNewCardFeature()

  private constructor() {
    super('FillNewCardFeature', 'Allows to fill and get new card data')
  }
}

export interface FillNewCard {
  waitForNewCardScreen(mSec: Int32): Throwing<boolean>

  tapOnField(field: NewCardField): Throwing<void>

  setFieldValue(field: NewCardField, value: string): Throwing<void>

  pasteFieldValue(field: NewCardField, value: string): Throwing<void>

  getFieldValue(field: NewCardField): Throwing<string>

  setSaveCardCheckboxEnabled(value: boolean): Throwing<void>

  isSaveCardCheckboxEnabled(): Throwing<boolean>

  getFocusedField(): Nullable<NewCardField>

  tapOnBackButton(): Throwing<void>

  isBackButtonShown(): Throwing<boolean>

  getNewCardMode(): Nullable<NewCardMode>
}
