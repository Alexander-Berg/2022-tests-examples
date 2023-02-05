import { makeNullable, TSLocalType } from '../../../src/generators-model/basic-types'
import {
  TSBinaryExpression,
  TSBinaryOperator,
  TSCallExpression,
  TSExpression,
  TSExpressionKind,
  TSIdentifier,
  TSLiteralExpression,
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
import { KotlinExpressionGenerator } from '../../../src/generators/kotlin/expression-generator'
import { KotlinTypeMapper } from '../../../src/generators/kotlin/kotlin-type-mapper'
import { KotlinStatementGenerator } from '../../../src/generators/kotlin/statement-generator'
import Printer from '../../../src/utils/printer'
import { double_, int32_, int64_, RESERVED_KEYWORD__BREAK__, str_ } from '../../__helpers__/test-helpers'

describe('KotlinStatementsGenerator', () => {
  let printer: Printer
  let generator: KotlinStatementGenerator
  beforeEach(() => {
    printer = new Printer()
    const expressionGenerator = new KotlinExpressionGenerator((mapping) => KotlinTypeMapper.getTypeMapping(mapping), {
      generate(): string {
        return '__PRFX__11111'
      },
    })
    generator = new KotlinStatementGenerator(printer, expressionGenerator)
  })

  // EXPRESSION STATEMENT
  it('should generate expression statements', () => {
    const statements: TSExpressionStatement = {
      kind: TSStatementKind.ExpressionStatement,
      expression: {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'k',
        },
        args: [
          {
            kind: TSExpressionKind.Literal,
            value: 42,
          } as TSLiteralExpression,
        ],
        typeArguments: [],
        optionalChaining: false,
      } as TSCallExpression,
    }
    generator.generate(statements)
    expect(generator.extract()).toBe(new Printer().addLine('k(42)').print())
  })

  // BLOCK
  it('should generate block statements', () => {
    const expressionStatement: TSExpressionStatement = {
      kind: TSStatementKind.ExpressionStatement,
      expression: {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'l',
        } as TSIdentifier,
        args: [
          {
            kind: TSExpressionKind.Literal,
            value: 12,
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
          name: 'm',
        },
      } as TSUnaryExpression,
    }

    const statements: TSBlockStatement = {
      kind: TSStatementKind.Block,
      statements: [expressionStatement, returnStatement],
    }
    generator.generate(statements)
    const expected = new Printer().addLine('l(12)').addLine('return !m').print()
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
    const expected = new Printer().beginScope('while (a > 10)').addLine('f?.invoke(10)').endScope().print()
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
    const expected = new Printer().beginScope('do').addLine('f(10)').unindent().addLine('} while a > 10').print()
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
    const expected = new Printer().beginScope('if (a > 10)').addLine('f(10)').endScope().print()
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
      .beginScope('if (a > 10)')
      .addLine('f(10)')
      .unindent()
      .add('}')
      .beginScope(' else')
      .addLine('g?.invoke(20, z)')
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
    const expected = new Printer().beginScope('if (a > 10)').unindent().add('}').beginScope(' else').endScope().print()
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

    function call(f: string, value: number, name: string, optionalChaining: boolean): TSExpressionStatement {
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
          optionalChaining,
        } as TSCallExpression,
      }
    }

    const sample: TSIfElseStatement = {
      kind: TSStatementKind.IfElse,
      condition: condition('a', '>', 10),
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [call('f', 10, 'alpha', true)],
      } as TSBlockStatement,
      elseBlock: {
        kind: TSStatementKind.IfElse,
        condition: condition('b', '<', 40),
        thenBlock: {
          kind: TSStatementKind.Block,
          statements: [call('g', 20, 'beta', false)],
        },
        elseBlock: {
          kind: TSStatementKind.Block,
          statements: [call('h', 100, 'gamma', true)],
        },
      } as TSIfElseStatement,
    }
    generator.generate(sample)
    const expected = new Printer()
      .beginScope('if (a > 10)')
      .addLine('f?.invoke(10, alpha)')
      .unindent()
      .add('}')
      .beginScope(' else if (b < 40)')
      .addLine('g(20, beta)')
      .unindent()
      .beginScope('} else')
      .addLine('h?.invoke(100, gamma)')
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
      .beginScope('when (a)')
      .beginScope('"A" ->')
      .addLine('f(10, alpha)')
      .addLine('g(20, beta)')
      .endScope()
      .beginScope('"B" ->')
      .endScope()
      .beginScope('"C" ->')
      .endScope()
      .beginScope('"D" ->')
      .addLine('return h(30, gamma)')
      .endScope()
      .beginScope('else ->')
      .addLine('k(40, delta)')
      .addLine('l(50, epsilon)')
      .endScope()
      .endScope()
      .print()
    expect(generator.extract()).toBe(expected)
  })

  it('should generate switch statement with break in the middle of case', () => {
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
        caseClause('A', [call('f', 10, 'alpha'), { kind: TSStatementKind.Break }, call('g', 20, 'beta')], true),
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
      .beginScope('when (a)')
      .beginScope('"A" ->')
      .addLine('f(10, alpha)')
      .addLine('break')
      .addLine('g(20, beta)')
      .endScope()
      .beginScope('"B" ->')
      .endScope()
      .beginScope('"C" ->')
      .endScope()
      .beginScope('"D" ->')
      .addLine('return h(30, gamma)')
      .endScope()
      .beginScope('else ->')
      .addLine('k(40, delta)')
      .addLine('l(50, epsilon)')
      .endScope()
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

    function call(f: string, value: number, name: string, optionalChaining: boolean): TSExpressionStatement {
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
          optionalChaining,
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
        caseClause('A', [call('f', 10, 'alpha', true), call('g', 20, 'beta', false)], true),
        caseClause('B', [], true),
        caseClause('C', [], false),
        caseClause(
          'D',
          [
            {
              kind: TSStatementKind.Return,
              expression: call('h', 30, 'gamma', true).expression,
            } as TSReturnStatement,
          ],
          false,
        ),
      ],
    }
    generator.generate(sample)
    const expected = new Printer()
      .beginScope('when (a)')
      .addLine('"A" -> {')
      .indent()
      .addLine('f?.invoke(10, alpha)')
      .addLine('g(20, beta)')
      .unindent()
      .addLine('}')
      .addLine('"B" -> {')
      .indent()
      .unindent()
      .addLine('}')
      .addLine('"C" -> {')
      .indent()
      .unindent()
      .addLine('}')
      .addLine('"D" -> {')
      .indent()
      .addLine('return h?.invoke(30, gamma)')
      .unindent()
      .addLine('}')
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
    }).toThrowError('Either type, or initializer, or both must be present in variable declaration')
    expect(() => {
      generator.generate(sampleImmutable)
    }).toThrowError('Either type, or initializer, or both must be present in variable declaration')
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
    expect(generator.extract()).toBe(new Printer().addLine('var a: Int').print())

    generator.generate(declaration('b', false, makeNullable(str_())))
    expect(generator.extract()).toBe(new Printer().addLine('val b: String?').print())

    generator.generate(declaration(RESERVED_KEYWORD__BREAK__, true, int32_()))
    expect(generator.extract()).toBe(new Printer().addLine(`var \`${RESERVED_KEYWORD__BREAK__}\`: Int`).print())
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
    expect(generator.extract()).toBe(new Printer().addLine('val a = f(10)').print())
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
      new Printer().beginScope('for (var i: Double in mutableListOf(1, 2, 3))').addLine('continue').endScope().print(),
    )
    generator.generate(forIn('i', false, int32_()))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for (i: Int in mutableListOf(1, 2, 3))').addLine('continue').endScope().print(),
    )
    generator.generate(forIn('i', true))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for (var i in mutableListOf(1, 2, 3))').addLine('continue').endScope().print(),
    )
    generator.generate(forIn('i', false))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for (i in mutableListOf(1, 2, 3))').addLine('continue').endScope().print(),
    )

    // Reserved keywords
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, true, double_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for (var \`${RESERVED_KEYWORD__BREAK__}\`: Double in mutableListOf(1, 2, 3))`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, false, int32_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for (\`${RESERVED_KEYWORD__BREAK__}\`: Int in mutableListOf(1, 2, 3))`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, true))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for (var \`${RESERVED_KEYWORD__BREAK__}\` in mutableListOf(1, 2, 3))`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forIn(RESERVED_KEYWORD__BREAK__, false))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for (\`${RESERVED_KEYWORD__BREAK__}\` in mutableListOf(1, 2, 3))`)
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
      new Printer().beginScope('for (var i in (a + 10 until b + 20 step 1))').addLine('continue').endScope().print(),
    )
    generator.generate(forRange('i', false, false, expr('a', 10), expr('b', 20), int32_()))
    expect(generator.extract()).toBe(
      new Printer().beginScope('for (i: Int in (a + 10 downTo b + 20 step 1))').addLine('continue').endScope().print(),
    )

    // Reserved keywords
    generator.generate(forRange(RESERVED_KEYWORD__BREAK__, true, true, expr('a', 10), expr('b', 20)))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for (var \`${RESERVED_KEYWORD__BREAK__}\` in (a + 10 until b + 20 step 1))`)
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forRange(RESERVED_KEYWORD__BREAK__, false, false, expr('a', 10), expr('b', 20), int32_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope(`for (\`${RESERVED_KEYWORD__BREAK__}\`: Int in (a + 10 downTo b + 20 step 1))`)
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
        .beginScope('for (var i in (a + 10 until b + 20 step c + 30))')
        .addLine('continue')
        .endScope()
        .print(),
    )
    generator.generate(forRange(false, false, expr('a', 10), expr('b', 20), expr('c', 30), int64_()))
    expect(generator.extract()).toBe(
      new Printer()
        .beginScope('for (i: Long in (a + 10 downTo b + 20 step c + 30))')
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
  it('should generate throw statement with expression', () => {
    const statement = {
      kind: TSStatementKind.Throw,
      expression: {
        kind: TSExpressionKind.ObjectCreation,
        args: [],
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'MyError',
        },
        typeArguments: [],
      } as TSObjectCreationExpression,
    } as TSThrowStatement

    generator.generate(statement)
    expect(generator.extract()).toBe(new Printer().addLine('throw MyError()').print())
  })
})
