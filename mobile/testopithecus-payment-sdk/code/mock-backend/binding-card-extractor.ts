import { YSError } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { decodeJSONItem, JSONItem } from '../../../common/code/json/json-types'
import { Result, resultError, resultValue } from '../../../common/code/result/result'
import { extractMockRequest } from './mock-backend-utils'
import { MockCard } from './model/mock-data-types'

export interface CardDataDecryptor {
  decrypt(data: string, hashAlgo: string): Result<string>
}

export class BindingCardExtractor {
  public constructor(
    private readonly cardDataDecryptor: CardDataDecryptor,
    private readonly jsonSerializer: JSONSerializer,
  ) {}

  public createCardFromData(id: string, encryptedData: string, hashAlgo: string): Result<MockCard> {
    const decrypted = this.cardDataDecryptor.decrypt(encryptedData, hashAlgo)
    if (decrypted.isError()) {
      return resultError(new YSError('cannot decrypt'))
    }
    const card = extractMockRequest(decrypted.getValue(), this.jsonSerializer, (item) =>
      BindingCardExtractor.createCardFromJson(id, item),
    )
    return card.flatMap((value) => resultValue(card.getValue()))
  }

  private static createCardFromJson(id: string, item: JSONItem): Result<MockCard> {
    return decodeJSONItem(item, (json) => {
      const map = json.tryCastAsMapJSONItem()
      return new MockCard(
        map.tryGetString('card_number'),
        map.tryGetString('expiration_month'),
        map.tryGetString('expiration_year'),
        map.tryGetString('cvn'),
        id,
      )
    })
  }
}
