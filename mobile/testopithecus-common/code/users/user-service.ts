import { SyncNetwork } from '../client/network/sync-network'
import { Logger } from '../../../common/code/logging/logger'
import { Int32, Nullable, Throwing } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  NetworkMethod,
  NetworkRequest,
  RequestEncoding,
  UrlRequestEncoding,
} from '../../../common/code/network/network-request'
import { AccountType2 } from '../mbt/test/mbt-test'
import { extractErrorMessage } from '../utils/utils'
import { TusEnv, UserServiceEnsemble } from './user-service-ensemble'

export class UserServiceAccount {
  public constructor(public login: string, public password: string, public uid: string) {}
}

/**
 * Дока https://wiki.yandex-team.ru/test-user-service/
 */
export class UserService {
  public constructor(private network: SyncNetwork, private jsonSerializer: JSONSerializer, private logger: Logger) {}

  public static userServiceUrl: string = 'https://tus.yandex-team.ru'
  // Get token from here: https://wiki.yandex-team.ru/test-user-service/#autentifikacija
  // And place it into USER_SERVICE_OAUTH_TOKEN environment variable
  public static userServiceOauthToken: string = ''

  public getAccount(
    tusEnv: TusEnv,
    tusConsumer: string,
    tag: Nullable<string>,
    lockDuration: Int32,
    ignoreLocks: boolean,
    uidd: Nullable<string>,
  ): Nullable<UserServiceAccount> {
    let response: MapJSONItem
    try {
      response = this.syncRequest(new GetAccountRequest(tusEnv, tusConsumer, tag, lockDuration, ignoreLocks, uidd))
    } catch (e) {
      this.logger.error(`Failed to get account, error: ${extractErrorMessage(e)}`)
      return null
    }
    const account = response.get('account') as MapJSONItem
    if (account === null) {
      return null
    }

    const login = this.getFullLogin(account.getString(`login`), tag)
    const password = account.getString('password')
    const uid = account.getString('uid')
    if (login === null || password === null || uid === null) {
      return null
    }
    this.logger.info(`Got account login=${login!} password=${password!} uid=${uid!}`)
    return new UserServiceAccount(login!, password!, uid!)
  }

  private getFullLogin(login: Nullable<string>, tag: Nullable<string>): Nullable<string> {
    if (login === null || tag === null) {
      return null
    }
    switch (tag) {
      case UserServiceEnsemble.getTagByAccountType(AccountType2.Yandex):
        return `${login!}@${tag!}.ru`
      case UserServiceEnsemble.getTagByAccountType(AccountType2.YandexTeam):
        return `${login!}@${tag!}.ru`
      case UserServiceEnsemble.getTagByAccountType(AccountType2.Rambler):
        return `${login!}@${tag!}.ru`
      case UserServiceEnsemble.getTagByAccountType(AccountType2.YandexTest):
        return login!
      case UserServiceEnsemble.getTagByAccountType(AccountType2.Mail):
        return `${login!}@${tag!}`
      default:
        return `${login!}@${tag!}.com`
    }
  }

  public unlockAccount(tusEnv: TusEnv, uid: string): void {
    try {
      this.syncRequest(new UnlockAccountRequest(tusEnv, uid))
    } catch (e) {
      this.logger.error(`Account was not unlocked. error: ${extractErrorMessage(e)}`)
    }
  }

  private syncRequest(networkRequest: NetworkRequest): Throwing<MapJSONItem> {
    const oauthToken = UserService.userServiceOauthToken
    if (oauthToken === '') {
      this.logger.error(
        'Empty OAuth token for Test User Service. Get token here: ' +
          'https://wiki.yandex-team.ru/test-user-service/#autentifikacija and place it into ' +
          'USER_SERVICE_OAUTH_TOKEN environment variable',
      )
      throw new Error('Empty OAuth token for TUS')
    }
    const response = this.network.syncExecuteWithRetries(3, UserService.userServiceUrl, networkRequest, oauthToken)
    const json = this.jsonSerializer.deserialize(response.tryGetValue()).getValue()
    return json as MapJSONItem
  }
}

class GetAccountRequest extends BaseNetworkRequest {
  public constructor(
    private tusEnv: TusEnv,
    private tusConsumer: string,
    private tag: Nullable<string>,
    private lockDuration: Int32,
    private ignoreLocks: boolean,
    private uid: Nullable<string>,
  ) {
    super()
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public params(): MapJSONItem {
    return new MapJSONItem()
      .putString('env', this.tusEnv.toString())
      .putString('tus_consumer', this.tusConsumer)
      .putInt32('lock_duration', this.lockDuration)
      .putBoolean('ignore_locks', this.ignoreLocks)
      .putStringIfPresent('tags', this.tag)
      .putStringIfPresent('uid', this.uid)
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem()
  }

  public targetPath(): string {
    return '1/get_account/'
  }
}

class UnlockAccountRequest extends BaseNetworkRequest {
  public constructor(private readonly tusEnv: TusEnv, private readonly uid: string) {
    super()
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public params(): MapJSONItem {
    return new MapJSONItem().putString('env', this.tusEnv.toString()).putString('uid', this.uid)
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem()
  }

  public targetPath(): string {
    return '1/unlock_account/'
  }
}
