import { RuntimeClassInfo } from '../../../code/utils/runtime-info'

describe(RuntimeClassInfo, () => {
  it('should be constructible', () => {
    const info = new RuntimeClassInfo('className')
    expect(info.name).toBe('className')
  })
})
