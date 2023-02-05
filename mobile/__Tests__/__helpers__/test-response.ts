import { Int32, Nullable } from '../../../../common/ys'
import { JSONItem } from '../../../common/code/json/json-types'
import { NetworkResponse, NetworkResponseBody } from '../../../common/code/network/network-response'
import { JSONItemFromJSON } from '../../../common/__tests__/__helpers__/json-helpers'

export class TestResponse implements NetworkResponse {
  public constructor(
    private readonly _code: Int32,
    private readonly _body: Nullable<string>,
    private readonly _headers: Map<string, string> = new Map(),
  ) {}

  public code(): Int32 {
    return this._code
  }

  public headers(): ReadonlyMap<string, string> {
    return this._headers
  }

  public isSuccessful(): boolean {
    return this.code() >= 200 && this.code() <= 299
  }

  public body(): Nullable<NetworkResponseBody> {
    return {
      string: () => this._body,
    } as NetworkResponseBody
  }

  public fullJSON(): object {
    return JSON.parse(this._body!)
  }

  public fullJSONItem(): JSONItem {
    return JSONItemFromJSON(this.fullJSON())
  }

  public static success(data: object): TestResponse {
    return new TestResponse(
      200,
      JSON.stringify({
        code: 200,
        status: 'success',
        data,
      }),
    )
  }

  public static fail(code: Int32, message: string): TestResponse {
    return new TestResponse(
      code,
      JSON.stringify({
        code,
        status: 'fail',
        data: {
          message,
          params: {},
        },
      }),
    )
  }
}
