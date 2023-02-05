import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { DiehardRequest } from '../../../../code/network/diehard/diehard-request'

describe(DiehardRequest, () => {
  it('should represent basic diehard request', () => {
    const request = new DiehardRequest()
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(() => request.targetPath()).toThrowError('Should be overridden by inheritors')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {},
      }),
    )
  })
})
