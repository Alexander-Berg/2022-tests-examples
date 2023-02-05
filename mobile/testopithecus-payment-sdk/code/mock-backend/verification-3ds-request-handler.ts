import {
  HttpRequest,
  HttpRequestHandler,
  HttpResponse,
  HttpResponseBuilder,
} from '../../../common/code/network/http-layer'
import { Uris } from '../../../common/native-modules/native-modules'
import { MockTrustModel } from './model/mock-trust-model'

export class Verification3dsRequestHandler implements HttpRequestHandler {
  public constructor(private readonly trustModel: MockTrustModel) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    if (request.url.startsWith('/web/redirect_3ds')) {
      const url = Uris.fromString(request.url)
      if (url === null) {
        return new HttpResponseBuilder().setCode(500).build()
      }
      const purchaseToken = url!.getQueryParameter('purchase_token')
      if (purchaseToken === null || !this.trustModel.has3dsChallenge(purchaseToken!)) {
        return new HttpResponseBuilder().setCode(500).build()
      }

      // TODO: insert appropriate amount if needed
      return new HttpResponseBuilder()
        .setCode(200)
        .setBodyText(
          '<!DOCTYPE html>\n' +
            '        <html lang="en">\n' +
            '        <head>\n' +
            '            <title>Trust test 3DS page</title>\n' +
            '        </head>\n' +
            '        <body>\n' +
            '            <h2>Test 3DS page</h2>\n' +
            '            <p>Payment amount: 10000.00 </p>\n' +
            '            <p>Input a 3 digit code.</p>\n' +
            '            <p>Use the following code values to trigger specific\n' +
            '            authorization results:</p>\n' +
            '            <ul>\n' +
            '                <li><b>200</b> - success</li>\n' +
            '                <li><b>400</b> - wrong 3ds - issuing bank was not able to perform 3dsecure card authorization</li>\n' +
            '                <li><b>401</b> - wrong 3ds - issuing bank could not determine if the card is 3dsecure</li>\n' +
            '                <li><b>300</b> - not enough money </li>\n' +
            '                <li><b>301</b> - transaction is rejected since the amount exceeds limits </li>\n' +
            '                <li><b>302</b> - 3DS-communication error </li>\n' +
            '                <li><b>303</b> - unable to process </li>\n' +
            '                <li><b>304</b> - the client has performed the maximum number of transactions</li>\n' +
            '                <li><b>305</b> - invalid transaction - card limitations</li>\n' +
            '                <li><b>306</b> - invalid transaction - The message format is incorrect</li>\n' +
            '                <li>All other - RBS internal error </li>\n' +
            '            </ul>\n' +
            '            <form action="/web/fake_3ds" method="GET">\n' +
            '                <label for="3ds_code">Input 3DS code here: </label>\n' +
            '                <input name="3ds_code" size="3" id="3dsCode" />\n' +
            `                <input name="purchase_token" type="hidden" id="purchase_token" value="${purchaseToken!}" />\n` +
            '                <input type="submit" value="Submit" />\n' +
            '            </form>\n' +
            '        </body>\n' +
            '        </html>',
        )
        .build()
    } else if (request.url.startsWith('/web/fake_3ds')) {
      const url = Uris.fromString(request.url)
      if (url === null) {
        return new HttpResponseBuilder().setCode(500).build()
      }
      const purchaseToken = url!.getQueryParameter('purchase_token')
      const code = url!.getQueryParameter('3ds_code')
      if (code === null || purchaseToken === null) {
        return new HttpResponseBuilder().setCode(500).build()
      }
      const result = this.trustModel.provide3ds(purchaseToken, code)
      return new HttpResponseBuilder().setCode(result ? 200 : 500).build()
    }
    return new HttpResponseBuilder().setCode(500).build()
  }
}
