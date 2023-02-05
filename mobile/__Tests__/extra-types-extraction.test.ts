import ts from 'typescript'
import {
  TSBuiltInType,
  TSLocalTypeKind,
  TSPrimitiveType,
  TSPrimitiveTypeName,
} from '../src/generators-model/basic-types'
import {
  TSCallExpression,
  TSExpressionExtraTypeHintKind,
  TSExpressionKind,
  TSIdentifier,
  TSMemberAccessExpression,
} from '../src/generators-model/expression'
import { TSFunctionArgumentType } from '../src/generators-model/function-argument'
import { TSGlobalTypeKind } from '../src/generators-model/global-types'
import { extractExpression } from '../src/parsing-helpers/expression-extractors'
import {
  arr_,
  bool_,
  function_,
  gen_,
  getExpressionsWithTypechecker,
  int32_,
  int64_,
  lit_,
  ref_,
  refArg,
  str_,
} from './__helpers__/test-helpers'

function id_(name: string): TSIdentifier {
  return {
    kind: TSExpressionKind.Identifier,
    name,
  } as TSIdentifier
}

describe('Expressions resolution with extra types', () => {
  let expressions: readonly ts.Expression[]
  let typechecker: ts.TypeChecker
  beforeAll(() => {
    ;[expressions, typechecker] = getExpressionsWithTypechecker(
      './__tests__/__helpers__/files/method-call-extra-type.ts',
    )
  })
  it('should extract "Number" extra type when Number static methods called', () => {
    const result = extractExpression(expressions[0], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('Number'),
        member: id_('parseInt'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Number,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_('Hello')],
      typeArguments: [],
      fnDeclarationHint: {
        args: [
          {
            kind: TSFunctionArgumentType.Normal,
            name: 'string',
            type: {
              kind: TSLocalTypeKind.Primitive,
              name: TSPrimitiveTypeName.String,
            } as TSPrimitiveType,
          },
          {
            kind: TSFunctionArgumentType.Normal,
            name: 'radix',
            type: {
              kind: TSLocalTypeKind.Primitive,
              name: TSPrimitiveTypeName.Int32,
            } as TSPrimitiveType,
          },
        ],
        body: undefined,
        generics: [],
        isExport: false,
        kind: TSGlobalTypeKind.Function,
        name: 'parseInt',
        returnType: {
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Int32,
        },
      },
      nullableTypeHint: undefined,
      returnTypeHint: int32_(),
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Number" extra type when Number instance methods called', () => {
    const result = extractExpression(expressions[1], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('n'),
        member: id_('toFixed'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Number,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Boolean" extra type when Boolean methods called', () => {
    const result = extractExpression(expressions[2], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: lit_(true),
        member: id_('valueOf'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Boolean,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })

  it('should extract "String" extra type when String methods called', () => {
    const result = extractExpression(expressions[3], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: lit_('Hello'),
        member: id_('trim'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.String,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Array" extra type when Array methods called', () => {
    const result = extractExpression(expressions[4], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: lit_([lit_(1)]),
        member: id_('indexOf'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Array,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_(1)],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Map" extra type when Map methods called', () => {
    const result = extractExpression(expressions[5], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('m'),
        member: id_('has'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Map,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_('hello')],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Set" extra type when Set methods called', () => {
    const result = extractExpression(expressions[6], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('s'),
        member: id_('has'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Set,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_('hello')],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Date" extra type when Date methods called', () => {
    const result = extractExpression(expressions[7], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('d'),
        member: id_('getDay'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Date,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "Math" extra type when Math methods called', () => {
    const result = extractExpression(expressions[8], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('Math'),
        member: id_('abs'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.Math,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_(1)],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract class extra type when class methods called', () => {
    const result = extractExpression(expressions[9], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('c'),
        member: id_('method'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.Reference,
          type: ref_('MyClass'),
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: {
        args: [],
        body: undefined,
        generics: [],
        isExport: false,
        kind: TSGlobalTypeKind.Function,
        name: 'method',
        returnType: {
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Int32,
        },
      },
      nullableTypeHint: undefined,
      returnTypeHint: int32_(),
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract interface extra type when interface methods called', () => {
    const result = extractExpression(expressions[10], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('i'),
        member: id_('method'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.Reference,
          type: ref_('MyInterface'),
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: {
        args: [],
        body: undefined,
        generics: [],
        isExport: false,
        kind: TSGlobalTypeKind.Function,
        name: 'method',
        returnType: {
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Int32,
        },
      },
      nullableTypeHint: undefined,
      returnTypeHint: int32_(),
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract enum extra type when enum member accessed', () => {
    const result = extractExpression(expressions[11], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.MemberAccess,
      object: id_('MyEnum'),
      member: id_('myValue'),
      nullableTypeHint: undefined,
      objectTypeHint: {
        kind: TSExpressionExtraTypeHintKind.Reference,
        type: ref_('MyEnum'),
      },
      optionalChaining: false,
    } as TSMemberAccessExpression)
  })
  it('should extract "ReadonlyArray" extra type when ReadonlyArray methods called', () => {
    const result = extractExpression(expressions[12], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('ra'),
        member: id_('indexOf'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.ReadonlyArray,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_('hello')],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "ReadonlyMap" extra type when ReadonlyMap methods called', () => {
    const result = extractExpression(expressions[13], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('rm'),
        member: id_('has'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.ReadonlyMap,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_('hello')],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "ReadonlySet" extra type when ReadonlySet methods called', () => {
    const result = extractExpression(expressions[14], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('rs'),
        member: id_('has'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.ReadonlySet,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [lit_('hello')],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should support parenthesized types, like "readonly (readonly *[])[]"', () => {
    const result = extractExpression(expressions[15], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: id_('f'),
      args: [],
      typeArguments: [],
      fnDeclarationHint: function_('f', [], arr_(arr_(str_(), true), true), [], false),
      nullableTypeHint: undefined,
      returnTypeHint: arr_(arr_(str_(), true), true),
      optionalChaining: false,
    } as TSCallExpression)
  })

  describe('literal types', () => {
    it('should support boolean literal type', () => {
      const result = extractExpression(expressions[16], typechecker)
      expect(result).toStrictEqual({
        kind: TSExpressionKind.Call,
        expression: id_('identity'),
        args: [lit_(true)],
        typeArguments: [],
        fnDeclarationHint: function_('identity', [refArg('value', 'T')], ref_('T'), [gen_('T')], false),
        nullableTypeHint: undefined,
        returnTypeHint: bool_(),
        optionalChaining: false,
      } as TSCallExpression)
    })
    it('should support numeric literal type', () => {
      const result = extractExpression(expressions[17], typechecker)
      expect(result).toStrictEqual({
        kind: TSExpressionKind.Call,
        expression: id_('identity'),
        args: [lit_(1)],
        typeArguments: [],
        fnDeclarationHint: function_('identity', [refArg('value', 'T')], ref_('T'), [gen_('T')], false),
        nullableTypeHint: undefined,
        returnTypeHint: int32_(),
        optionalChaining: false,
      } as TSCallExpression)
    })
    it('should support string literal type', () => {
      const result = extractExpression(expressions[18], typechecker)
      expect(result).toStrictEqual({
        kind: TSExpressionKind.Call,
        expression: id_('identity'),
        args: [lit_('foo')],
        typeArguments: [],
        fnDeclarationHint: function_('identity', [refArg('value', 'T')], ref_('T'), [gen_('T')], false),
        nullableTypeHint: undefined,
        returnTypeHint: str_(),
        optionalChaining: false,
      } as TSCallExpression)
    })
  })
  it('should extract "BigInt" extra type when BigInt instance methods called', () => {
    const result = extractExpression(expressions[19], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('bi'),
        member: id_('toString'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.BigInt,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [],
      typeArguments: [],
      fnDeclarationHint: undefined,
      nullableTypeHint: undefined,
      returnTypeHint: undefined,
      optionalChaining: false,
    } as TSCallExpression)
  })
  it('should extract "BigInt" extra type when BigInt static methods called', () => {
    const result = extractExpression(expressions[20], typechecker)
    expect(result).toStrictEqual({
      kind: TSExpressionKind.Call,
      expression: {
        kind: TSExpressionKind.MemberAccess,
        object: id_('BigInt'),
        member: id_('asIntN'),
        objectTypeHint: {
          kind: TSExpressionExtraTypeHintKind.BuiltIn,
          type: TSBuiltInType.BigInt,
        },
        nullableTypeHint: undefined,
        optionalChaining: false,
      } as TSMemberAccessExpression,
      args: [
        lit_(24),
        {
          kind: TSExpressionKind.Call,
          expression: id_('BigInt'),
          args: [lit_(10)],
          typeArguments: [],
          fnDeclarationHint: undefined,
          nullableTypeHint: undefined,
          returnTypeHint: int64_(),
          optionalChaining: false,
        } as TSCallExpression,
      ],
      typeArguments: [],
      fnDeclarationHint: {
        args: [
          {
            kind: TSFunctionArgumentType.Normal,
            name: 'bits',
            type: {
              kind: TSLocalTypeKind.Primitive,
              name: TSPrimitiveTypeName.Int32,
            } as TSPrimitiveType,
          },
          {
            kind: TSFunctionArgumentType.Normal,
            name: 'int',
            type: {
              kind: TSLocalTypeKind.Primitive,
              name: TSPrimitiveTypeName.Int64,
            } as TSPrimitiveType,
          },
        ],
        body: undefined,
        generics: [],
        isExport: false,
        kind: TSGlobalTypeKind.Function,
        name: 'asIntN',
        returnType: {
          kind: TSLocalTypeKind.Primitive,
          name: TSPrimitiveTypeName.Int64,
        },
      },
      nullableTypeHint: undefined,
      returnTypeHint: int64_(),
      optionalChaining: false,
    } as TSCallExpression)
  })
})
