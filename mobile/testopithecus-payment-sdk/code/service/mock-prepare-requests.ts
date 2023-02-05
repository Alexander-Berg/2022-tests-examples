import { MapJSONItem } from '../../../common/code/json/json-types'
import {
  BaseNetworkRequest,
  NetworkMethod,
  RequestEncoding,
  UrlRequestEncoding,
} from '../../../common/code/network/network-request'
import { BankName } from '../../../payment-sdk/code/busilogics/bank-name'
import { FamilyInfoMode } from '../mock-backend/model/mock-data-types'

export class MockBankRequest extends BaseNetworkRequest {
  public constructor(private readonly bankName: BankName) {
    super()
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public targetPath(): string {
    return 'mock-trust-bank'
  }

  public params(): MapJSONItem {
    return new MapJSONItem().putString('bank', this.bankName.toString())
  }
}

export class MockFamilyInfoModeRequest extends BaseNetworkRequest {
  public constructor(private readonly familyInfoMode: FamilyInfoMode) {
    super()
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public targetPath(): string {
    return 'mock-family-info-mode'
  }

  public params(): MapJSONItem {
    return new MapJSONItem().putString('mode', this.familyInfoMode.toString())
  }
}
