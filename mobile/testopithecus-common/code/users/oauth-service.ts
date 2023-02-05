import { Nullable, Throwing, undefinedToNull } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  NetworkMethod,
  RequestEncoding,
  UrlRequestEncoding,
} from '../../../common/code/network/network-request'
import { SyncNetwork } from '../client/network/sync-network'
import { AccountType2 } from '../mbt/test/mbt-test'
import { requireNonNull } from '../utils/utils'
import { UserAccount } from './user-pool'

export class OauthHostsConfig {
  public constructor(
    public readonly yandex: string = 'https://oauth.yandex.ru',
    public readonly yandexTeam: string = 'https://oauth.yandex-team.ru',
    public readonly yandexTest: string = 'https://oauth-test.yandex.ru',
  ) {}
}

export class OauthService {
  public constructor(
    private applicationCredentials: OAuthApplicationCredentialsRegistry,
    private network: SyncNetwork,
    private jsonSerializer: JSONSerializer,
    private readonly hostsConfig: OauthHostsConfig = new OauthHostsConfig(),
  ) {}

  public getToken(account: UserAccount, type: AccountType2): Throwing<Nullable<string>> {
    const oauthHost = this.getOauthHost(type)
    const credentials = this.getOAuthCredentials(type)
    if (credentials === null) {
      return null
    }
    return this.getTokenForCredentials(account, oauthHost, credentials)
  }

  public getTokenForCredentials(
    account: UserAccount,
    oauthHost: string,
    credentials: OAuthCredentials,
  ): Throwing<string> {
    const response = this.network.syncExecuteWithRetries(3, oauthHost, new TokenRequest(account, credentials), null)
    const json = this.jsonSerializer.deserialize(response.tryGetValue()).getValue()
    return requireNonNull((json as MapJSONItem).getString('access_token'), 'No access_token!')
  }

  private getOAuthCredentials(type: AccountType2): Nullable<OAuthCredentials> {
    return this.applicationCredentials.getCredentials(type)
  }

  private getOauthHost(accountType: AccountType2): string {
    switch (accountType) {
      case AccountType2.Yandex:
        return this.hostsConfig.yandex
      case AccountType2.YandexTeam:
        return this.hostsConfig.yandexTeam
      case AccountType2.YandexTest:
        return this.hostsConfig.yandexTest
      default:
        throw new Error('Пока неизвестно, как получать токены не в яндексе')
    }
  }
}

class TokenRequest extends BaseNetworkRequest {
  public constructor(private readonly account: UserAccount, private readonly oauthCredentials: OAuthCredentials) {
    super()
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public params(): MapJSONItem {
    return new MapJSONItem()
      .putString('grant_type', 'password')
      .putString('username', this.account.login)
      .putString('password', this.account.password)
      .putString('client_id', this.oauthCredentials.clientId)
      .putString('client_secret', this.oauthCredentials.clientSecret)
  }

  public targetPath(): string {
    return 'token'
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem()
  }
}

export class OAuthCredentials {
  public constructor(public readonly clientId: string, public readonly clientSecret: string) {}
}

export class OAuthApplicationCredentialsRegistry {
  private credentials: Map<AccountType2, OAuthCredentials> = new Map<AccountType2, OAuthCredentials>()

  public constructor() {}

  public getCredentials(type: AccountType2): Nullable<OAuthCredentials> {
    return undefinedToNull(this.credentials.get(type))
  }

  public register(accountType: AccountType2, clientData: OAuthCredentials): OAuthApplicationCredentialsRegistry {
    this.credentials.set(accountType, clientData)
    return this
  }
}
