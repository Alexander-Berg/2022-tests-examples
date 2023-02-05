import { nullthrows } from '../../../../common/ys'
import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { Uris } from '../../../common/native-modules/native-modules'

export interface MockServerRequestMatcher extends HttpRequestHandler {
  canHandleRequest(request: HttpRequest): boolean
}

export class MockServerRequestMatchers {
  public static path(path: string, handler: HttpRequestHandler): MockServerPathRequestMatcher {
    return new MockServerPathRequestMatcher(path, handler)
  }
  public static pathPrefix(pathPrefix: string, handler: HttpRequestHandler): MockServerPathPrefixRequestMatcher {
    return new MockServerPathPrefixRequestMatcher(pathPrefix, handler)
  }
}

// ----

export abstract class MockServerAbstractRequestMatcher implements MockServerRequestMatcher {
  public constructor(private readonly handler: HttpRequestHandler) {}

  public abstract canHandleRequest(request: HttpRequest): boolean

  public handleRequest(request: HttpRequest): HttpResponse {
    return this.handler.handleRequest(request)
  }
}

export class MockServerPathRequestMatcher extends MockServerAbstractRequestMatcher {
  public constructor(private readonly path: string, handler: HttpRequestHandler) {
    super(handler)
  }

  public canHandleRequest(request: HttpRequest): boolean {
    const requestPath = nullthrows(Uris.fromString(request.url)?.getPath())
    return this.dropLeadingSlash(this.path) === this.dropLeadingSlash(requestPath)
  }

  // just drop leading slashes for consistency
  private dropLeadingSlash(path: string): string {
    return path.startsWith('/') ? path.slice(1) : path
  }
}

export class MockServerPathPrefixRequestMatcher extends MockServerAbstractRequestMatcher {
  public constructor(private readonly pathPrefix: string, handler: HttpRequestHandler) {
    super(handler)
  }

  public canHandleRequest(request: HttpRequest): boolean {
    const pathComponents = nullthrows(Uris.fromString(request.url)?.getPathSegments())
    return pathComponents.length > 0 && pathComponents[0] === this.pathPrefix
  }
}
