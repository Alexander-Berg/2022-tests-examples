import { makeNullable, TSLocalType } from '../../../src/generators-model/basic-types'
import {
  TSBinaryExpression,
  TSBinaryOperator,
  TSCallExpression,
  TSExpression,
  TSExpressionKind,
  TSIdentifier,
  TSLiteralExpression,
  TSMemberAccessExpression,
  TSObjectCreationExpression,
  TSUnaryExpression,
} from '../../../src/generators-model/expression'
import {
  TSBlockStatement,
  TSBreakStatement,
  TSContinueStatement,
  TSDoWhileStatement,
  TSExpressionStatement,
  TSForEachStatement,
  TSForRangeStatement,
  TSIfElseStatement,
  TSImportStatement,
  TSReturnStatement,
  TSStatement,
  TSStatementKind,
  TSSwitchCase,
  TSSwitchStatement,
  TSThrowStatement,
  TSVariableDeclarationStatement,
  TSWhileStatement,
} from '../../../src/generators-model/statement'
import { SwiftExpressionGenerator } from '../../../src/generators/swift/expression-generator'
import { SwiftStatementGenerator } from '../../../src/generators/swift/statement-generator'
import { SwiftTypeMapper } from '../../../src/generators/swift/swift-type-mapper'
import Printer from '../../../src/utils/printer'
import { bool_, double_, int32_, int64_, RESERVED_KEYWORD__BREAK__, str_ } from '../../__helpers__/test-helpers'

describe('SwiftStatementsGenerator', () => {
  let printer: Printer
  let generator: SwiftStatementGenerator
  beforeEach(() => {
    printer = new Printer()
    const expressionGenerator = new SwiftExpressionGenerator((mapping) => SwiftTypeMapper.getTypeMapping(mapping))
    generator = new SwiftStatementGenerator(printer, expressionGenerator)
  })

  // EXPRESSION STATEMENT
  it('should generate expression statements', () => {
    const statements: TSExpressionStatement = {
      kind: TSStatementKind.ExpressionStatement,
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
          } as TSLiteralExpression,
        ],
        typeArguments: [],
        optionalChaining: false,
      } as TSCallExpression,
    }
    generator.generate(statements)
    expect(generator.extract()).toBe(new Printer().addLine('f(10)').print())
  })

  // BLOCK
  it('should generate block statements', () => {
    const expressionStatement: TSExpressionStatement = {
      kind: TSStatementKind.ExpressionStatement,
      expression: {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        } as TSIdentifier,
        args: [
          {
            kind: TSExpressionKind.Literal,
            value: 10,
          } as TSLiteralExpression,
        ],
        typeArguments: [],
        optionalChaining: false,
      } as TSCallExpression,
    }
    const returnStatement: TSReturnStatement = {
      kind: TSStatementKind.Return,
      expression: {
        kind: TSExpressionKind.Unary,
        operator: '!',
        operand: {
          kind: TSExpressionKind.Identifier,
          name: 'g',
        },
      } as TSUnaryExpression,
    }

    const statements: TSBlockStatement = {
      kind: TSStatementKind.Block,
      statements: [expressionStatement, returnStatement],
    }
    generator.generate(statements)
    const expected = new Printer().addLine('f(10)').addLine('return !g').print()
    expect(generator.extract()).toBe(expected)
  })

  // RETURN STATEMENT
  it('should generate return with expression statement', () => {
    const sample: TSReturnStatement = {
      kind: TSStatementKind.Return,
      expression: {
        kind: TSExpressionKind.Unary,
        operator: '!',
        operand: {
          kind: TSExpressionKind.Identifier,
          name: 'g',
        },
      } as TSUnaryExpression,
    }
    generator.generate(sample)
    const expected = new Printer().addLine('return !g').print()
    expect(printer.print()).toBe(expected)
  })
  it('should generate empty return statement', () => {
    const sample: TSReturnStatement = {
      kind: TSStatementKind.Return,
    }
    generator.generate(sample)
    const expected = new Printer().addLine('return').print()
    expect(generator.extract()).toBe(expected)
  })

  // BREAK STATEMENT
  it('should generate break statement', () => {
    const sample: TSBreakStatement = {
      kind: TSStatementKind.Break,
    }
    generator.generate(sample)
    const expected = new Printer().addLine('break').print()
    expect(generator.extract()).toBe(expected)
  })

  // CONTINUE STATEMENT
  it('should generate continue statement', () => {
    const sample: TSContinueStatement = {
      kind: TSStatementKind.Continue,
    }
    generator.generate(sample)
    const expected = new Printer().addLine('continue').print()
    expect(generator.extract()).toBe(expected)
  })

  // WHILE STATEMENT
  it('should generate while statement', () => {
    const sample: TSWhileStatement = {
      kind: TSStatementKind.While,
      condition: {
        kind: TSExpressionKind.Binary,
        operator: '>',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      } as TSBinaryExpression,
      block: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
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
                } as TSLiteralExpression,
              ],
              typeArguments: [],
              optionalChaining: true,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ],
      } as TSBlockStatement,
    }
    generator.generate(sample)
    const expected = new Printer().beginScope('while a > 10').addLine('f?(10)').endScope().print()
    expect(generator.extract()).toBe(expected)
  })

  // DO-WHILE STATEMENT
  it('should generate repeat-while statement', () => {
    const sample: TSDoWhileStatement = {
      kind: TSStatementKind.DoWhile,
      condition: {
        kind: TSExpressionKind.Binary,
        operator: '>',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      } as TSBinaryExpression,
      block: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
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
                } as TSLiteralExpression,
              ],
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ],
      } as TSBlockStatement,
    }
    generator.generate(sample)
    const expected = new Printer().beginScope('repeat').addLine('f(10)').unindent().addLine('} while a > 10').print()
    expect(generator.extract()).toBe(expected)
  })

  // IF-ELSE STATEMENT
  it('should generate if statement', () => {
    const sample: TSIfElseStatement = {
      kind: TSStatementKind.IfElse,
      condition: {
        kind: TSExpressionKind.Binary,
        operator: '>',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      } as TSBinaryExpression,
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
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
                } as TSLiteralExpression,
              ],
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ],
      } as TSBlockStatement,
    }
    generator.generate(sample)
    const expected = new Printer().beginScope('if a > 10').addLine('f(10)').endScope().print()
    expect(generator.extract()).toBe(expected)
  })
  it('should generate if-else statement', () => {
    const sample: TSIfElseStatement = {
      kind: TSStatementKind.IfElse,
      condition: {
        kind: TSExpressionKind.Binary,
        operator: '>',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      } as TSBinaryExpression,
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
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
                } as TSLiteralExpression,
              ],
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ],
      } as TSBlockStatement,
      elseBlock: {
        kind: TSStatementKind.Block,
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: {
                kind: TSExpressionKind.Identifier,
                name: 'g',
              },
              args: [
                {
                  kind: TSExpressionKind.Literal,
                  value: 20,
                } as TSLiteralExpression,
                {
                  kind: TSExpressionKind.Identifier,
                  name: 'z',
                } as TSIdentifier,
              ],
              typeArguments: [],
              optionalChaining: true,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ],
      } as TSBlockStatement,
    }
    generator.generate(sample)
    const expected = new Printer()
      .beginScope('if a > 10')
      .addLine('f(10)')
      .unindent()
      .add('}')
      .beginScope(' else')
      .addLine('g?(20, z)')
      .endScope()
      .print()
    expect(generator.extract()).toBe(expected)
  })
  it('should generate empty-bodied if-else statement', () => {
    const sample: TSIfElseStatement = {
      kind: TSStatementKind.IfElse,
      condition: {
        kind: TSExpressionKind.Binary,
        operator: '>',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      } as TSBinaryExpression,
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [],
      } as TSBlockStatement,
      elseBlock: {
        kind: TSStatementKind.Block,
        statements: [],
      } as TSBlockStatement,
    }
    generator.generate(sample)
    const expected = new Printer().beginScope('if a > 10').unindent().add('}').beginScope(' else').endScope().print()
    expect(generator.extract()).toBe(expected)
  })
  it('should generate if-else-if statement', () => {
    function condition(name: string, operator: TSBinaryOperator, value: number): TSBinaryExpression {
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
    function call(f: string, value: number, name: string): TSExpressionStatement {
      return {
        kind: TSStatementKind.ExpressionStatement,
        expression: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: f,
          },
          args: [
            {
              kind: TSExpressionKind.Literal,
              value,
            } as TSLiteralExpression,
            {
              kind: TSExpressionKind.Identifier,
              name,
            } as TSIdentifier,
          ],
          typeArguments: [],
          optionalChaining: false,
        } as TSCallExpression,
      }
    }
    const sample: TSIfElseStatement = {
      kind: TSStatementKind.IfElse,
      condition: condition('a', '>', 10),
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [call('f', 10, 'alpha')],
      } as TSBlockStatement,
      elseBlock: {
        kind: TSStatementKind.IfElse,
        condition: condition('b', '<', 40),
        thenBlock: {
          kind: TSStatementKind.Block,
          statements: [call('g', 20, 'beta')],
        },
        elseBlock: {
          kind: TSStatementKind.Block,
          statements: [call('h', 100, 'gamma')],
        },
      } as TSIfElseStatement,
    }
    generator.generate(sample)
    const expected = new Printer()
      .beginScope('if a > 10')
      .addLine('f(10, alpha)')
      .unindent()
      .add('}')
      .beginScope(' else if b < 40')
      .addLine('g(20, beta)')
      .unindent()
      .beginScope('} else')
      .addLine('h(100, gamma)')
      .endScope()
      .print()
    expect(generator.extract()).toBe(expected)
  })

  // SWITCH STATEMENT
  it('should generate switch statement', () => {
    function caseClause(literal: string, statements: readonly TSStatement[], appendBreak: boolean): TSSwitchCase {
      const preparedStatements = appendBreak ? statements.concat({ kind: TSStatementKind.Break }) : statements
      return {
        expression: {
          kind: TSExpressionKind.Literal,
          value: literal,
        } as TSLiteralExpression,
        statements: preparedStatements,
      }
    }
    function call(f: string, value: number, name: string): TSExpressionStatement {
      return {
        kind: TSStatementKind.ExpressionStatement,
        expression: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: f,
          },
          args: [
            {
              kind: TSExpressionKind.Literal,
              value,
            } as TSLiteralExpression,
            {
              kind: TSExpressionKind.Identifier,
              name,
            } as TSIdentifier,
          ],
          typeArguments: [],
          optionalChaining: false,
        } as TSCallExpression,
      }
    }
    const sample: TSSwitchStatement = {
      kind: TSStatementKind.Switch,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      } as TSIdentifier,
      cases: [
        caseClause('A', [call('f', 10, 'alpha'), call('g', 20, 'beta')], true),
        caseClause('B', [], true),
        caseClause('C', [], false),
        caseClause(
          'D',
          [
            {
              kind: TSStatementKind.Return,
              expression: call('h', 30, 'gamma').expression,
            } as TSReturnStatement,
          ],
          false,
        ),
      ],
      defaultCase: {
        statements: [call('k', 40, 'delta'), call('l', 50, 'epsilon')],
      },
    }
    generator.generate(sample)
    const expected = new Printer()
      .beginScope('switch a')
      .addLine('case "A":')
      .indent()
      .addLine('f(10, alpha)')
      .addLine('g(20, beta)')
      .unindent()
      .addLine('case "B":')
      .indent()
      .addLine('break')
      .unindent()
      .addLine('case "C":')
      .indent()
      .addLine('fallthrough')
      .unindent()
      .addLine('case "D":')
      .indent()
      .addLine('return h(30, gamma)')
      .unindent()
      .addLine('default:')
      .indent()
      .addLine('k(40, delta)')
      .addLine('l(50, epsilon)')
      .unindent()
      .endScope()
      .print()
    expect(generator.extract()).toBe(expected)
  })
  it('should generate switch statement without default', () => {
    function caseClause(literal: string, statements: readonly TSStatement[], appendBreak: boolean): TSSwitchCase {
      const preparedStatements = appendBreak ? statements.concat({ kind: TSStatementKind.Break }) : statements
      return {
        expression: {
          kind: TSExpressionKind.Literal,
          value: literal,
        } as TSLiteralExpression,
        statements: preparedStatements,
      }
    }
    function call(f: string, value: number, name: string): TSExpressionStatement {
      return {
        kind: TSStatementKind.ExpressionStatement,
        expression: {
          kind: TSExpressionKind.Call,
          expression: {
            kind: TSExpressionKind.Identifier,
            name: f,
          },
          args: [
            {
              kind: TSExpressionKind.Literal,
              value,
            } as TSLiteralExpression,
            {
              kind: TSExpressionKind.Identifier,
              name,
            } as TSIdentifier,
          ],
          typeArguments: [],
          optionalChaining: false,
        } as TSCallExpression,
      }
    }
    const sample: TSSwitchStatement = {
      kind: TSStatementKind.Switch,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'a',
      } as TSIdentifier,
      cases: [
        caseClause('A', [call('f', 10, 'alpha'), call('g', 20, 'beta')], true),
        caseClause('B', [], true),
        caseClause('C', [], false),
        caseClause(
          'D',
          [
            {
              kind: TSStatementKind.Return,
              expression: call('h', 30, 'gamma').expression,
            } as TSReturnStatement,
          ],
          false,
        ),
      ],
    }
    generator.generate(sample)
    const expected = new Printer()
      .beginScope('switch a')
      .addLine('case "A":')
      .indent()
      .addLine('f(10, alpha)')
      .addLine('g(20, beta)')
      .unindent()
      .addLine('case "B":')
      .indent()
      .addLine('break')
      .unindent()
      .addLine('case "C":')
      .indent()
      .addLine('fallthrough')
      .unindent()
      .addLine('case "D":')
      .indent()
      .addLine('return h(30, gamma)')
      .unindent()
      .endScope()
      .print()
    expect(generator.extract()).toBe(expected)
  })

  // VARIABLE DECLARATION STATEMENT
  it('should throw if variable declaration statement with neither nor initializer', () => {
    const sampleMutable: TSVariableDeclarationStatement = {
      kind: TSStatementKind.VariableDeclaration,
      name: 'a',
      mutable: true,
    }
    const sampleImmutable: TSVariableDeclarationStatement = {
      kind: TSStatementKind.VariableDeclaration,
      name: 'a',
      mutable: false,
    }
    expect(() => {
      generator.generate(sampleMutable)
    }).toThrow('Either type, or initializer, or both must be present in variable declaration')
    expect(() => {
      generator.generate(sampleImmutable)
    }).toThrow('Either type, or initializer, or both must be present in variable declaration')
  })
  it('should generate variable declaration statement with type and no initializer', () => {
    function declaration(
      name: string,
      mutable: boolean,
      type?: TSLocalType,
      initializer?: TSExpression,
    ): TSVariableDeclarationStatement {
      return {
        kind: TSStatementKind.VariableDeclaration,
        mutable,
        name,
        type,
        initializer,
      }
    }
    generator.generate(declaration('a', true, int32_()))
    expect(generator.extract()).toBe(new Printer().addLine('var a: Int32').print())

    generator.generate(declaration('a', false, makeNullable(str_())))
    expect(generator.extract()).toBe(new Printer().addLine('let a: String!').print())

    generator.generate(declaration(RESERVED_KEYWORD__BREAK__, true, int32_()))
    expect(generator.extract()).toBe(new Printer().addLine(`var \`${RESERVED_KEYWORD__BREAK__}\`: Int32`).print())
  })
  it('should generate variable declaration statement with initializer and no type', () => {
    function declaration(
      name: string,
      mutable: boolean,
      type?: TSLocalType,
      initializer?: TSExpression,
    ): TSVariableDeclarationStatement {
      return {
        kind: TSStatementKind.VariableDeclaration,
        mutable,
        name,
        type,
        initializer,
      }
    }
    function call(): TSCallExpression {
      return {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
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
    }
    generator.generate(declaration('a', true, undefined, call()))
    expect(generator.extract()).toBe(new Printer().addLine('var a = f(10)').print())

    generator.generate(declaration('a', false, undefined, call()))
    expect(generator.extract()).toBe(new Printer().addLine('let a = f(10)').print())

    generator.generate(declaration(RESERVED_KEYWORD__BREAK__, false, undefined, call()))
    expect(generator.extract()).toBe(new Printer().addLine(`let \`${RESERVED_KEYWORD__BREAK__}\` = f(10)`).print())
  })
  it('should generate variable declaration statement with nullable initializer', () => {
    function declaration(
      name: string,
      mutable: boolean,
      type?: TSLocalType,
      initializer?: TSExpression,
    ): TSVariableDeclarationStatement {
      return {
        kind: TSStatementKind.VariableDeclaration,
        mutable,
        name,
        type,
        initializer,
      }
    }
    function call(): TSCallExpression {
      return {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        },
        args: [
          {
            kind: TSExpressionKind.Literal,
            value: 10,
          } as TSLiteralExpression,
        ],
        typeArguments: [],
        nullableTypeHint: int64_(),
        optionalChaining: false,
      }
    }
    function member(): TSMemberAccessExpression {
      return {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'A',
        } as TSIdentifier,
        member: id(),
        nullableTypeHint: str_(),
        optionalChaining: false,
      }
    }
    function id(): TSIdentifier {
      return {
        kind: TSExpressionKind.Identifier,
        name: 'z',
        nullableTypeHint: bool_(),
      }
    }
    generator.generate(declaration('a', true, undefined, call()))
    expect(generator.extract()).toBe(new Printer().addLine('var a: Int64! = f(10)').print())

    generator.generate(declaration('a', false, undefined, call()))
    expect(generator.extract()).toBe(new Printer().addLine('let a: Int64! = f(10)').print())

    generator.generate(declaration('a', true, undefined, member()))
    expect(generator.extract()).toBe(new Printer().addLine('var a: String! = A.z').print())

    generator.generate(declaration('a', false, undefined, member()))
    expect(generator.extract()).toBe(new Printer().addLine('let a: String! = A.z').print())

    generator.generate(declaration('a', true, undefined, id()))
    expect(generator.extract()).toBe(new Printer().addLine('var a: Bool! = z').print())

    generator.generate(declaration('a', false, undefined, id()))
    expect(generator.extract()).toBe(new Printer().addLine('let a: Bool! = z').print())
  })

  // FOR-IN STATEMENT
  it('should generate for-in statement with variable', () => {
    function forIn(name: string, mutable: boolean, type?: TSLocalType): TSForEachStatement {
      return {
        kind: TSStatementKind.ForEach,
        expression: {
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
          ] as readonly TSLiteralExpression[],
        } as TSLiteralExpression,
        initializer: {
          kind: TSStatementKind.VariableDeclaration,
          mutable,
          name,
          type,
        },
        block: {
          kind: TSStatementKind.Block,
          statements: [
            {
              kind: TSStatementKind.Continue,
            },
          ],
        },
      }
    }
    generator.generate(forIn('i', true, double_()))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for var i: Double in YSArray(1, 2, 3)').addLine('continue').endScope().print(),
    )
    generator.generate(forIn('i', false, int32_()))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for i: Int32 in YSArray(1, 2, 3)').addLine('continue').endScope().print(),
    )
    generator.generate(forIn('i', true))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for var i in YSArray(1, 2, 3)').addLine('continue').endScope().print(),
    )
    generator.generate(forIn('i', false))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for i in YSArray(1, 2, 3)').addLine('continue').endScope().print(),
    )

    // Reserved keywords
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, true, double_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for var \`${RESERVED_KEYWORD__BREAK__}\`: Double in YSArray(1, 2, 3)`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, false, int32_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for \`${RESERVED_KEYWORD__BREAK__}\`: Int32 in YSArray(1, 2, 3)`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, true))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for var \`${RESERVED_KEYWORD__BREAK__}\` in YSArray(1, 2, 3)`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, false))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for \`${RESERVED_KEYWORD__BREAK__}\` in YSArray(1, 2, 3)`)
        .addLine('continue')
        .endScope()
        .print(),
    )
  })

  // FOR-RANGE STATEMENT
  it('should generate for-range statement without steps', () => {
    function forRange(
      name: string,
      mutable: boolean,
      growing: boolean,
      from: TSExpression,
      to: TSExpression,
      type?: TSLocalType,
    ): TSForRangeStatement {
      return {
        kind: TSStatementKind.ForRange,
        initializer: {
          kind: TSStatementKind.VariableDeclaration,
          mutable,
          name,
          type,
        },
        growing,
        from,
        to,
        block: {
          kind: TSStatementKind.Block,
          statements: [
            {
              kind: TSStatementKind.Continue,
            },
          ],
        },
      }
    }
    function expr(name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator: '+',
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

    generator.generate(forRange('i', true, true, expr('a', 10), expr('b', 20)))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope('for var i in stride(from: a + 10, to: b + 20, by: 1)')
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forRange('i', false, false, expr('a', 10), expr('b', 20), int32_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope('for i: Int32 in stride(from: a + 10, through: b + 20, by: -1)')
        .addLine('continue')
        .endScope()
        .print(),
    )

    // Reserved keywords
    generator.generate(forRange(RESERVED_KEYWORD__BREAK__, true, true, expr('a', 10), expr('b', 20)))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for var \`${RESERVED_KEYWORD__BREAK__}\` in stride(from: a + 10, to: b + 20, by: 1)`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forRange(RESERVED_KEYWORD__BREAK__, false, false, expr('a', 10), expr('b', 20), int32_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for \`${RESERVED_KEYWORD__BREAK__}\`: Int32 in stride(from: a + 10, through: b + 20, by: -1)`)
        .addLine('continue')
        .endScope()
        .print(),
    )
  })
  it('should generate for-range statement with steps', () => {
    function forRange(
      mutable: boolean,
      growing: boolean,
      from: TSExpression,
      to: TSExpression,
      step: TSExpression,
      type?: TSLocalType,
    ): TSForRangeStatement {
      return {
        kind: TSStatementKind.ForRange,
        initializer: {
          kind: TSStatementKind.VariableDeclaration,
          mutable,
          name: 'i',
          type,
        },
        growing,
        from,
        to,
        step,
        block: {
          kind: TSStatementKind.Block,
          statements: [
            {
              kind: TSStatementKind.Continue,
            },
          ],
        },
      }
    }
    function expr(name: string, value: number): TSBinaryExpression {
      return {
        kind: TSExpressionKind.Binary,
        operator: '+',
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
    generator.generate(forRange(true, true, expr('a', 10), expr('b', 20), expr('c', 30)))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope('for var i in stride(from: a + 10, to: b + 20, by: c + 30)')
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forRange(false, false, expr('a', 10), expr('b', 20), expr('c', 30), int64_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope('for i: Int64 in stride(from: a + 10, through: b + 20, by: -(c + 30))')
        .addLine('continue')
        .endScope()
        .print(),
    )
  })

  // IMPORT STATEMENT
  it('should generate for-range statement without steps', () => {
    function makeImport(names: readonly string[], source: string): TSImportStatement {
      return {
        kind: TSStatementKind.Import,
        names,
        source,
      }
    }
    generator.generate(makeImport(['A, B'], './ab'))
    expect(generator.extract()).toBe('')
  })

  // THROW STATEMENT
  it('should generate fatalError on throw statement', () => {
    const statement = {
      kind: TSStatementKind.Throw,
      expression: {
        kind: TSExpressionKind.ObjectCreation,
        args: [],
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'Error',
        },
        typeArguments: [],
      } as TSObjectCreationExpression,
    } as TSThrowStatement

    generator.generate(statement)
    expect(generator.extract()).toBe(new Printer().addLine('fatalError()').print())
  })
})
