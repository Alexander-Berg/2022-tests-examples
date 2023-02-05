import { HttpRequest, HttpRequestHandler, HttpResponse } from '../../../common/code/network/http-layer'
import { MockServerRequestMatcher, MockServerRequestMatchers } from './mock-server-request-matchers'

export class MockServerRequestDispatcher implements HttpRequestHandler {
  private readonly matchers: MockServerRequestMatcher[] = []

  public constructor(private readonly notFoundRequestHandler: HttpRequestHandler) {}

  public handleRequest(request: HttpRequest): HttpResponse {
    for (const handler of this.matchers) {
      if (handler.canHandleRequest(request)) {
        return handler.handleRequest(request)
      }
    }
    return this.notFoundRequestHandler.handleRequest(request)
  }

  public match(matcher: MockServerRequestMatcher): MockServerRequestDispatcher {
    this.matchers.push(matcher)
    return this
  }

  public matchPath(path: string, handler: HttpRequestHandler): MockServerRequestDispatcher {
    this.matchers.push(MockServerRequestMatchers.path(path, handler))
    return this
  }

  public matchPathPrefix(pathPrefix: string, handler: HttpRequestHandler): MockServerRequestDispatcher {
    this.matchers.push(MockServerRequestMatchers.pathPrefix(pathPrefix, handler))
    return this
  }
}
