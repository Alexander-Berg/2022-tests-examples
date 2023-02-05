import { undefinedToNull } from '../../../../common/ys'
import { BankLogo } from '../../code/models/bank-logo'
import { BankLogoItem } from '../../code/models/bank-logo-item'

interface Logo {
  full?: {
    light?: string
    dark?: string
    mono?: string
  }
  short?: Logo['full']
}

export function bankLogo(map: Logo): BankLogo {
  const logoItem = (value: Logo['full'] | undefined): BankLogoItem =>
    new BankLogoItem(undefinedToNull(value?.light), undefinedToNull(value?.dark), undefinedToNull(value?.mono))
  return new BankLogo(logoItem(map.full), logoItem(map.short))
}
