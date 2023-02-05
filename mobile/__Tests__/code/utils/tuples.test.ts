import { YSTriplet } from '../../../../common/code/utils/tuples'

describe(YSTriplet, () => {
  it('should be creatable', () => {
    const triplet = new YSTriplet<string, string, string>('one', 'two', 'three')
    expect(triplet).not.toBeNull()
    expect(triplet.first).toBe('one')
    expect(triplet.second).toBe('two')
    expect(triplet.third).toBe('three')
  })
})
