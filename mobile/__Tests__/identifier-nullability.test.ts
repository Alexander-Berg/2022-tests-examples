import ts from 'typescript'
import { YandexScriptCompilerOptions } from '../src/compiler-options'
import { TSBuiltInType } from '../src/generators-model/basic-types'
import {
  HasNullableTypeHint,
  TSCallExpression,
  TSExpressionExtraTypeHintKind,
  TSExpressionKind,
  TSMemberAccessExpression,
} from '../src/generators-model/expression'
import { TSFunctionArgumentType } from '../src/generators-model/function-argument'
import { TSBlockStatement, TSStatementKind, TSVariableDeclarationStatement } from '../src/generators-model/statement'
import { extractExpression } from '../src/parsing-helpers/expression-extractors'
import {
  buildEnvironmentDependentFilePath,
  getExpressionsWithTypechecker,
  int32_,
  ref_,
  str_,
} from './__helpers__/test-helpers'

describe('Identifier nullability extraction', () => {
  let expressions: readonly ts.Expression[]
  let typechecker: ts.TypeChecker
  const fileName = './__tests__/__helpers__/files/identifier-nullability.ts'
  beforeAll(() => {
    ;[expressions, typechecker] = getExpressionsWithTypechecker(fileName)
  })
  it('should extract Nullability extra from explicitly typed Nullable Identifier', () => {
    const result = extractExpression(expressions[0], typechecker)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'typedN',
          nullableTypeHint: int32_(),
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'toString',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Number,
        },
        optionalChaining: true,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract Nullability extra from implicitly typed Nullable Identifier', () => {
    const result = extractExpression(expressions[1], typechecker)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'untypedN',
          nullableTypeHint: int32_(),
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'toFixed',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Number,
        },
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract Nullability extra from explicitly typed NonNullable Identifier', () => {
    const result = extractExpression(expressions[2], typechecker)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'typedUN',
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'toString',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Number,
        },
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract Nullability extra from implicitly typed NonNullable Identifier', () => {
    const result = extractExpression(expressions[3], typechecker)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'untypedUN',
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'toFixed',
        },
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract Nullability extra from Call expression', () => {
    const result = extractExpression(expressions[4], typechecker)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'getNull',
      },
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 3,
        },
      ],
      typeArguments: [],
      nullableTypeHint: int32_(),
      optionalChaining: false,
    } as TSCallExpression | HasNullableTypeHint)
  })
  it('should extract Nullability extra from Member Access expression', () => {
    const result = extractExpression(expressions[5], typechecker)
    expect(result).toMatchObject({
      kind: TSExpressionKind.MemberAccess,
      object: {
        kind: TSExpressionKind.Identifier,
        name: 'A',
      },
      member: {
        kind: TSExpressionKind.Identifier,
        name: 'b',
      },
      nullableTypeHint: str_(),
      optionalChaining: false,
    } as TSMemberAccessExpression | HasNullableTypeHint)
  })
  it('should extract Nullability extra from call expression', () => {
    const result = extractExpression(expressions[6], typechecker)

    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'f',
      },
      args: [
        {
          kind: TSExpressionKind.ArrowFunction,
          parameters: [
            {
              kind: TSFunctionArgumentType.Arrow,
              name: 'r',
            },
          ],
          mustWeakify: false,
          body: {
            kind: TSStatementKind.Block,
            statements: [
              {
                kind: TSStatementKind.VariableDeclaration,
                mutable: false,
                name: 'n',
                initializer: {
                  kind: TSExpressionKind.Call,
                  expression: {
                    kind: TSExpressionKind.MemberAccess,
                    object: {
                      kind: TSExpressionKind.Call,
                      expression: {
                        kind: TSExpressionKind.MemberAccess,
                        object: {
                          kind: TSExpressionKind.Identifier,
                          name: 'r',
                        },
                        member: {
                          kind: TSExpressionKind.Identifier,
                          name: 'getError',
                        },
                        optionalChaining: false,
                      } as TSMemberAccessExpression,
                      args: [],
                      typeArguments: [],
                      optionalChaining: false,
                    } as TSCallExpression,
                    member: {
                      kind: TSExpressionKind.Identifier,
                      name: 'getInner',
                    },
                    optionalChaining: false,
                  } as TSMemberAccessExpression,
                  args: [],
                  typeArguments: [],
                  nullableTypeHint: ref_('MyError'),
                  optionalChaining: false,
                } as TSCallExpression,
              } as TSVariableDeclarationStatement,
            ],
          } as TSBlockStatement,
        },
      ],
      optionalChaining: false,
    } as TSCallExpression | HasNullableTypeHint)
  })
  it('should extract Nullability extra from under if (... !== null) ', () => {
    const realFileName = buildEnvironmentDependentFilePath(fileName)
    const program = ts.createProgram([realFileName], YandexScriptCompilerOptions)
    const tc = program.getTypeChecker()
    let protectedExpression: ts.Expression
    for (const statement of program.getSourceFile(realFileName)!.statements) {
      if (ts.isIfStatement(statement) && ts.isBlock(statement.thenStatement)) {
        const expressionStatement = statement.thenStatement.statements[0]
        if (ts.isExpressionStatement(expressionStatement)) {
          protectedExpression = expressionStatement.expression
        }
      }
    }

    const result = extractExpression(protectedExpression!, tc)
    expect(result).toMatchObject({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'typedN',
          nullableTypeHint: int32_(),
        },
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'toString',
        },
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Number,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      nullableTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
})
