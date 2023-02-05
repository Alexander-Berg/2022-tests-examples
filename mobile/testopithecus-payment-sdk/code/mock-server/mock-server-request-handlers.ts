import { Int32, nullthrows } from '../../../../common/ys'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { Uris } from '../../../common/native-modules/native-modules'
import { SyncSleep } from '../../../testopithecus-common/code/utils/sync-sleep'

export class MockServerRequestHandlers {
  public static callback(callback: (request: HttpRequest) => HttpResponse): MockServerRequestHandlerWithCallback {
    return new MockServerRequestHandlerWithCallback(callback)
  }
  public static stripPathPrefix(
    pathPrefix: string,
    handler: HttpRequestHandler,
  ): MockServerStripPathPrefixRequestHandler {
    return new MockServerStripPathPrefixRequestHandler(pathPrefix, handler)
  }
}

export class MockServerRequestHandlerWithCallback implements HttpRequestHandler {
  public constructor(private readonly callback: (request: HttpRequest) => HttpResponse) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    return this.callback(request)
  }
}

export class MockServerStripPathPrefixRequestHandler implements HttpRequestHandler {
  public constructor(private readonly pathPrefix: string, private readonly handler: HttpRequestHandler) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    const url = nullthrows(Uris.fromString(request.url))
    let pathSegments = url.getPathSegments()
    if (pathSegments.length > 0 && pathSegments[0] === this.pathPrefix) {
      pathSegments = pathSegments.slice(1)
    }
    const newUrl = url.builder().setPath(pathSegments.join('/')).build()
    const newRequest = new HttpRequest(request.method, newUrl.getAbsoluteString(), request.headers, request.body)
    return this.handler.handleRequest(newRequest)
  }
}

export class MockServerDelayedRequestHandler implements HttpRequestHandler {
  public constructor(
    private readonly handler: HttpRequestHandler,
    private readonly delayMs: Int32,
    private readonly sleepImpl: SyncSleep,
  ) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    this.sleepImpl.sleepMs(this.delayMs)
    return this.handler.handleRequest(request)
  }
}
