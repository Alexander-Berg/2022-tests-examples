/* eslint-disable @typescript-eslint/unbound-method */
import { JSONSerializerWrapper } from '../../../code/json/json-serializer-wrapper'
import { MapJSONItem } from '../../../code/json/json-types'
import { Result } from '../../../code/result/result'
import { MockJSONSerializer } from '../../__helpers__/mock-patches'

describe(JSONSerializerWrapper, () => {
  it('should serialize JSON item', () => {
    const expectedResult = new Result('serialized value', null)
    const jsonSerializer = MockJSONSerializer({
      serialize: jest.fn().mockReturnValue(expectedResult),
    })
    const serializerWrapper = new JSONSerializerWrapper(jsonSerializer)

    const jsonItem = new MapJSONItem()
    const result = serializerWrapper.serialize(jsonItem)

    expect(result).toBe(expectedResult)
    expect(jsonSerializer.serialize).toBeCalledWith(jsonItem)
  })

  it('should deserialize JSON item', () => {
    const jsonItem = new MapJSONItem()
    const jsonSerializer = MockJSONSerializer({
      deserialize: jest.fn().mockReturnValue(new Result(jsonItem, null)),
    })
    const serializerWrapper = new JSONSerializerWrapper(jsonSerializer)

    const result = serializerWrapper.deserializeJSONItem('some json value')

    expect(result.getValue()).toBe(jsonItem)
    expect((jsonSerializer.deserialize as jest.Mock).mock.calls[0][0]).toBe('some json value')
  })
})
