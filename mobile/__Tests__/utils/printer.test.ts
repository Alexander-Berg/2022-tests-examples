import Printer from '../../src/utils/printer'

describe('Printer', () => {
  it('can print lines with indentation', () => {
    const p = new Printer()
      .addLine('indented 0')
      .indent()
      .addLine('indented 1')
      .indent()
      .addLine('indented 2')
      .unindent()
      .addLine('indented 1')
      .unindent()
      .addLine('indented 0')

    const result = p.print()
    const expected = 'indented 0\n\tindented 1\n\t\tindented 2\n\tindented 1\nindented 0\n'
    expect(result).toStrictEqual(expected)
  })

  it('can print statements', () => {
    const p = new Printer()
      .beginScope('if (true)')
      .addLine('st 1')
      .addLine('st 2')
      .beginScope('for (a; b; c)')
      .addLine('st 3')
      .endScope()
      .addLine('st 4')
      .endScope()
      .addLine('st 5')

    const result = p.print()
    const expected = 'if (true) {\n\tst 1\n\tst 2\n\tfor (a; b; c) {\n\t\tst 3\n\t}\n\tst 4\n}\nst 5\n'
    expect(result).toStrictEqual(expected)
  })

  it('can concat lines', () => {
    const p = new Printer()
      .beginScope('if (true)')
      .add('const arr = [', true)
      .add('1, ', false)
      .add('2, ', false)
      .add('3', false)
      .addLine(']', false)
      .endScope()

    const result = p.print()
    const expected = 'if (true) {\n\tconst arr = [1, 2, 3]\n}\n'
    expect(result).toStrictEqual(expected)
  })

  it('should not allow negative indentation', () => {
    const p = new Printer()
      // ind: 0
      .indent() // ind: +1
      .indent() // ind: +2
      .addLine('A')
      .unindent() // ind: +1
      .addLine('B')
      .unindent() // ind: 0
      .addLine('C')
      .unindent() // ind: 0
      .addLine('D')

    const result = p.print()
    const expected = '\t\tA\n\tB\nC\nD\n'
    expect(result).toStrictEqual(expected)
  })

  it('allows printing empty lines', () => {
    const p = new Printer().addLine('A').addLine().addLine('B').addLine().addLine('C')

    const result = p.print()
    const expected = 'A\n\nB\n\nC\n'
    expect(result).toStrictEqual(expected)
  })

  it('allows printing unindented lines', () => {
    const p = new Printer().addLine('A').indent().addLine('B', true).addLine('C', false)

    const result = p.print()
    const expected = 'A\n\tB\nC\n'
    expect(result).toStrictEqual(expected)
  })

  it('allows printing unindented strings', () => {
    const p = new Printer().add('A').indent().add('B', true).add('C', false)

    const result = p.print()
    const expected = 'A\tBC'
    expect(result).toStrictEqual(expected)
  })
})
