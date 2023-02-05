import {
  makeNullable,
  TSBuiltInType,
  TSLocalType,
  TSLocalTypeKind,
  TSPrimitiveType,
  TSPrimitiveTypeName,
  TSReferenceType,
} from '../../../src/generators-model/basic-types'
import {
  TSArrowFunctionExpression,
  TSAsExpression,
  TSBangExpression,
  TSBinaryExpression,
  TSBinaryOperator,
  TSCallExpression,
  TSConditionalExpression,
  TSElementAccessExpression,
  TSExpression,
  TSExpressionExtraTypeHintKind,
  TSExpressionKind,
  TSExpressionTemplateExpressionSpan,
  TSGroupedExpression,
  TSIdentifier,
  TSLiteralExpression,
  TSLiteralTemplateExpressionSpan,
  TSMemberAccessExpression,
  TSObjectCreationExpression,
  TSSuperExpression,
  TSTemplateExpressionSpanKind,
  TSThisExpression,
  TSUnaryExpression,
} from '../../../src/generators-model/expression'
import { TSFunctionArgumentType } from '../../../src/generators-model/function-argument'
import {
  TSBlockStatement,
  TSExpressionStatement,
  TSReturnStatement,
  TSStatementKind,
} from '../../../src/generators-model/statement'
import { KotlinExpressionGenerator } from '../../../src/generators/kotlin/expression-generator'
import { KotlinTypeMapper } from '../../../src/generators/kotlin/kotlin-type-mapper'
import Printer from '../../../src/utils/printer'
import {
  arr_,
  double_,
  fun_,
  int32_,
  int64_,
  ref_,
  refs_,
  RESERVED_KEYWORD__BREAK__,
  str_,
} from '../../__helpers__/test-helpers'

describe(KotlinExpressionGenerator, () => {
  let generator: KotlinExpressionGenerator
  let printer: Printer
  beforeAll(() => {
    printer = new Printer()
    generator = new KotlinExpressionGenerator(KotlinTypeMapper.getTypeMapping, {
      generate(): string {
        return '__PRFX__11111'
      },
    })
  })

  // LITERALS
  it('should generate literals', () => {
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        } as TSLiteralExpression,
        printer,
      ),
    ).toBe('10')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: 'hello',
        } as TSLiteralExpression,
        printer,
      ),
    ).toBe('"hello"')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: true,
        } as TSLiteralExpression,
        printer,
      ),
    ).toBe('true')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: false,
        } as TSLiteralExpression,
        printer,
      ),
    ).toBe('false')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: null,
        } as TSLiteralExpression,
        printer,
      ),
    ).toBe('null')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: [
            {
              kind: TSExpressionKind.Literal,
              value: 10,
            } as TSExpression,
            {
              kind: TSExpressionKind.Identifier,
              name: 'a',
            } as TSExpression,
          ],
        } as TSLiteralExpression,
        printer,
      ),
    ).toBe('mutableListOf(10, a)')
  })
  it('should generate template strings', () => {
    const value = {
      kind: TSExpressionKind.Template,
      spans: [
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: 'hello \\ \n \t ',
        } as TSLiteralTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Call,
            expression: {
              kind: TSExpressionKind.MemberAccess,
              object: {
                kind: TSExpressionKind.Identifier,
                name: 'world',
              },
              member: {
                kind: TSExpressionKind.Identifier,
                name: 'toString',
              },
              optionalChaining: false,
            } as TSMemberAccessExpression,
            args: [],
            typeArguments: [],
            optionalChaining: false,
          } as TSCallExpression,
        },
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: '; keep ',
        } as TSLiteralTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Literal,
            value: 'calm',
          } as TSLiteralExpression,
        } as TSExpressionTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: ' and carry ',
        } as TSLiteralTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Conditional,
            condition: {
              kind: TSExpressionKind.Binary,
              operator: '===',
              left: {
                kind: TSExpressionKind.Literal,
                value: 1,
              } as TSLiteralExpression,
              right: {
                kind: TSExpressionKind.Literal,
                value: 1,
              } as TSLiteralExpression,
            } as TSBinaryExpression,
            whenTrue: {
              kind: TSExpressionKind.Literal,
              value: 'on',
            } as TSLiteralExpression,
            whenFalse: {
              kind: TSExpressionKind.Literal,
              value: 'off',
            } as TSLiteralExpression,
          } as TSConditionalExpression,
        },
      ],
    }
    expect(generator.generate(value, printer)).toBe(
      '"hello \\ \n \t ${world.toString()}; keep ${"calm"} and carry ${if (1 == 1) "on" else "off"}"',
    )
  })
  it('should throw if unknown type of literal', () => {
    expect(() => {
      generator.generate(
        {
          kind: TSExpressionKind.Literal,
          value: {},
        } as TSLiteralExpression,
        printer,
      )
    }).toThrowError(/Unknown type of literal expression/)
  })

  // IDENTIFIERS
  it('should generate identifiers', () => {
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
        printer,
      ),
    ).toBe('a')

    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Identifier,
          name: RESERVED_KEYWORD__BREAK__,
        } as TSIdentifier,
        printer,
      ),
    ).toBe('`' + RESERVED_KEYWORD__BREAK__ + '`')
  })

  // UNARY
  it('should generate unary operations', () => {
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Unary,
          operator: '+',
          operand: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
        } as TSUnaryExpression,
        printer,
      ),
    ).toBe('+10')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Unary,
          operator: '-',
          operand: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          },
        } as TSUnaryExpression,
        printer,
      ),
    ).toBe('-a')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Unary,
          operator: '!',
          operand: {
            kind: TSExpressionKind.Literal,
            value: true,
          },
        } as TSUnaryExpression,
        printer,
      ),
    ).toBe('!true')
    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Unary,
          operator: '~',
          operand: {
            kind: TSExpressionKind.Identifier,
            name: 'b',
          },
        } as TSUnaryExpression,
        printer,
      ),
    ).toBe('~b')
  })

  // BINARY
  it('should generate binary operations', () => {
    function makeExpression(operator: TSBinaryOperator): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      } as TSBinaryExpression
    }
    expect(generator.generate(makeExpression('+'), printer)).toBe('a + 10')
    expect(generator.generate(makeExpression('-'), printer)).toBe('a - 10')
    expect(generator.generate(makeExpression('*'), printer)).toBe('a * 10')
    expect(generator.generate(makeExpression('/'), printer)).toBe('a / 10')
    expect(generator.generate(makeExpression('%'), printer)).toBe('a % 10')
    expect(generator.generate(makeExpression('<<'), printer)).toBe('a shl 10')
    expect(generator.generate(makeExpression('>>'), printer)).toBe('a shr 10')
    expect(generator.generate(makeExpression('||'), printer)).toBe('a || 10')
    expect(generator.generate(makeExpression('&&'), printer)).toBe('a && 10')
    expect(generator.generate(makeExpression('|'), printer)).toBe('a or 10')
    expect(generator.generate(makeExpression('&'), printer)).toBe('a and 10')
    expect(generator.generate(makeExpression('^'), printer)).toBe('a xor 10')
    expect(generator.generate(makeExpression('==='), printer)).toBe('a == 10')
    expect(generator.generate(makeExpression('!=='), printer)).toBe('a != 10')
    expect(generator.generate(makeExpression('<'), printer)).toBe('a < 10')
    expect(generator.generate(makeExpression('<='), printer)).toBe('a <= 10')
    expect(generator.generate(makeExpression('>'), printer)).toBe('a > 10')
    expect(generator.generate(makeExpression('>='), printer)).toBe('a >= 10')
    expect(generator.generate(makeExpression('='), printer)).toBe('a = 10')
    expect(generator.generate(makeExpression('+='), printer)).toBe('a += 10')
    expect(generator.generate(makeExpression('-='), printer)).toBe('a -= 10')
    expect(generator.generate(makeExpression('*='), printer)).toBe('a *= 10')
    expect(generator.generate(makeExpression('/='), printer)).toBe('a /= 10')
    expect(generator.generate(makeExpression('%='), printer)).toBe('a %= 10')
    expect(generator.generate(makeExpression('??'), printer)).toBe('a ?: 10')
    expect(generator.generate(makeExpression('instanceof'), printer)).toBe('a is 10')
    expect(() => generator.generate(makeExpression('>>>' as TSBinaryOperator), printer)).toThrowError(
      'Unsupported operation type: >>>',
    )
  })

  // AS
  it('should generate "as" expression', () => {
    function makeExpression(type: TSLocalType): TSAsExpression {
      return {
        kind: TSExpressionKind.As,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        type,
      } as TSAsExpression
    }
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Any,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as Any')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Boolean,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as Boolean')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Int32,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as Int')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Int64,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as Long')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Double,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as Double')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.String,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as String')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Void,
        } as TSPrimitiveType),
        printer,
      ),
    ).toBe('a as Unit')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'A',
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as A')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'A',
          typeParameters: [int32_(), str_()],
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as A<Int, String>')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Array',
          typeParameters: [int64_()],
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSArray<Long>')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Array',
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSArray')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Map',
          typeParameters: [double_(), str_()],
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSMap<Double, String>')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Map',
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSMap')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Set',
          typeParameters: [int32_()],
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSSet<Int>')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Set',
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSSet')
    expect(
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Date',
        } as TSReferenceType),
        printer,
      ),
    ).toBe('a as YSDate')
    expect(() => {
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Number',
        } as TSReferenceType),
        printer,
      )
    }).toThrowError("Type 'Number' cannot be used as a Reference Type. It must be used as a Primitive.")
    expect(() => {
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Boolean',
        } as TSReferenceType),
        printer,
      )
    }).toThrowError("Type 'Boolean' cannot be used as a Reference Type. It must be used as a Primitive.")
    expect(() => {
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'String',
        } as TSReferenceType),
        printer,
      )
    }).toThrowError("Type 'String' cannot be used as a Reference Type. It must be used as a Primitive.")
    expect(() => {
      generator.generate(
        makeExpression({
          kind: TSLocalTypeKind.Reference,
          name: 'Math',
        } as TSReferenceType),
        printer,
      )
    }).toThrowError('Math type cannot be used as a Reference Type.')
  })

  // GROUPED
  it('should generate grouped operations', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const left: TSGroupedExpression = {
      kind: TSExpressionKind.Grouped,
      expression: makeExpression('+', 'a', 10),
    }
    const right: TSGroupedExpression = {
      kind: TSExpressionKind.Grouped,
      expression: makeExpression('-', 'b', 50),
    }

    expect(
      generator.generate(
        {
          kind: TSExpressionKind.Binary,
          operator: '*',
          left,
          right,
        } as TSBinaryExpression,
        printer,
      ),
    ).toBe('(a + 10) * (b - 50)')
  })

  // CONDITIONAL
  it('should generate conditional operations', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSConditionalExpression = {
      kind: TSExpressionKind.Conditional,
      condition: makeExpression('<', 'a', 10),
      whenTrue: {
        kind: TSExpressionKind.Conditional,
        condition: makeExpression('>', 'b', 50),
        whenTrue: {
          kind: TSExpressionKind.Literal,
          value: 'Done',
        },
        whenFalse: {
          kind: TSExpressionKind.Identifier,
          name: 'd',
        },
      } as TSConditionalExpression,
      whenFalse: {
        kind: TSExpressionKind.Literal,
        value: 'Ouch',
      } as TSLiteralExpression,
    }

    expect(generator.generate(expression, printer)).toBe('if (a < 10) if (b > 50) "Done" else d else "Ouch"')
  })

  // ELEMENT ACCESS
  it('should generate element access by index', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSElementAccessExpression = {
      kind: TSExpressionKind.ElementAccess,
      array: {
        kind: TSExpressionKind.Literal,
        value: [
          {
            kind: TSExpressionKind.Literal,
            value: 1,
          } as TSLiteralExpression,
          {
            kind: TSExpressionKind.Literal,
            value: 2,
          } as TSLiteralExpression,
          {
            kind: TSExpressionKind.Literal,
            value: 3,
          } as TSLiteralExpression,
        ],
      } as TSLiteralExpression,
      index: makeExpression('+', 'i', 1),
      optionalChaining: false,
    }

    expect(generator.generate(expression, printer)).toBe('mutableListOf(1, 2, 3)[i + 1]')
  })
  it('should generate optional element access by index', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSElementAccessExpression = {
      kind: TSExpressionKind.ElementAccess,
      array: {
        kind: TSExpressionKind.Literal,
        value: [
          {
            kind: TSExpressionKind.Literal,
            value: 1,
          } as TSLiteralExpression,
          {
            kind: TSExpressionKind.Literal,
            value: 2,
          } as TSLiteralExpression,
          {
            kind: TSExpressionKind.Literal,
            value: 3,
          } as TSLiteralExpression,
        ],
      } as TSLiteralExpression,
      index: makeExpression('+', 'i', 1),
      optionalChaining: true,
    }

    expect(generator.generate(expression, printer)).toBe('mutableListOf(1, 2, 3)?.get(i + 1)')
  })

  // THIS
  it('should generate "this" variable', () => {
    const expression: TSElementAccessExpression = {
      kind: TSExpressionKind.ElementAccess,
      array: {
        kind: TSExpressionKind.This,
      } as TSThisExpression,
      index: {
        kind: TSExpressionKind.As,
        expression: {
          kind: TSExpressionKind.This,
        },
        type: int32_(),
      } as TSAsExpression,
      optionalChaining: false,
    }

    expect(generator.generate(expression, printer)).toBe('this[this as Int]')
  })

  // SUPER
  it('should generate "super" special variable', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Super,
      } as TSSuperExpression,
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 'Hello',
        } as TSLiteralExpression,
        {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
      ],
      typeArguments: [],
      optionalChaining: false,
    }

    expect(generator.generate(expression, printer)).toBe('super("Hello", a)')
  })

  // BANG
  it('should generate "!" postfix operator (bang)', () => {
    const expression: TSBangExpression = {
      kind: TSExpressionKind.Bang,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      } as TSIdentifier,
    }
    expect(generator.generate(expression, printer)).toBe('a!!')
  })

  // CALL
  it('should generate call expressions', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      } as TSIdentifier,
      args: [
        makeExpression('+', 'a', 10),
        {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'g',
          },
          args: [
            makeExpression('*', 'b', 100),
            {
              kind: TSExpressionKind.Literal,
              value: 'Hello',
            } as TSLiteralExpression,
            {
              kind: TSExpressionKind.Unary,
              operator: '!',
              operand: {
                kind: TSExpressionKind.Identifier,
                name: 'z',
              },
            } as TSUnaryExpression,
          ],
          typeArguments: [],
          optionalChaining: false,
        } as TSCallExpression,
      ],
      typeArguments: [],
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('f(a + 10, g(b * 100, "Hello", !z))')
  })
  it('should generate optional call expressions', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      } as TSIdentifier,
      args: [
        makeExpression('+', 'a', 10),
        {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'g',
          },
          args: [
            makeExpression('*', 'b', 100),
            {
              kind: TSExpressionKind.Literal,
              value: 'Hello',
            } as TSLiteralExpression,
            {
              kind: TSExpressionKind.Unary,
              operator: '!',
              operand: {
                kind: TSExpressionKind.Identifier,
                name: 'z',
              },
            } as TSUnaryExpression,
          ],
          typeArguments: [],
          optionalChaining: true,
        } as TSCallExpression,
      ],
      typeArguments: [],
      optionalChaining: true,
    }
    expect(generator.generate(expression, printer)).toBe('f?.invoke(a + 10, g?.invoke(b * 100, "Hello", !z))')
  })
  it('should generate call expressions with reserved keyword in its name', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: RESERVED_KEYWORD__BREAK__,
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        } as TSLiteralExpression,
      ],
      typeArguments: [],
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe(`\`${RESERVED_KEYWORD__BREAK__}\`(10)`)
  })
  it('should generate call expressions with generics', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      } as TSIdentifier,
      args: [
        {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
      ],
      typeArguments: [ref_('T')],
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('f<T>(a)')
  })
  it('should generate optional call expressions with generics', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      } as TSIdentifier,
      args: [
        {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
      ],
      typeArguments: [ref_('T')],
      optionalChaining: true,
    }
    expect(generator.generate(expression, printer)).toBe('f<T>?.invoke(a)')
  })

  // OBJECT CREATION
  it('should generate object creation expressions with generics', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSObjectCreationExpression = {
      kind: TSExpressionKind.ObjectCreation,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'A',
      },
      args: [
        makeExpression('+', 'a', 10),
        {
          kind: TSExpressionKind.Unary,
          operator: '!',
          operand: {
            kind: TSExpressionKind.Identifier,
            name: 'z',
          },
        } as TSUnaryExpression,
      ],
      typeArguments: [int32_(), ref_('A', refs_('B')), str_()],
    }
    expect(generator.generate(expression, printer)).toBe('A<Int, A<B>, String>(a + 10, !z)')
  })
  it('should generate object creation expressions without generics', () => {
    function makeExpression(operator: TSBinaryOperator, name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator,
        left: {
          kind: TSExpressionKind.Identifier,
          name,
        } as TSIdentifier,
        right: {
          kind: TSExpressionKind.Literal,
          value,
        } as TSLiteralExpression,
      }
    }
    const expression: TSObjectCreationExpression = {
      kind: TSExpressionKind.ObjectCreation,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'A',
      },
      args: [
        makeExpression('+', 'a', 10),
        {
          kind: TSExpressionKind.Unary,
          operator: '!',
          operand: {
            kind: TSExpressionKind.Identifier,
            name: 'z',
          },
        } as TSUnaryExpression,
      ],
      typeArguments: [],
    }
    expect(generator.generate(expression, printer)).toBe('A(a + 10, !z)')
  })
  it('should generate mapped object instatiation', () => {
    const expression: TSObjectCreationExpression = {
      kind: TSExpressionKind.ObjectCreation,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'Map',
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 'key',
        } as TSLiteralExpression,
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        } as TSLiteralExpression,
      ],
      typeArguments: [str_(), int32_()],
    }
    expect(generator.generate(expression, printer)).toBe('mutableMapOf<String, Int>("key", 10)')
  })

  // MEMBER ACCESS
  it('should generate member access expressions', () => {
    const expression: TSMemberAccessExpression = {
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Bang,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          },
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'b',
        },
        optionalChaining: false,
      } as TSMemberAccessExpression,
      member: {
        kind: TSExpressionKind.Identifier,
        name: 'c',
      },
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('a!!.b.c')
  })
  it('should generate optional member access expressions', () => {
    const expression: TSMemberAccessExpression = {
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'b',
        },
        optionalChaining: true,
      } as TSMemberAccessExpression,
      member: {
        kind: TSExpressionKind.Identifier,
        name: 'c',
      },
      optionalChaining: true,
    }
    expect(generator.generate(expression, printer)).toBe('a?.b?.c')
  })
  it('should remap property names when member access to BuiltIns', () => {
    const expression: TSMemberAccessExpression = {
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      } as TSIdentifier,
      member: {
        kind: TSExpressionKind.Identifier,
        name: 'length',
      },
      objectTypeHint: {
        kind: TSExpressionExtraTypeHintKind.BuiltIn,
        type: TSBuiltInType.Array,
      },
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('a.size')
  })
  it('should remap function names when member access to BuiltIns', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'includes',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Array,
        },
        optionalChaining: true,
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        } as TSLiteralExpression,
      ],
      typeArguments: [],
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('a?.contains(10)')
  })
  it('should remap static method calls with remapping the object for Builtins', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'Date',
        } as TSIdentifier,
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'now',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Date,
        },
        optionalChaining: false,
      },
      args: [],
      typeArguments: [],
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('YSDate.now()')
  })
  it('should leave function names intact for reference objects (not builtin)', () => {
    const expression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'includes',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.Reference,
          type: ref_('A'),
        },
        optionalChaining: false,
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        } as TSLiteralExpression,
      ],
      typeArguments: [],
      optionalChaining: false,
    }
    expect(generator.generate(expression, printer)).toBe('a.includes(10)')
  })
  it('should generate member access expressions with reserved keywords in members name', () => {
    const expression: TSMemberAccessExpression = {
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      } as TSIdentifier,
      member: {
        kind: TSExpressionKind.Identifier,
        name: RESERVED_KEYWORD__BREAK__,
      },
      optionalChaining: false,
    }
    const result = generator.generate(expression, printer)
    expect(result).toBe(`a.\`${RESERVED_KEYWORD__BREAK__}\``)
  })

  // ARROW FUNCTIONS
  it('should generate arrow functions with types if provided', () => {
    const expression: TSArrowFunctionExpression = {
      kind: TSExpressionKind.ArrowFunction,
      parameters: [
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'a',
          type: int32_(),
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'b',
          type: str_(),
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'c',
          type: fun_([int64_(), arr_(double_(), false)], str_()),
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: RESERVED_KEYWORD__BREAK__,
          type: str_(),
        },
      ],
      mustWeakify: false,
      returnType: str_(),
      body: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    }
    const expected = new Printer()
      .beginScope('__PRFX__11111@')
      .addLine(`a: Int, b: String, c: (Long, YSArray<Double>) -> String, \`${RESERVED_KEYWORD__BREAK__}\`: String ->`)
      .endScope()
      .print()
    expect(generator.generate(expression, printer)).toBe(expected)
  })
  it('should generate arrow functions with nullable return type types if provided', () => {
    const expression: TSArrowFunctionExpression = {
      kind: TSExpressionKind.ArrowFunction,
      parameters: [
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'a',
          type: int32_(),
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'c',
          type: makeNullable(fun_([int64_(), arr_(double_(), false)], makeNullable(str_()))),
        },
      ],
      mustWeakify: false,
      returnType: makeNullable(str_()),
      body: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    }
    const expected = new Printer()
      .beginScope('__PRFX__11111@')
      .addLine('a: Int, c: ((Long, YSArray<Double>) -> String?)? ->')
      .endScope()
      .print()
    expect(generator.generate(expression, printer)).toBe(expected)
  })
  it('should generate arrow functions without types if omitted', () => {
    const expression: TSArrowFunctionExpression = {
      kind: TSExpressionKind.ArrowFunction,
      parameters: [
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'a',
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'b',
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'c',
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: RESERVED_KEYWORD__BREAK__,
        },
      ],
      mustWeakify: false,
      body: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.Return,
          },
        ],
      },
    }
    const expected = new Printer()
      .beginScope('__PRFX__11111@')
      .addLine(`a, b, c, \`${RESERVED_KEYWORD__BREAK__}\` ->`)
      .addLine('return@__PRFX__11111')
      .endScope()
      .print()
    expect(generator.generate(expression, printer)).toBe(expected)
  })
  it('should generate expression body if one-liner', () => {
    const expression: TSArrowFunctionExpression = {
      kind: TSExpressionKind.ArrowFunction,
      parameters: [
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'a',
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'b',
        },
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'c',
        },
      ],
      mustWeakify: false,
      body: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Binary,
          operator: '-',
          left: {
            kind: TSExpressionKind.Identifier,
            name: 'b',
          },
          right: {
            kind: TSExpressionKind.Identifier,
            name: 'c',
          },
        },
      } as TSExpression,
    }
    const expected = new Printer().beginScope('').addLine('a, b, c ->').addLine('a + b - c').endScope().print()
    expect(generator.generate(expression, printer)).toBe(expected)
  })
  it('should generate identifier expression if one-liner', () => {
    const expression: TSArrowFunctionExpression = {
      kind: TSExpressionKind.ArrowFunction,
      parameters: [
        {
          kind: TSFunctionArgumentType.Arrow,
          name: '_',
        },
      ],
      mustWeakify: false,
      body: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      } as TSIdentifier,
    }
    const expected = new Printer().beginScope('').addLine('_ ->').addLine('a').endScope().print()
    expect(generator.generate(expression, printer)).toBe(expected)
  })
  it('should generate arrow function with return@', () => {
    const expression: TSArrowFunctionExpression = {
      kind: TSExpressionKind.ArrowFunction,
      parameters: [
        {
          kind: TSFunctionArgumentType.Arrow,
          name: 'element',
          type: str_(),
        },
      ],
      mustWeakify: false,
      body: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              args: [
                {
                  kind: TSExpressionKind.Identifier,
                  name: 'element',
                } as TSExpression,
              ],
              expression: {
                kind: TSExpressionKind.Identifier,
                name: 'print',
              } as TSIdentifier,
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
          {
            kind: TSStatementKind.Return,
            expression: {
              kind: TSExpressionKind.Binary,
              left: {
                kind: TSExpressionKind.Literal,
                value: 1,
              } as TSLiteralExpression,
              right: {
                kind: TSExpressionKind.Literal,
                value: 1,
              } as TSLiteralExpression,
              operator: '===',
            } as TSBinaryExpression,
          } as TSReturnStatement,
        ],
      } as TSBlockStatement,
    }

    const callExpression: TSCallExpression = {
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'filter',
      } as TSIdentifier,
      args: [expression],
      typeArguments: [],
      optionalChaining: false,
    }

    const expected = new Printer()
      .beginScope('filter(__PRFX__11111@')
      .addLine('element: String ->')
      .addLine('print(element)')
      .addLine('return@__PRFX__11111 1 == 1')
      .add('})')
      .print()
    expect(generator.generate(callExpression, printer)).toBe(expected)
  })
})
