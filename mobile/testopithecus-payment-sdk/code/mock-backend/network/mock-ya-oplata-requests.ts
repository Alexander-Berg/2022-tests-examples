import { int32ToString } from '../../../../../common/ys'
import { decodeJSONItem, JSONItem } from '../../../../common/code/json/json-types'
import { Result } from '../../../../common/code/result/result'

export function decodeAmountFromYaOplataCreateOrderRequest(item: JSONItem): Result<string> {
  return decodeJSONItem(item, (json) => {
    return int32ToString(
      json
        .tryCastAsMapJSONItem()
        .tryGet('items')
        .tryCastAsArrayJSONItem()
        .get(0)
        .tryCastAsMapJSONItem()
        .tryGetInt32('amount'),
    )
  })
}
