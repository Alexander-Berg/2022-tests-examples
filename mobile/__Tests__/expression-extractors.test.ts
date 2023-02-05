import { TSLocalTypeKind } from '../src/generators-model/basic-types'
import {
  TSBinaryExpression,
  TSBinaryOperators,
  TSCallExpression,
  TSConditionalExpression,
  TSElementAccessExpression,
  TSExpressionKind,
  TSExpressionTemplateExpressionSpan,
  TSIdentifier,
  TSLiteralExpression,
  TSLiteralTemplateExpressionSpan,
  TSMemberAccessExpression,
  TSSuperExpression,
  TSTemplateExpression,
  TSTemplateExpressionSpanKind,
  TSUnaryOperators,
} from '../src/generators-model/expression'
import { TSArrowFunctionArgument, TSFunctionArgumentType } from '../src/generators-model/function-argument'
import { TSStatementKind } from '../src/generators-model/statement'
import { extractExpression } from '../src/parsing-helpers/expression-extractors'
import { getExpression, int32_, makeSourceFile, refs_, str_ } from './__helpers__/test-helpers'

describe('extractExpression', () => {
  // LITERALS
  it('should extract numeric literals', () => {
    const sample = '10'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: 10,
    })
  })
  it('should extract string literals (single quotes)', () => {
    const sample = "'hello world'"
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: 'hello world',
    })
  })
  it('should extract string literals (double quotes)', () => {
    const sample = '"hello world"'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: '"hello world"',
    })
  })
  it('should extract template strings', () => {
    const sample = '`hello \n world`'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: 'hello \n world',
    })
  })
  it('should extract template strings, starting with template', () => {
    const sample = '`${a}`'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Template,
      spans: [
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          } as TSIdentifier,
        } as TSExpressionTemplateExpressionSpan,
      ],
    } as TSTemplateExpression)
  })
  it('should extract template strings, ending with template', () => {
    const sample = '`hello ${a}${b}`'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Template,
      spans: [
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: 'hello ',
        } as TSLiteralTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          } as TSIdentifier,
        } as TSExpressionTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'b',
          } as TSIdentifier,
        } as TSExpressionTemplateExpressionSpan,
      ],
    } as TSTemplateExpression)
  })
  it('should extract template strings, ending with non-template', () => {
    const sample = '`hello ${a}${b} `'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Template,
      spans: [
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: 'hello ',
        } as TSLiteralTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          } as TSIdentifier,
        } as TSExpressionTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Expression,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: 'b',
          } as TSIdentifier,
        } as TSExpressionTemplateExpressionSpan,
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: ' ',
        } as TSLiteralTemplateExpressionSpan,
      ],
    } as TSTemplateExpression)
  })
  it('should extract template strings, escaping inner strings', () => {
    const sample = '`\'hello\' "world"`'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: '\'hello\' "world"',
    })
  })
  it('should extract template strings with expressions', () => {
    const sample = "`\nhello \\; \t ${world.toString()}; keep $ ${'calm'} and carry ${1 === 1 ? 'on' : 'off'}`"
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Template,
      spans: [
        {
          kind: TSTemplateExpressionSpanKind.Literal,
          value: '\nhello \\; \t ',
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
          value: '; keep $ ',
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
    })
  })
  it('should extract boolean literals', () => {
    const trueSample = 'true'
    const trueExpression = getExpression(makeSourceFile(trueSample))
    const trueResult = extractExpression(trueExpression)
    expect(trueResult).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: true,
    })

    const falseSample = 'false'
    const falseExpression = getExpression(makeSourceFile(falseSample))
    const falseResult = extractExpression(falseExpression)
    expect(falseResult).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: false,
    })
  })
  it('should extract null literals', () => {
    const sample = 'null'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: null,
    })
  })
  it('should extract array literals', () => {
    const sample = '[1, 2, 3]'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Literal,
      value: [
        {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
        {
          kind: TSExpressionKind.Literal,
          value: 2,
        },
        {
          kind: TSExpressionKind.Literal,
          value: 3,
        },
      ],
    })
  })

  // IDENTIFIER
  it('should extract identifier', () => {
    const sample = 'a'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Identifier,
      name: 'a',
    })
  })

  // UNSUPPORTED LITERALS
  it('should throw on unsupported type of literal', () => {
    const sample = '/regex/'
    const expression = getExpression(makeSourceFile(sample))
    expect(() => {
      extractExpression(expression)
    }).toThrow(/Unsupported type of expression/)
  })

  // UNSUPPORTED EXPRESSIONS
  it('should throw on unsupported expression', () => {
    const sample = '[...a, 10]'
    const expression = getExpression(makeSourceFile(sample))
    expect(() => {
      extractExpression(expression)
    }).toThrow(/Unsupported type of expression/)
  })

  // BINARY EXPRESSIONS
  it('should extract binary expressions', () => {
    const sample = 'a + 2'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '+',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      },
      right: {
        kind: TSExpressionKind.Literal,
        value: 2,
      },
    })
  })
  it('should throw on unsupported binary operators', () => {
    const unsupportedOperators = ['**', 'in']
    for (const operator of unsupportedOperators) {
      const sample = `a ${operator} b`
      const expression = getExpression(makeSourceFile(sample))
      expect(() => {
        extractExpression(expression)
      }).toThrow(`Unsupported operator type '${operator}'`)
    }
  })
  it('should extract tree of expressions', () => {
    const sample = 'a + b - 10 * c === true'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '===',
      left: {
        kind: TSExpressionKind.Binary,
        operator: '-',
        left: {
          kind: TSExpressionKind.Binary,
          operator: '+',
          left: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          },
          right: {
            kind: TSExpressionKind.Identifier,
            name: 'b',
          },
        },
        right: {
          kind: TSExpressionKind.Binary,
          operator: '*',
          left: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
          right: {
            kind: TSExpressionKind.Identifier,
            name: 'c',
          },
        },
      },
      right: {
        kind: TSExpressionKind.Literal,
        value: true,
      },
    })
  })
  it('should extract binary expressions from TSBinaryOperator', () => {
    const left: TSIdentifier = {
      kind: TSExpressionKind.Identifier,
      name: 'a',
    }
    const right: TSIdentifier = {
      kind: TSExpressionKind.Identifier,
      name: 'b',
    }
    for (const operator of TSBinaryOperators) {
      const sample = `a ${operator} b`
      const expression = getExpression(makeSourceFile(sample))
      const result = extractExpression(expression)
      expect(result).toMatchObject({
        kind: TSExpressionKind.Binary,
        operator,
        left,
        right,
      })
    }
  })
  it("should support 'instanceof' operator", () => {
    const sample = 'a instanceof MyClass'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: 'instanceof',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      },
      right: {
        kind: TSExpressionKind.Identifier,
        name: 'MyClass',
      },
    })
  })
  it("should support 'Nullish coalescing' operator", () => {
    const sample = 'a ?? f()'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '??',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      },
      right: {
        kind: TSExpressionKind.Call,
        args: [],
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        } as TSIdentifier,
        typeArguments: [],
        fnDeclarationHint: undefined,
        nullableTypeHint: undefined,
        returnTypeHint: undefined,
        optionalChaining: false,
      } as TSCallExpression,
    })
  })
  it("should throw if the right operand of 'instanceof' is not an identifier", () => {
    const sample = 'a instanceof 10'
    const expression = getExpression(makeSourceFile(sample))
    expect(() => {
      extractExpression(expression)
    }).toThrow(/The right operand of 'instanceof' operator must be an identifier/)
  })

  // UNARY EXPRESSIONS
  it('should extract unary expressions', () => {
    const sample = '!a'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Unary,
      operator: '!',
      operand: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      },
    })
  })
  it('should extract tree of expressions', () => {
    const sample = '!a && ~10 + -c'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '&&',
      left: {
        kind: TSExpressionKind.Unary,
        operator: '!',
        operand: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
      },
      right: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Unary,
          operator: '~',
          operand: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
        },
        right: {
          kind: TSExpressionKind.Unary,
          operator: '-',
          operand: {
            kind: TSExpressionKind.Identifier,
            name: 'c',
          },
        },
      },
    })
  })
  it('should extract unary expressions from TSUnaryOperators', () => {
    const operand: TSIdentifier = {
      kind: TSExpressionKind.Identifier,
      name: 'a',
    }
    for (const operator of TSUnaryOperators) {
      const sample = `${operator}a`
      const expression = getExpression(makeSourceFile(sample))
      const result = extractExpression(expression)
      expect(result).toMatchObject({
        kind: TSExpressionKind.Unary,
        operand,
        operator,
      })
    }
  })
  it('should throw on ++ and -- operators', () => {
    expect(() => {
      extractExpression(getExpression(makeSourceFile('a++')))
    }).toThrow("'++' operator is not supported")
    expect(() => {
      extractExpression(getExpression(makeSourceFile('++a')))
    }).toThrow("'++' operator is not supported")
    expect(() => {
      extractExpression(getExpression(makeSourceFile('a--')))
    }).toThrow("'--' operator is not supported")
    expect(() => {
      extractExpression(getExpression(makeSourceFile('--a')))
    }).toThrow("'--' operator is not supported")
  })

  // GROUPED EXPRESSION
  it('should extract grouped expression', () => {
    const sample = 'a * (b - 10) * (-3)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '*',
      left: {
        kind: TSExpressionKind.Binary,
        operator: '*',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Grouped,
          expression: {
            kind: TSExpressionKind.Binary,
            operator: '-',
            left: {
              kind: TSExpressionKind.Identifier,
              name: 'b',
            },
            right: {
              kind: TSExpressionKind.Literal,
              value: 10,
            },
          },
        },
      },
      right: {
        kind: TSExpressionKind.Grouped,
        expression: {
          kind: TSExpressionKind.Unary,
          operator: '-',
          operand: {
            kind: TSExpressionKind.Literal,
            value: 3,
          },
        },
      },
    })
  })

  // ASSIGNMENT EXPRESSION
  it('should extract assignment expression', () => {
    const sample = 'a = (b - 10) * -3'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      },
      right: {
        kind: TSExpressionKind.Binary,
        operator: '*',
        left: {
          kind: TSExpressionKind.Grouped,
          expression: {
            kind: TSExpressionKind.Binary,
            operator: '-',
            left: {
              kind: TSExpressionKind.Identifier,
              name: 'b',
            },
            right: {
              kind: TSExpressionKind.Literal,
              value: 10,
            },
          },
        },
        right: {
          kind: TSExpressionKind.Unary,
          operator: '-',
          operand: {
            kind: TSExpressionKind.Literal,
            value: 3,
          },
        },
      },
    })
  })
  it('should throw on some compound assignment operators', () => {
    const unsupportedOperators = ['**=', '<<=', '>>=', '>>>=', '&=', '^=', '|=']
    for (const operator of unsupportedOperators) {
      const sample = `a ${operator} b`
      const expression = getExpression(makeSourceFile(sample))
      expect(() => {
        extractExpression(expression)
      }).toThrow(`Unsupported operator type '${operator}'`)
    }
  })

  // CONDITIONAL EXPRESSION
  it('should extract assignment expression', () => {
    const sample = 'a * 10 ? b - 10 : 3'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Conditional,
      condition: {
        kind: TSExpressionKind.Binary,
        operator: '*',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      },
      whenTrue: {
        kind: TSExpressionKind.Binary,
        operator: '-',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'b',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      },
      whenFalse: {
        kind: TSExpressionKind.Literal,
        value: 3,
      },
    })
  })

  // ARRAY INDEXING EXPRESSION
  it('should extract element access expression', () => {
    const sample = '[1, 2, 3][a + 1]'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.ElementAccess,
      array: {
        kind: TSExpressionKind.Literal,
        value: [
          {
            kind: TSExpressionKind.Literal,
            value: 1,
          },
          {
            kind: TSExpressionKind.Literal,
            value: 2,
          },
          {
            kind: TSExpressionKind.Literal,
            value: 3,
          },
        ],
      },
      index: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
      },
      optionalChaining: false,
    } as TSElementAccessExpression)
  })
  it('should extract optional element access expression', () => {
    const sample = '[1, 2, 3]?.[a + 1]'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.ElementAccess,
      array: {
        kind: TSExpressionKind.Literal,
        value: [
          {
            kind: TSExpressionKind.Literal,
            value: 1,
          },
          {
            kind: TSExpressionKind.Literal,
            value: 2,
          },
          {
            kind: TSExpressionKind.Literal,
            value: 3,
          },
        ],
      },
      index: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
      },
      optionalChaining: true,
    } as TSElementAccessExpression)
  })

  // PROPERTY ACCESS EXPRESSION
  it('should extract property access', () => {
    const sample = '(10 + 20).a.b'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Grouped,
          expression: {
            kind: TSExpressionKind.Binary,
            operator: '+',
            left: {
              kind: TSExpressionKind.Literal,
              value: 10,
            },
            right: {
              kind: TSExpressionKind.Literal,
              value: 20,
            },
          },
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        optionalChaining: false,
      },
      member: {
        kind: TSExpressionKind.Identifier,
        name: 'b',
      },
      optionalChaining: false,
    })
  })
  it('should extract optional property access', () => {
    const sample = '(10 + 20)?.a?.b'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Grouped,
          expression: {
            kind: TSExpressionKind.Binary,
            operator: '+',
            left: {
              kind: TSExpressionKind.Literal,
              value: 10,
            },
            right: {
              kind: TSExpressionKind.Literal,
              value: 20,
            },
          },
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        optionalChaining: true,
      },
      member: {
        kind: TSExpressionKind.Identifier,
        name: 'b',
      },
      optionalChaining: true,
    })
  })

  // OBJECT CREATION EXPRESSION
  it("should create generic objects with operator 'new'", () => {
    const sample = "new A<T>(10 + 20, 'hello')"
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.ObjectCreation,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'A',
      },
      args: [
        {
          kind: TSExpressionKind.Binary,
          operator: '+',
          left: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
          right: {
            kind: TSExpressionKind.Literal,
            value: 20,
          },
        },
        {
          kind: TSExpressionKind.Literal,
          value: 'hello',
        },
      ],
      typeArguments: refs_('T'),
    })
  })
  it("should create non-generic objects with operator 'new'", () => {
    const sample = 'new A(10)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.ObjectCreation,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'A',
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      ],
      typeArguments: [],
    })
  })
  it("should throw if 'new' used with non-class items", () => {
    const sample = "new ((function() { return String })())('Hello')"
    const expression = getExpression(makeSourceFile(sample))
    expect(() => {
      extractExpression(expression)
    }).toThrow(/Only identifiers are supported as objects for creation with 'new'/)
  })

  // CALL EXPRESSION
  it('should support call with named callable', () => {
    const sample = "f<Int32>('A')"
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 'A',
        },
      ],
      typeArguments: [int32_()],
      optionalChaining: false,
    })
  })
  it('should support optional call with named callable', () => {
    const sample = "f?.<Int32>('A')"
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 'A',
        } as TSLiteralExpression,
      ],
      typeArguments: [int32_()],
      optionalChaining: true,
    } as TSCallExpression)
  })
  it('should support call with element from array', () => {
    const sample = 'a[10](10)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.ElementAccess,
        array: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        index: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
        optionalChaining: false,
      } as TSElementAccessExpression,
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        } as TSLiteralExpression,
      ],
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should support call with member access', () => {
    const sample = 'o.method<T>(10, 20)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'o',
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'method',
        },
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
        {
          kind: TSExpressionKind.Literal,
          value: 20,
        },
      ],
      typeArguments: refs_('T'),
    })
  })
  it('should support call within call', () => {
    const sample = 'f<T>(10)(20)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        },
        args: [
          {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
        ],
        typeArguments: refs_('T'),
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 20,
        },
      ],
      typeArguments: [],
    })
  })
  it('should support call with bang assertion', () => {
    const sample = 'o.method!(10, 20)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Bang,
        expression: {
          kind: TSExpressionKind.MemberAccess,
          object: {
            kind: TSExpressionKind.Identifier,
            name: 'o',
          },
          member: {
            kind: TSExpressionKind.Identifier,
            name: 'method',
          },
        },
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
        {
          kind: TSExpressionKind.Literal,
          value: 20,
        },
      ],
      typeArguments: [],
    })
  })
  it('should throw on unsupported types of calls', () => {
    const sample = '(f)(20)'
    const expression = getExpression(makeSourceFile(sample))
    expect(() => {
      extractExpression(expression)
    }).toThrow(/Unsupported type of call expression/)
  })

  // 'THIS' EXPRESSION
  it("should support 'this' expression", () => {
    const sample = 'this.a = this.f() + 10'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.This,
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
      },
      right: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.MemberAccess,
            object: {
              kind: TSExpressionKind.This,
            },
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'f',
            },
          },
          args: [],
          typeArguments: [],
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      },
    })
  })

  // 'SUPER' EXPRESSION
  it("should support 'super' expression", () => {
    const sample = 'k > super.f + 10'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '>',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.MemberAccess,
          object: {
            kind: TSExpressionKind.Super,
          },
          member: {
            kind: TSExpressionKind.Identifier,
            name: 'f',
          },
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      },
    })
  })
  it("should support 'super' call expression", () => {
    const sample = 'super(a)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Super,
      } as TSSuperExpression,
      args: [
        {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
      ],
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })

  // ARROW FUNCTIONS EXPRESSION
  it('should support arrow functions expression', () => {
    const sample = 'k = (a: Int32): string => String.valueOf(a)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.ArrowFunction,
        parameters: [
          {
            kind: TSFunctionArgumentType.Arrow,
            name: 'a',
            type: int32_(),
          },
        ],
        returnType: str_(),
        mustWeakify: false,
        body: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.MemberAccess,
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'valueOf',
            },
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'String',
            },
          },
          args: [
            {
              kind: TSExpressionKind.Identifier,
              name: 'a',
            },
          ],
          typeArguments: [],
        },
      },
    })
  })
  it('should extract arrow function body as Block statement', () => {
    const sample = 'k = (a: Int32): string => { }'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.ArrowFunction,
        parameters: [
          {
            kind: TSFunctionArgumentType.Arrow,
            name: 'a',
            type: int32_(),
          },
        ],
        returnType: str_(),
        mustWeakify: false,
        body: {
          kind: TSStatementKind.Block,
          statements: [],
        },
      },
    })
  })
  it('should support omitting arguments and return types', () => {
    const sample = 'k = (a) => String.valueOf(a)'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.ArrowFunction,
        parameters: [
          {
            kind: TSFunctionArgumentType.Arrow,
            name: 'a',
          } as TSArrowFunctionArgument,
        ],
        mustWeakify: false,
        body: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.MemberAccess,
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'valueOf',
            },
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'String',
            },
          },
          args: [
            {
              kind: TSExpressionKind.Identifier,
              name: 'a',
            },
          ],
          typeArguments: [],
        },
      },
    })
  })
  it('should support extracting const-functions with identifiers', () => {
    // https://st.yandex-team.ru/SSP-124
    const sample = 'k = (_) => a'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.ArrowFunction,
        parameters: [
          {
            kind: TSFunctionArgumentType.Arrow,
            name: '_',
          } as TSArrowFunctionArgument,
        ],
        mustWeakify: false,
        body: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        } as TSIdentifier,
      },
    })
  })
  it('should support weakification with "weakThis" function wrapper', () => {
    const sample = 'k = weakThis((a) => String.valueOf(a))'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.ArrowFunction,
        parameters: [
          {
            kind: TSFunctionArgumentType.Arrow,
            name: 'a',
          },
        ],
        mustWeakify: true,
        body: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.MemberAccess,
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'valueOf',
            },
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'String',
            },
          },
          args: [
            {
              kind: TSExpressionKind.Identifier,
              name: 'a',
            },
          ],
          typeArguments: [],
        },
      },
    })
  })

  // AS EXPRESSION
  it('should support "as" expression', () => {
    const sample = 'k = a as String'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.As,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        type: {
          kind: TSLocalTypeKind.Reference,
          name: 'String',
        },
      },
    })
  })

  // BANG EXPRESSIONS
  it('should support "bang" expression', () => {
    const sample = 'k = a!'
    const expression = getExpression(makeSourceFile(sample))
    const result = extractExpression(expression)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Binary,
      operator: '=',
      left: {
        kind: TSExpressionKind.Identifier,
        name: 'k',
      },
      right: {
        kind: TSExpressionKind.Bang,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
      },
    })
  })
})
