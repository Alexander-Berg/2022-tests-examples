import { Throwing } from '../../../../common/ys'
import { Feature } from '../../../testopithecus-common/code/mbt/mbt-abstractions'

export class PaymentScreenTitleFeature extends Feature<PaymentScreenTitle> {
  public static get: PaymentScreenTitleFeature = new PaymentScreenTitleFeature()

  private constructor() {
    super('PaymentScreenTitleFeature', 'Returns current title for payment screen(popup)')
  }
}

export interface PaymentScreenTitle {
  getTitle(): Throwing<string>
}
