import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../common/code/network/network-request'
import { DiehardRequest } from '../../../../../code/network/diehard-backend/entities/diehard-request'

describe(DiehardRequest, () => {
  it('should build DiehardRequest request', () => {
    const request = new DiehardRequest()
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {},
      }),
    )
    expect(() => request.targetPath()).toThrowError('Should be overridden by inheritors')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
