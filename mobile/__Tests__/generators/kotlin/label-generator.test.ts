import { FilenameWithCounterBasedReturnLabelGenerator } from '../../../src/generators/kotlin/filename-label-generator'

describe(FilenameWithCounterBasedReturnLabelGenerator, () => {
  it('should generate different labels from different instances', () => {
    const gen1 = new FilenameWithCounterBasedReturnLabelGenerator('somefile1')
    const gen2 = new FilenameWithCounterBasedReturnLabelGenerator('somefile2')
    const label1 = gen1.generate()
    const label2 = gen2.generate()
    expect(label1).not.toStrictEqual(label2)
  })
  it('should generate different labels from consecutive invocations', () => {
    const gen = new FilenameWithCounterBasedReturnLabelGenerator('somefile1')
    const label1 = gen.generate()
    const label2 = gen.generate()
    expect(label1).not.toStrictEqual(label2)
  })
  it('should generate increasing sequence labels from consecutive invocations', () => {
    const gen = new FilenameWithCounterBasedReturnLabelGenerator('somefile1')
    const label1 = gen.generate()
    const label2 = gen.generate()
    expect(label1).toBe('__LBL__somefile1_1')
    expect(label2).toBe('__LBL__somefile1_2')
  })
  it('should generate labels using prefix', () => {
    const gen = new FilenameWithCounterBasedReturnLabelGenerator('somefile1')
    const label = gen.generate()
    expect(label).toBe('__LBL__somefile1_1')
  })
  it('should generate labels using default prefix if not provided', () => {
    const gen = new FilenameWithCounterBasedReturnLabelGenerator('new_file')
    const label = gen.generate()
    expect(label).toBe('__LBL__new_file_1')
  })
  it('should generate labels using custom prefix if not provided and custom start index', () => {
    const gen = new FilenameWithCounterBasedReturnLabelGenerator('new_file', '__DEF_PRFX__', 42)
    const label = gen.generate()
    expect(label).toBe('__DEF_PRFX__new_file_42')
  })
})
