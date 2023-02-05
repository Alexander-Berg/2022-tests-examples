import { ExpressionBuilder } from '../../../code/expression/expression-builder'
import { Variable, Version } from '../../../code/expression/variable'

describe('Expression unit tests', () => {
  describe('Flags evaluation expression', () => {
    describe('== operation', () => {
      it('should return true if numbers are equal', () => {
        const expression = ExpressionBuilder.build('13 == 13')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
      it('should return false if numbers are not equal', () => {
        const expression = ExpressionBuilder.build('234 == 235')
        expect(expression.execute(null)).toStrictEqual(Variable.false())
      })
      it('should return true if strings are equal', () => {
        const expression = ExpressionBuilder.build('\'yes\' == "yes"')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
      it('should return true if variables are equal', () => {
        const expression = ExpressionBuilder.build('a == b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if variables are not equal', () => {
        const expression = ExpressionBuilder.build('abacaba == bakabaka')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['abacaba', Variable.int(239)],
              ['bakabaka', Variable.int(231)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return false if variables are not equal', () => {
        const expression = ExpressionBuilder.build('abacaba == Abra_cadabra')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['abacaba', Variable.int(239)],
              ['Abra_cadabra', Variable.int(231)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true for equals boolean', () => {
        const expression = ExpressionBuilder.build('(2 < 3) == (3 > 2)')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
      it('should return true for equals version', () => {
        const expression = ExpressionBuilder.build('version == v("3.14")')
        expect(
          expression.execute(
            new Map<string, Variable>([['version', Variable.version(new Version('3.14'))]]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return true for equals version with leading zero', () => {
        const expression = ExpressionBuilder.build('version == v("3.04")')
        expect(
          expression.execute(
            new Map<string, Variable>([['version', Variable.version(new Version('3.4'))]]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return true for equals zero-ended version', () => {
        const expression = ExpressionBuilder.build('version == v("3.4.0")')
        expect(
          expression.execute(
            new Map<string, Variable>([['version', Variable.version(new Version('3.4'))]]),
          ),
        ).toStrictEqual(Variable.true())
      })
    })
    describe('!= operation', () => {
      it('should return true if numbers are not equal', () => {
        const expression = ExpressionBuilder.build('2 != 1')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
      it('should return false if numbers are equal', () => {
        const expression = ExpressionBuilder.build('3.14159 != 3.14159')
        expect(expression.execute(null)).toStrictEqual(Variable.false())
      })

      it('should return true if strings are not equal', () => {
        const expression = ExpressionBuilder.build('"yes" != "no"')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
      it('should return true if versions are not equal', () => {
        const expression = ExpressionBuilder.build('v("3.14.1") != v("3.14")')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
    })
    describe('&& operation', () => {
      it('should return true if variables are not equal', () => {
        const expression = ExpressionBuilder.build('2 == 2 && a != b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(231)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
    })
    describe('|| operation', () => {
      it('should return true if variables are not equal', () => {
        const expression = ExpressionBuilder.build('2 == 22 || a != b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(231)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
    })
    describe('! operation', () => {
      it('should check operation NOT', () => {
        const expression = ExpressionBuilder.build('!(2 == 2) || a != b && b == c')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(231)],
              ['c', Variable.int(0)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should check double operation NOT', () => {
        const expression = ExpressionBuilder.build('5 != 5 || !(!(2 == 2))')
        expect(expression.execute(null)).toStrictEqual(Variable.true())
      })
    })
  })
  describe('Priority of operations', () => {
    describe('between || and &&', () => {
      it('should check priority between OR and AND', () => {
        const expression = ExpressionBuilder.build('2 == 2 || a != b && b == c')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(231)],
              ['c', Variable.int(0)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
    })
    describe('between logical and arithmetic operations', () => {
      it('should return false if b is greater than 10', () => {
        const expression = ExpressionBuilder.build('a && b <= 10')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.true()],
              ['b', Variable.int(11)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
    })
    describe('priority of brackets', () => {
      it('should return false if b is not equal c', () => {
        const expression = ExpressionBuilder.build('(2 == 2 || a != b) && b == c')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(231)],
              ['c', Variable.int(0)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
    })
  })

  describe('Arithmetic operations', () => {
    describe('< operation', () => {
      it('should return true if a is less b', () => {
        const expression = ExpressionBuilder.build('a < b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a < b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(237)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return false if a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a < b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if version a is less b', () => {
        const expression = ExpressionBuilder.build('a < b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('3.3'))],
              ['b', Variable.version(new Version('3.14'))],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if version a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a < b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('3.14'))],
              ['b', Variable.version(new Version('3.3'))],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
    })
    describe('<= operation', () => {
      it('should return true if a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a <= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if a is greater b', () => {
        const expression = ExpressionBuilder.build('a <= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(237)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a <= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return true if version a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a <= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('2.3.8'))],
              ['b', Variable.version(new Version('2.3.9'))],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if version a is greater b', () => {
        const expression = ExpressionBuilder.build('a <= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('23.8'))],
              ['b', Variable.version(new Version('23.7'))],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if version a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a <= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('239'))],
              ['b', Variable.version(new Version('239'))],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
    })
    describe('> operation', () => {
      it('should return false if a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a > b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if a is greater b', () => {
        const expression = ExpressionBuilder.build('a > b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(237)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a > b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if version a is greater b', () => {
        const expression = ExpressionBuilder.build('a > b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('0.0.12'))],
              ['b', Variable.version(new Version('0.0.9'))],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if version a is less or equal b', () => {
        const expression = ExpressionBuilder.build('a > b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('1.9.128'))],
              ['b', Variable.version(new Version('1.10.0'))],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
    })
    describe('>= operation', () => {
      it('should return false if a is less b', () => {
        const expression = ExpressionBuilder.build('a >= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a >= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(238)],
              ['b', Variable.int(237)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return true if a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a >= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return false if version a is less b', () => {
        const expression = ExpressionBuilder.build('a >= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('2.38'))],
              ['b', Variable.version(new Version('2.39'))],
            ]),
          ),
        ).toStrictEqual(Variable.false())
      })
      it('should return true if version a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a >= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('0.0.238'))],
              ['b', Variable.version(new Version('0.0.237'))],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
      it('should return true if version a is greater or equal b', () => {
        const expression = ExpressionBuilder.build('a >= b')
        expect(
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.version(new Version('239.0'))],
              ['b', Variable.version(new Version('239.0'))],
            ]),
          ),
        ).toStrictEqual(Variable.true())
      })
    })
  })

  describe('Check incompatible types', () => {
    describe('&& operation', () => {
      it('should return error if variables are not boolean', () => {
        const expression = ExpressionBuilder.build('a && b')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toThrowError('')
      })
      it('should return error with non-boolean operand', () => {
        const expression = ExpressionBuilder.build('a && 3')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with non-boolean operand', () => {
        const expression = ExpressionBuilder.build('"two" && a')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with non-boolean operand', () => {
        const expression = ExpressionBuilder.build('2 < 3 && 3')
        expect(() => expression.execute(null)).toThrowError('')
      })
    })
    describe('|| operation', () => {
      it('should return error if variables are not boolean', () => {
        const expression = ExpressionBuilder.build('a || b')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.int(239)],
            ]),
          ),
        ).toThrowError('')
      })
      it('should return error with non-boolean operand', () => {
        const expression = ExpressionBuilder.build('a || 3')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with non-boolean operand', () => {
        const expression = ExpressionBuilder.build('"two" || a')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with non-boolean operand', () => {
        const expression = ExpressionBuilder.build('2 < 3 || 3')
        expect(() => expression.execute(null)).toThrowError('')
      })
    })
    describe('! operation', () => {
      it('should return error if variable is not boolean', () => {
        const expression = ExpressionBuilder.build('!a')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([['a', Variable.int(239)]]),
          ),
        ).toThrowError('')
      })
    })
    describe('< operation', () => {
      it('should return error if operand is not number', () => {
        const expression = ExpressionBuilder.build('3 < "two"')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error if operand is not number', () => {
        const expression = ExpressionBuilder.build('"two" < 3')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with error operand', () => {
        const expression = ExpressionBuilder.build('a < 45')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with error operand', () => {
        const expression = ExpressionBuilder.build('3.14 < no')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error if operands are not number or version', () => {
        const expression = ExpressionBuilder.build('"two" < "three"')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error if operands are not the same type', () => {
        const expression = ExpressionBuilder.build('3 < 3.14')
        expect(() => expression.execute(null)).toThrowError('')
      })
    })
    describe('<= operation', () => {
      it('should return error if operand is not number', () => {
        const expression = ExpressionBuilder.build('3 <= "two"')
        expect(() => expression.execute(null)).toThrowError('')
      })
    })
    describe('> operation', () => {
      it('should return error if operand is not number', () => {
        const expression = ExpressionBuilder.build('3 > "two"')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error if operand is not number', () => {
        const expression = ExpressionBuilder.build('"two" > 3')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with error operand', () => {
        const expression = ExpressionBuilder.build('6354 > b')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with error operand', () => {
        const expression = ExpressionBuilder.build('b > "no"')
        expect(() => expression.execute(null)).toThrowError('')
      })
    })
    describe('>= operation', () => {
      it('should return error if variable is not number', () => {
        const expression = ExpressionBuilder.build('3 >= "two"')
        expect(() => expression.execute(null)).toThrowError('')
      })
    })
    describe('== operation', () => {
      it('should return error with error operand', () => {
        const expression = ExpressionBuilder.build('a == 45')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error with error operand', () => {
        const expression = ExpressionBuilder.build('45 == a')
        expect(() => expression.execute(null)).toThrowError('')
      })
      it('should return error if operands have different types', () => {
        const expression = ExpressionBuilder.build('a == "b"')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([['a', Variable.int(239)]]),
          ),
        ).toThrowError('')
      })
      it('should return error if operands have different types', () => {
        const expression = ExpressionBuilder.build('a == b')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([
              ['a', Variable.int(239)],
              ['b', Variable.double(239)],
            ]),
          ),
        ).toThrowError('')
      })
      it('should return error if operands have different types', () => {
        const expression = ExpressionBuilder.build('a == 239.0')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([['a', Variable.int(239)]]),
          ),
        ).toThrowError('')
      })
      it('should return error if operands have different types', () => {
        const expression = ExpressionBuilder.build('a == 239')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([['a', Variable.double(239)]]),
          ),
        ).toThrowError('')
      })
    })
    describe('!= operation', () => {
      it('should return undefined if operands have different types', () => {
        const expression = ExpressionBuilder.build('a != "b"')
        expect(() =>
          expression.execute(
            new Map<string, Variable>([['a', Variable.int(239)]]),
          ),
        ).toThrowError('')
      })
    })
  })

  it('should return error if variable from binary operation is not defined', () => {
    const expression = ExpressionBuilder.build('a == "b"')
    expect(() => expression.execute(null)).toThrowError('')
  })
  it('should return error if variable from ! operation is not defined', () => {
    const expression = ExpressionBuilder.build('!b')
    expect(() => expression.execute(null)).toThrowError('')
  })

  it('should check expression without second quote', () => {
    const expression = ExpressionBuilder.build('3 >= "two')
    expect(() => expression.execute(null)).toThrowError('')
  })
  it('should return error for empty expression', () => {
    const expression = ExpressionBuilder.build('')
    expect(() => expression.execute(null)).toThrowError('')
  })
  it('should return error for space-only expression', () => {
    const expression = ExpressionBuilder.build('    ')
    expect(() => expression.execute(null)).toThrowError('')
  })
  it('should check expression without spaces', () => {
    const expression = ExpressionBuilder.build('a&&b<=10||ab')
    expect(
      expression.execute(
        new Map<string, Variable>([
          ['a', Variable.true()],
          ['b', Variable.int(11)],
          ['ab', Variable.true()],
        ]),
      ),
    ).toStrictEqual(Variable.true())
  })
  it('should check expression with many spaces', () => {
    const expression = ExpressionBuilder.build('a   &&b<=   10||  ab')
    expect(
      expression.execute(
        new Map<string, Variable>([
          ['a', Variable.true()],
          ['b', Variable.int(11)],
          ['ab', Variable.false()],
        ]),
      ),
    ).toStrictEqual(Variable.false())
  })
  it('should return error for expression with strange symbols', () => {
    const expression = ExpressionBuilder.build('a~b<=10||a*b')
    expect(() =>
      expression.execute(
        new Map<string, Variable>([
          ['a', Variable.true()],
          ['b', Variable.int(11)],
        ]),
      ),
    ).toThrowError('')
  })
  it('should return error for version with strange symbols', () => {
    const expression = ExpressionBuilder.build('a < b')
    expect(() =>
      expression.execute(
        new Map<string, Variable>([
          ['a', Variable.version(new Version('4.0.8'))],
          ['b', Variable.version(new Version('4.O.8'))],
        ]),
      ),
    ).toThrowError('')
  })
  it('should return error for version with strange symbols', () => {
    const expression = ExpressionBuilder.build('a < b')
    expect(() =>
      expression.execute(
        new Map<string, Variable>([
          ['a', Variable.version(new Version('4.0.B'))],
          ['b', Variable.version(new Version('4.0.8'))],
        ]),
      ),
    ).toThrowError('')
  })
  it('check variable v', () => {
    const expression = ExpressionBuilder.build('v("4.4") == v')
    expect(
      expression.execute(
        new Map<string, Variable>([['v', Variable.version(new Version('4.04'))]]),
      ),
    ).toStrictEqual(Variable.true())
  })
})
