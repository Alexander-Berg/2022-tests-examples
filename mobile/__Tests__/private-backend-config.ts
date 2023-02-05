import { AccountType2 } from '../../testopithecus-common/code/mbt/test/mbt-test'
import { UserAccount } from '../../testopithecus-common/code/users/user-pool'

export const PRIVATE_BACKEND_CONFIG = {
  account: new UserAccount('yandex-team-47907-42601@yandex.ru', 'simple123456'),
  accountType: AccountType2.Yandex,
}
