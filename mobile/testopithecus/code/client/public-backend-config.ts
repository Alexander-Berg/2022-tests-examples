import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import {
  OAuthApplicationCredentialsRegistry,
  OAuthCredentials,
} from '../../../testopithecus-common/code/users/oauth-service'

export class PublicBackendConfig {
  public static mailBaseUrl: string = 'https://mail.yandex.ru/api/mobile'
  public static mailYandexTeamBaseUrl: string = 'https://mail.yandex-team.ru/api/mobile'
  public static xenoBaseUrl: string = 'https://xeno.mail.yandex.net/api/mobile'

  public static mailApplicationCredentials: OAuthApplicationCredentialsRegistry = new OAuthApplicationCredentialsRegistry()
    .register(
      AccountType2.Yandex,
      new OAuthCredentials('e7618c5efed842be839cc9a580be94aa', '81a97a4e05094a4c96e9f5fa0b21f794'),
    )
    .register(
      AccountType2.YandexTeam,
      new OAuthCredentials('a517719ccf0c4aebade6cdc90a5aefe2', '71b77d7aa4d54aa09dd68526cc97bb98'),
    )

  public static baseUrl(accountType: AccountType2): string {
    switch (accountType) {
      case AccountType2.Yandex:
        return PublicBackendConfig.mailBaseUrl
      case AccountType2.YandexTeam:
        return PublicBackendConfig.mailYandexTeamBaseUrl
      default:
        return PublicBackendConfig.xenoBaseUrl
    }
  }
}
