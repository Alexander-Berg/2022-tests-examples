import ts from 'typescript'
import { TSBuiltInType, UndefinedToNullFunctionName } from '../src/generators-model/basic-types'
import {
  TSCallExpression,
  TSExpressionExtraTypeHint,
  TSExpressionExtraTypeHintKind,
  TSExpressionKind,
  TSIdentifier,
  TSLiteralExpression,
  TSMemberAccessExpression,
} from '../src/generators-model/expression'
import { TSFunction, TSGlobalTypeKind } from '../src/generators-model/global-types'
import { extractExpression } from '../src/parsing-helpers/expression-extractors'
import { makeSourceFileWithTypechecker, ref_ } from './__helpers__/test-helpers'

describe('Function type declaration at call site extraction', () => {
  let source: ts.SourceFile
  let typechecker: ts.TypeChecker
  beforeAll(() => {
    ;[source, typechecker] = makeSourceFileWithTypechecker([
      './__tests__/__helpers__/files/fn-declaration-in-call-expression.ts',
    ])
  })
  it('should find function declaration for call expression, not belonging to excluded entities', () => {
    const statement = source.statements[5] as ts.ExpressionStatement // foo()
    const expression = statement.expression as ts.CallExpression
    const extractedExpression = extractExpression(expression, typechecker) as TSCallExpression
    expect(extractedExpression).toStrictEqual({
      kind: TSExpressionKind.Call,
      args: [],
      expression: {
        kind: TSExpressionKind.Identifier,
        name: 'foo',
      } as TSIdentifier,
      fnDeclarationHint: {
        kind: TSGlobalTypeKind.Function,
        name: 'f',
        isExport: false,
        returnType: ref_('A'),
        args: [],
        generics: [],
        body: undefined,
      } as TSFunction,
      nullableTypeHint: undefined,
      returnTypeHint: ref_('A'),
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should omit function declaration extraction for entities belonging to Built-in types', () => {
    const statement = source.statements[6] as ts.ExpressionStatement // m.get(...)
    const expression = statement.expression as ts.CallExpression
    const extractedExpression = extractExpression(expression, typechecker) as TSCallExpression
    expect(extractedExpression).toStrictEqual({
      kind: TSExpressionKind.Call,
      args: [
        {
          kind: TSExpressionKind.Literal,
          value: 'hello',
        } as TSLiteralExpression,
      ],
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: {
          kind: TSExpressionKind.Identifier,
          name: 'm',
        } as TSIdentifier,
        member: {
          kind: TSExpressionKind.Identifier,
          name: 'get',
        },
        nullableTypeHint: undefined,
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Map,
        } as TSExpressionExtraTypeHint,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should omit function declaration extraction for entities belonging to excluded functions', () => {
    const statement = source.statements[7] as ts.ExpressionStatement // undefinedToNull(m.get(...))
    const expression = statement.expression as ts.CallExpression
    const extractedExpression = extractExpression(expression, typechecker) as TSCallExpression
    expect(extractedExpression).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.Identifier,
        name: UndefinedToNullFunctionName,
      } as TSIdentifier,
      args: [
        {
          kind: TSExpressionKind.Call,
          args: [
            {
              kind: TSExpressionKind.Literal,
              value: 'hello',
            } as TSLiteralExpression,
          ],
          expression: {
            kind: TSExpressionKind.MemberAccess,
            object: {
              kind: TSExpressionKind.Identifier,
              name: 'm',
            } as TSIdentifier,
            member: {
              kind: TSExpressionKind.Identifier,
              name: 'get',
            },
            nullableTypeHint: undefined,
            objectTypeHint: {
              kind: TSExpressionExtraTypeHintKind.BuiltIn,
              type: TSBuiltInType.Map,
            } as TSExpressionExtraTypeHint,
            optionalChaining: false,
          } as TSMemberAccessExpression,
          fnDeclarationHint: undefined,
          nullableTypeHint: undefined,
          returnTypeHint: undefined,
          typeArguments: [],
          optionalChaining: false,
        } as TSCallExpression,
      ],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      typeArguments: [],
      optionalChaining: false,
    } as TSCallExpression)
  })
})
