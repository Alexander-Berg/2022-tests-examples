import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { decodeJSONItem } from '../../../common/code/json/json-types'
import { Network } from '../../../common/code/network/network'
import { XPromise } from '../../../common/code/promise/xpromise'
import { BankName } from '../../../payment-sdk/code/busilogics/bank-name'
import { DiehardBackendErrorProcessor } from '../../../payment-sdk/code/network/diehard-backend/diehard-backend-api'
import { NetworkService } from '../../../payment-sdk/code/network/network-service'
import { FamilyInfoMode } from '../mock-backend/model/mock-data-types'
import { MockBankRequest, MockFamilyInfoModeRequest } from './mock-prepare-requests'

export class MockPrepareService {
  public constructor(private readonly networkService: NetworkService) {}

  public static create(network: Network, serializer: JSONSerializer): MockPrepareService {
    const errorProcessor = new DiehardBackendErrorProcessor()
    const networkService = new NetworkService(network, serializer, errorProcessor)
    return new MockPrepareService(networkService)
  }

  public setMockBank(bank: BankName): XPromise<boolean> {
    return this.networkService.performRequest(new MockBankRequest(bank), (item) =>
      decodeJSONItem(item, (json) => {
        const map = json.tryCastAsMapJSONItem()
        return map.tryGetString('status') === 'success'
      }),
    )
  }

  public setMockFamilyInfoMode(mode: FamilyInfoMode): XPromise<boolean> {
    return this.networkService.performRequest(new MockFamilyInfoModeRequest(mode), (item) =>
      decodeJSONItem(item, (json) => {
        const map = json.tryCastAsMapJSONItem()
        return map.tryGetString('status') === 'success'
      }),
    )
  }
}
