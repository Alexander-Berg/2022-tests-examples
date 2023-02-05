import { Int32 } from '../../../../common/ys'
import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { SyncSleep } from '../../../testopithecus-common/code/utils/sync-sleep'
import { MockServerRequestDispatcher } from '../mock-server/mock-server-request-dispatcher'
import { MockServerDelayedRequestHandler } from '../mock-server/mock-server-request-handlers'
import { BindingCardExtractor, CardDataDecryptor } from './binding-card-extractor'
import { DiehardRequestHandler } from './diehard-request-handler'
import { MobpaymentRequestHandler } from './mobpayment-request-handler'
import { MockTrustModel } from './model/mock-trust-model'
import { NspkRequestHandler } from './nspk-request-handler'
import { SbpRequestHandler } from './sbp-request-handler'
import { TrustRequestHandler } from './trust-request-handler'
import { TusRequestHandler } from './tus-request-handler'
import { Verification3dsRequestHandler } from './verification-3ds-request-handler'
import { YaOplataHandler } from './ya-oplata-handler'

export class NotFoundHandler implements HttpRequestHandler {
  public handleRequest(request: HttpRequest): HttpResponse {
    return new HttpResponse(500, new Map<string, string>(), new ArrayBuffer(0))
  }
}

export function createMockBackendDispatcher(
  cardDataDecryptor: CardDataDecryptor,
  sbpSupport: boolean,
  jsonSerializer: JSONSerializer,
  delayMs: Int32,
  sleepImpl: SyncSleep,
): HttpRequestHandler {
  const dispatcher = new MockServerRequestDispatcher(new NotFoundHandler())
  const model = new MockTrustModel(new BindingCardExtractor(cardDataDecryptor, jsonSerializer), sbpSupport)
  return new MockServerDelayedRequestHandler(
    dispatcher
      .matchPathPrefix('trust', new TrustRequestHandler(model, jsonSerializer))
      .matchPathPrefix('tus', new TusRequestHandler(jsonSerializer))
      .matchPathPrefix('mobpayment', new MobpaymentRequestHandler(model, jsonSerializer))
      .matchPathPrefix('diehard', new DiehardRequestHandler(model, jsonSerializer))
      .matchPathPrefix('web', new Verification3dsRequestHandler(model))
      .matchPathPrefix('yaoplata', new YaOplataHandler(model, jsonSerializer))
      .matchPathPrefix('sbp_pay', new SbpRequestHandler(model))
      .matchPathPrefix('nspk', new NspkRequestHandler()),
    delayMs,
    sleepImpl,
  )
}
