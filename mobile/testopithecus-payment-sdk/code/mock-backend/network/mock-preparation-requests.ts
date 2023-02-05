import { decodeJSONItem, JSONItem } from '../../../../common/code/json/json-types'
import { Result } from '../../../../common/code/result/result'

export class TrustPaymentsPaymentsRequest {
  public constructor(
    public readonly price: string,
    public readonly serviceOrderId: string,
    public readonly forced3ds: boolean,
  ) {}

  public static decodeJson(item: JSONItem): Result<TrustPaymentsPaymentsRequest> {
    return decodeJSONItem(item, (json) => {
      const map = json.tryCastAsMapJSONItem()
      let force3ds = false
      const passParams = map.get('pass_params')
      if (passParams !== null) {
        const terminalRouteData = passParams.tryCastAsMapJSONItem().get('terminal_route_data')
        if (terminalRouteData !== null) {
          force3ds = terminalRouteData.tryCastAsMapJSONItem().getInt32('service_force_3ds') === 1
        }
      }
      const ordersArray = map.tryGetArray('orders')
      const firstRequested = ordersArray[0].tryCastAsMapJSONItem()
      return new TrustPaymentsPaymentsRequest(
        firstRequested.tryGetString('price'),
        firstRequested.tryGetString('service_order_id'),
        force3ds,
      )
    })
  }
}
