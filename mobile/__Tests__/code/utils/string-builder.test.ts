import { StringBuilder } from '../../../code/utils/string-builder'

describe(StringBuilder, () => {
  let builder: StringBuilder
  beforeEach(() => {
    builder = new StringBuilder()
  })
  it('should return empty string if no strings were added', () => {
    expect(builder.build()).toHaveLength(0)
  })
  it('should add strings', () => {
    builder.add('1').add('2').add('3')
    expect(builder.build()).toStrictEqual('123')
  })
  it('should add lines', () => {
    builder.addLine('1').addLine('2').addLine('3')
    expect(builder.build()).toStrictEqual('1\n2\n3\n')
  })
  it('should add lines and strings with one run', () => {
    builder.addLine('1').add('2').addLine('3').add('4')
    expect(builder.build()).toStrictEqual('1\n23\n4')
  })
  it('after using build, it should return the same string all the time', () => {
    const first = builder.add('1').add('2').build()
    const second = builder.add('3').add('4').build()
    expect(first).toStrictEqual('12')
    expect(second).toStrictEqual(first)
  })
})
