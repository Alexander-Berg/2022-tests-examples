import { SyncNetwork } from '../../../testopithecus-common/code/client/network/sync-network'
import { AccountType2 } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { Nullable, Throwing } from '../../../../common/ys'
import { ArrayJSONItem, MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkMethod,
  RequestEncoding,
} from '../../../common/code/network/network-request'

export class WebApiRequest {
  private mailHost: string = ''

  public constructor(accountType: AccountType2) {
    switch (accountType) {
      case AccountType2.Yandex:
        this.mailHost = 'https://mail.yandex.ru'
        break
      case AccountType2.YandexTeam:
        this.mailHost = 'https://mail.yandex-team.ru'
        break
      default:
        throw new Error('Не удалось распознать хост для включения настройки IMAP')
    }
  }

  public enableImap(network: SyncNetwork, token: Nullable<string>): Throwing<void> {
    const result = network.syncExecuteWithRetries(3, this.mailHost, new SetImapRequest(), token)
    if (result.isError()) {
      throw new Error(`Ошибка при включении настройки IMAP: ${result.getError().message}`)
    }
  }
}

class SetImapRequest extends BaseNetworkRequest {
  public constructor() {
    super()
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public params(): MapJSONItem {
    return new MapJSONItem().put(
      'models',
      new ArrayJSONItem().add(
        new MapJSONItem()
          .putString('name', 'do-settings')
          .put('params', new MapJSONItem().putString('params', '{"enable_imap":true,"fid":[]}'))
          .put('meta', new MapJSONItem().putInt32('requestAttempt', 1)),
      ),
    )
  }

  public targetPath(): string {
    return 'web-api/models/liza1'
  }

  public urlExtra(): MapJSONItem {
    const parent = super.urlExtra()
    parent.putString('_m', 'do-settings')
    return parent
  }
}
