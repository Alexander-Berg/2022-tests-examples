import { YSError } from '../../../../../common/ys'
import { resultExecuteSequentially } from '../../../code/utils/result-utils'
import { Result } from '../../../code/result/result'

describe(resultExecuteSequentially, () => {
  it('should return array of values', () => {
    const values = resultExecuteSequentially([
      () => new Result(1, null),
      () => new Result(2, null),
      () => new Result(3, null),
    ])
    expect(values.getValue()).toStrictEqual([1, 2, 3])
  })
  it('should return error', () => {
    const values = resultExecuteSequentially([
      () => new Result(1, null),
      () => new Result(null, new YSError('error')),
      () => new Result(3, null),
    ])
    expect(values.getError().message).toStrictEqual('error')
  })
})
