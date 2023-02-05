import { Application, SpectronClient } from 'spectron'
import { Throwing } from '../../../../../common/ys'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'
import { YandexTeamLogin } from '../../../code/mail/feature/login-features'

export class DesktopYandexTeamLogin implements YandexTeamLogin {
  public constructor(
    private readonly app: Application, // TODO: remove
    private readonly client: SpectronClient,
  ) {}

  public async loginWithYandexTeamAccount(account: UserAccount): Throwing<Promise<void>> {
    await this.client.setValue('[name="login"]', account.login)
    await this.client.setValue('[name="passwd"]', account.password)
    await this.client.element('[type="submit"]').click()
    await this.app.start() // TODO: remove after bug fix
  }
}
