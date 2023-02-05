import { makeNullable } from '../src/generators-model/basic-types'
import {
  TSBinaryExpression,
  TSExpressionKind,
  TSIdentifier,
  TSLiteralExpression,
  TSObjectCreationExpression,
} from '../src/generators-model/expression'
import { TSStatementKind, TSThrowStatement } from '../src/generators-model/statement'
import { extractStatement } from '../src/parsing-helpers/statement-extractors'
import { double_, getStatements, int32_, int64_, makeSourceFile } from './__helpers__/test-helpers'

describe(extractStatement, () => {
  // THROWS ON NOOP
  it('should extract block statements', () => {
    const sample = ';'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Unsupported type of statement/)
  })

  // EXPRESSION STATEMENT
  it('should extract expression statement', () => {
    const sample = '10'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ExpressionStatement,
      expression: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
    })
  })

  // BLOCK STATEMENT
  it('should extract empty block of statements', () => {
    const sample = '{}'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Block,
      statements: [],
    })
  })
  it('should extract non-empty block of statements', () => {
    const sample = `{
      const a = 10
    }`
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Block,
      statements: [
        {
          kind: TSStatementKind.VariableDeclaration,
          mutable: false,
          name: 'a',
          initializer: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
        },
      ],
    })
  })

  // VARIABLE DECLARATIONS STATEMENT (LET, CONST)
  it('should extract const declarations', () => {
    const sample = 'const a = 10'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.VariableDeclaration,
      mutable: false,
      name: 'a',
      initializer: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
    })
  })
  it('should extract let declarations with initializer', () => {
    const sample = "let a = '10'"
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.VariableDeclaration,
      mutable: true,
      name: 'a',
      initializer: {
        kind: TSExpressionKind.Literal,
        value: '10',
      },
    })
  })
  it('should extract let declarations without initializer but with type annotation', () => {
    const sample = 'let a: number;'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.VariableDeclaration,
      mutable: true,
      name: 'a',
      type: int32_(),
    })
  })
  it('should extract let declarations with both initializer and type annotation', () => {
    const sample = 'let a: Int32 = 10'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.VariableDeclaration,
      mutable: true,
      name: 'a',
      type: int32_(),
      initializer: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
    })
  })
  it('should extract let nullable declarations with both initializer and type annotation', () => {
    const sample = 'let a: Nullable<Nullable<Int64>> = null'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.VariableDeclaration,
      mutable: true,
      name: 'a',
      type: makeNullable(makeNullable(int64_())),
      initializer: {
        kind: TSExpressionKind.Literal,
        value: null,
      },
    })
  })
  it('should throw on const without initializer', () => {
    const sample = 'const a: Double'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Const variables must be initialized/)
  })
  it('should throw if var declarations', () => {
    const sample = "var a = '10'"
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only let and const variable declarations are supported/)
  })
  it('should throw if more than one declaration per line', () => {
    const sample = 'const a = 10, b = 20'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Multiple variables introduction in one declaration is not supported/)
  })
  it('should throw if deconstruction is used in declaration list', () => {
    const sample = 'const [a, b] = [10, 20]'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only identifiers are supported as declaration items/)
  })
  it('should throw if untyped and uninitialized variables are declared', () => {
    const sample = 'let a;'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Untyped variables are not supported/)
  })

  // IF STATEMENT
  it("should support 'if-else' statement", () => {
    const sample = 'if (a > 20) { } else { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
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
          value: 20,
        },
      },
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [],
      },
      elseBlock: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it("should support just 'if', no 'else', statement", () => {
    const sample = 'if (a > 20) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
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
          value: 20,
        },
      },
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it("should support 'if-else-if' statement", () => {
    const sample = 'if (a > 20) { } else if (a < 40) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
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
          value: 20,
        },
      },
      thenBlock: {
        kind: TSStatementKind.Block,
        statements: [],
      },
      elseBlock: {
        kind: TSStatementKind.IfElse,
        condition: {
          kind: TSExpressionKind.Binary,
          operator: '<',
          left: {
            kind: TSExpressionKind.Identifier,
            name: 'a',
          },
          right: {
            kind: TSExpressionKind.Literal,
            value: 40,
          },
        },
        thenBlock: {
          kind: TSStatementKind.Block,
          statements: [],
        },
      },
    })
  })
  it("should throw if 'then' part is non-block", () => {
    const sample = 'if (true) f()'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Then part must be a block/)
  })
  it("should throw if 'else' part is non-block and non-if", () => {
    const sample = 'if (true) {} else f()'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Else part, if present and not itself 'if', must be a block/)
  })
  it('should throw if there is non-block in thenPart', () => {
    const sample = 'if (true);'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Then part must be a block/)
  })
  it('should throw if there is non-block in elsePart', () => {
    const sample = 'if (true) {} else;'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Else part, if present and not itself 'if', must be a block/)
  })

  // RETURN STATEMENT
  it('should support return without value statement', () => {
    const sample = 'return;'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Return,
    })
  })
  it('should support return with value statement', () => {
    const sample = 'return a + 10'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Return,
      expression: {
        kind: TSExpressionKind.Binary,
        operator: '+',
        left: {
          kind: TSExpressionKind.Identifier,
          name: 'a',
        },
        right: {
          kind: TSExpressionKind.Literal,
          value: 10,
        },
      },
    })
  })

  // FOR-OF STATEMENT
  it('should support for-of with const as for-each', () => {
    const sample = 'for (const a of [1, 2]) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForEach,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        name: 'a',
      },
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
        ],
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support for-of with typed const as for-each', () => {
    const sample = 'for (const a: number of [1, 2]) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForEach,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        type: int32_(),
        name: 'a',
      },
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
        ],
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support for-of with let as for-each', () => {
    const sample = 'for (let a of [1, 2]) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForEach,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        name: 'a',
      },
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
        ],
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support for-of with typed let as for-each', () => {
    const sample = 'for (let a: Int64 of [1, 2]) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForEach,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        type: int64_(),
        name: 'a',
      },
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
        ],
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it("should throw if there's no variable declaration in for-of statement", () => {
    const sample = 'for (a of [1, 2]) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only variable declarations are supported in for-of loop/)
  })
  it("should throw if there's deconstruction in for-of statement", () => {
    const sample = 'for (const [a, b] of [1, 2]) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only identifiers are supported as declaration items/)
  })
  it("should throw if there's no block but a single statement", () => {
    const sample = 'for (const a of [1, 2]) console.log(a)'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in for-of loop/)
  })

  // FOR RANGE STATEMENT
  it('should support const for-of with range function', () => {
    const sample = 'for (const a of range(1, 10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed const for-of with range function', () => {
    const sample = 'for (const a: Double of range(1, 10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        type: double_(),
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support let for-of with range function', () => {
    const sample = 'for (let a of range(1, 10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed let for-of with range function', () => {
    const sample = 'for (let a: Int32 of range(1, 10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        type: int32_(),
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support const for-of with decrementalRange function', () => {
    const sample = 'for (const a of decrementalRange(10, 1, -1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: {
        kind: TSExpressionKind.Unary,
        operator: '-',
        operand: {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed const for-of with decrementalRange function', () => {
    const sample = 'for (const a: number of decrementalRange(10, 1, -1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        type: int32_(),
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: {
        kind: TSExpressionKind.Unary,
        operator: '-',
        operand: {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support let for-of with decrementalRange function', () => {
    const sample = 'for (let a of decrementalRange(10, 1, -1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: {
        kind: TSExpressionKind.Unary,
        operator: '-',
        operand: {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed let for-of with decrementalRange function', () => {
    const sample = 'for (let a: Int64 of decrementalRange(10, 1, -1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        type: int64_(),
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: {
        kind: TSExpressionKind.Unary,
        operator: '-',
        operand: {
          kind: TSExpressionKind.Literal,
          value: 1,
        },
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support const for-of with range omitting step function', () => {
    const sample = 'for (const a of range(1, 10)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed const for-of with range omitting step function', () => {
    const sample = 'for (const a: Double of range(1, 10)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        type: double_(),
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support let for-of with range omitting step function', () => {
    const sample = 'for (let a of range(1, 10)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed let for-of with range omitting step function', () => {
    const sample = 'for (let a: Int32 of range(1, 10)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        type: int32_(),
        name: 'a',
      },
      growing: true,
      from: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support const for-of with decrementalRange omitting step function', () => {
    const sample = 'for (const a of decrementalRange(10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed const for-of with decrementalRange omitting step function', () => {
    const sample = 'for (const a: Int64 of decrementalRange(10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: false,
        type: int64_(),
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support let for-of with decrementalRange omitting step function', () => {
    const sample = 'for (let a of decrementalRange(10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should support typed let for-of with decrementalRange omitting step function', () => {
    const sample = 'for (let a: Int32 of decrementalRange(10, 1)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.ForRange,
      initializer: {
        kind: TSStatementKind.VariableDeclaration,
        mutable: true,
        type: int32_(),
        name: 'a',
      },
      growing: false,
      from: {
        kind: TSExpressionKind.Literal,
        value: 10,
      },
      to: {
        kind: TSExpressionKind.Literal,
        value: 1,
      },
      step: undefined,
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it("should throw if there's no variable declaration in for-range statement", () => {
    const sample = 'for (a of range(1, 10)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only variable declarations are supported in for-of loop/)
  })
  it("should throw if there's deconstruction in for-of statement", () => {
    const sample = 'for (const [a, b] of range(1, 10, 2)) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only identifiers are supported as declaration items/)
  })
  it("should throw if there's no block but a single statement", () => {
    const sample = 'for (const a of decrementalRange(4, 1, -1)) console.log(a)'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in for-of loop/)
  })
  it('should throw on noop block in for-of', () => {
    const sample = 'for (const a of range(1, 3));'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in for-of loop/)
  })

  // BREAK STATEMENT
  it('should support break statement', () => {
    const sample = 'break;'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Break,
    })
  })

  // CONTINUE STATEMENT
  it('should support continue statement', () => {
    const sample = 'continue;'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Continue,
    })
  })

  // WHILE STATEMENT
  it('should support while statement', () => {
    const sample = 'while (a > 100) { }'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
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
          value: 100,
        },
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should throw on noop block', () => {
    const sample = 'while (a > 100);'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in while loop/)
  })
  it('should throw on non-block block', () => {
    const sample = 'while (a > 100) console.log(a)'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in while loop/)
  })

  // DO-WHILE STATEMENT
  it('should support do-while statement', () => {
    const sample = 'do { } while (a > 100)'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
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
          value: 100,
        },
      },
      block: {
        kind: TSStatementKind.Block,
        statements: [],
      },
    })
  })
  it('should throw on noop block', () => {
    const sample = 'do; while (a > 100)'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in do-while loop/)
  })
  it('should throw on non-block block', () => {
    const sample = 'do console.log(a) while (a > 100)'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Only blocks are allowed in do-while loop/)
  })

  // SWITCH STATEMENT
  it('should support switch statement with default', () => {
    const sample = `
    switch (f()) {
      case 'a': // fallthrough
      case 10:
        z1()
        b = 4
        break
      case 20:
        break
      default:
        b = 3
        break
    }
    `
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Switch,
      expression: {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        },
        args: [],
      },
      cases: [
        {
          expression: {
            kind: TSExpressionKind.Literal,
            value: 'a',
          },
          statements: [],
        },
        {
          expression: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
          statements: [
            {
              kind: TSStatementKind.ExpressionStatement,
              expression: {
                kind: TSExpressionKind.Call,
                expression: {
                  kind: TSExpressionKind.Identifier,
                  name: 'z1',
                },
                args: [],
              },
            },
            {
              kind: TSStatementKind.ExpressionStatement,
              expression: {
                kind: TSExpressionKind.Binary,
                operator: '=',
                left: {
                  kind: TSExpressionKind.Identifier,
                  name: 'b',
                },
                right: {
                  kind: TSExpressionKind.Literal,
                  value: 4,
                },
              },
            },
            {
              kind: TSStatementKind.Break,
            },
          ],
        },
        {
          expression: {
            kind: TSExpressionKind.Literal,
            value: 20,
          },
          statements: [
            {
              kind: TSStatementKind.Break,
            },
          ],
        },
      ],
      defaultCase: {
        statements: [
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Binary,
              operator: '=',
              left: {
                kind: TSExpressionKind.Identifier,
                name: 'b',
              },
              right: {
                kind: TSExpressionKind.Literal,
                value: 3,
              },
            },
          },
          {
            kind: TSStatementKind.Break,
          },
        ],
      },
    })
  })
  it('should support switch statement without default', () => {
    const sample = `
    switch (f()) {
      case 10:
        a = 10
        break
    }
    `
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toMatchObject({
      kind: TSStatementKind.Switch,
      expression: {
        kind: TSExpressionKind.Call,
        expression: {
          kind: TSExpressionKind.Identifier,
          name: 'f',
        },
        args: [],
      },
      cases: [
        {
          expression: {
            kind: TSExpressionKind.Literal,
            value: 10,
          },
          statements: [
            {
              kind: TSStatementKind.ExpressionStatement,
              expression: {
                kind: TSExpressionKind.Binary,
                operator: '=',
                left: {
                  kind: TSExpressionKind.Identifier,
                  name: 'a',
                },
                right: {
                  kind: TSExpressionKind.Literal,
                  value: 10,
                },
              },
            },
            {
              kind: TSStatementKind.Break,
            },
          ],
        },
      ],
      defaultCase: undefined,
    })
  })

  // THROW STATEMENT
  it('should support throw statement', () => {
    const sample = 'throw new Error("error " + a)'
    const statement = getStatements(makeSourceFile(sample))[0]
    const result = extractStatement(statement)
    expect(result).toStrictEqual({
      kind: TSStatementKind.Throw,
      expression: {
        kind: TSExpressionKind.ObjectCreation,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'Error',
        },
        args: [
          {
            kind: TSExpressionKind.Binary,
            operator: '+',
            left: {
              kind: TSExpressionKind.Literal,
              value: '"error "',
            } as TSLiteralExpression,
            right: {
              kind: TSExpressionKind.Identifier,
              name: 'a',
            } as TSIdentifier,
          } as TSBinaryExpression,
        ],
        typeArguments: [],
      } as TSObjectCreationExpression,
    } as TSThrowStatement)
  })
  it('should throw if no catch', () => {
    const sample = 'try {}'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/After try should be catch/)
  })
  it('should throw if no catch variable declaration', () => {
    const sample = 'try {} catch {}'
    const statement = getStatements(makeSourceFile(sample))[0]
    expect(() => {
      extractStatement(statement)
    }).toThrow(/Catch cause should have error variable declaration/)
  })
})
