import FileProcessor from '../src/fileprocessor'
import { TSVisibility } from '../src/generators-model/basic-types'
import {
  TSCallExpression,
  TSExpressionKind,
  TSIdentifier,
  TSLiteralExpression,
  TSSuperExpression,
} from '../src/generators-model/expression'
import { TSExpressionStatement, TSReturnStatement, TSStatementKind } from '../src/generators-model/statement'
import { TestGenerator } from './__helpers__/generator'
import {
  arr_,
  block_,
  bool_,
  constructor_,
  ctorArg,
  field_,
  field_extra_,
  fun_,
  int32_,
  m_,
  makeSourceFileWithTypechecker,
  method_,
  null_,
  ref_,
  str_,
  void_,
} from './__helpers__/test-helpers'

describe('Automatic overrides and implements', () => {
  // tslint:disable-next-line: max-line-length
  it('should discover overrides and implements if there are methods/properties with the same names in inheritance chain', () => {
    const [sourceFile, typechecker] = makeSourceFileWithTypechecker([
      './__tests__/__helpers__/files/overrides-implements.ts',
      './__tests__/__helpers__/files/overrides-implements-base.ts',
    ])
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '', typechecker)
    processor.process(sourceFile)
    const classA = generator.classes[0]
    expect(classA).not.toBeNull()
    expect(classA.name).toBe('A')
    expect(classA.fields).toHaveLength(7)
    expect(classA.fields[0]).toStrictEqual(
      field_('iProp1', str_(), TSVisibility.Public, field_extra_(true, false, false, false, true)),
    )
    expect(classA.fields[1]).toStrictEqual(
      field_('iProp2', str_(), TSVisibility.Public, field_extra_(false, false, false, false, true)),
    )
    expect(classA.fields[2]).toStrictEqual(
      field_('preProp1', str_(), TSVisibility.Public, field_extra_(true, false, false, true, false)),
    )
    expect(classA.fields[3]).toStrictEqual(
      field_('preProp2', str_(), TSVisibility.Public, field_extra_(false, false, false, true, false)),
    )
    expect(classA.fields[4]).toStrictEqual(
      field_('prop1', str_(), TSVisibility.Public, field_extra_(true, false, false, true, false)),
    )
    expect(classA.fields[5]).toStrictEqual(
      field_('prop2', str_(), TSVisibility.Public, field_extra_(false, false, false, true, false)),
    )
    expect(classA.fields[6]).toStrictEqual(
      field_('a', int32_(), TSVisibility.Public, field_extra_(true, false, false, false, false), {
        kind: TSExpressionKind.Literal,
        value: 10,
      } as TSLiteralExpression),
    )

    expect(classA.methods).toHaveLength(4)
    expect(classA.methods[0]).toStrictEqual(
      method_(
        'iMethod',
        TSVisibility.Public,
        [],
        str_(),
        m_(false, false, false, true),
        [],
        block_([
          {
            kind: TSStatementKind.Return,
            expression: {
              kind: TSExpressionKind.Literal,
              value: 'hello',
            } as TSLiteralExpression,
          } as TSReturnStatement,
        ]),
      ),
    )
    expect(classA.methods[1]).toStrictEqual(
      method_(
        'preMethod',
        TSVisibility.Public,
        [],
        int32_(),
        m_(false, false, true, false),
        [],
        block_([
          {
            kind: TSStatementKind.Return,
            expression: {
              kind: TSExpressionKind.Literal,
              value: 10,
            } as TSLiteralExpression,
          } as TSReturnStatement,
        ]),
      ),
    )
    expect(classA.methods[2]).toStrictEqual(
      method_(
        'method',
        TSVisibility.Public,
        [],
        null_(str_()),
        m_(false, false, true, false),
        [],
        block_([
          {
            kind: TSStatementKind.Return,
            expression: {
              kind: TSExpressionKind.Literal,
              value: null,
            } as TSLiteralExpression,
          } as TSReturnStatement,
        ]),
      ),
    )
    expect(classA.methods[3]).toStrictEqual(
      method_(
        'm',
        TSVisibility.Public,
        [],
        bool_(),
        m_(false, false, false, false),
        [],
        block_([
          {
            kind: TSStatementKind.Return,
            expression: {
              kind: TSExpressionKind.Literal,
              value: true,
            } as TSLiteralExpression,
          } as TSReturnStatement,
        ]),
      ),
    )

    expect(classA.ctor).not.toBeNull()
    expect(classA.ctor).toStrictEqual(
      constructor_(
        [
          ctorArg('cp', int32_(), true, true, false, TSVisibility.Public),
          ctorArg('cp1', bool_(), true, false, true, TSVisibility.Public),
        ],
        TSVisibility.Public,
        false,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super },
              fnDeclarationHint: undefined,
              args: [{ kind: TSExpressionKind.Identifier, name: 'cp' } as TSIdentifier],
              nullableTypeHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )
  })
  it('should automatically discover overridden constructor', () => {
    const [sourceFile, typechecker] = makeSourceFileWithTypechecker([
      './__tests__/__helpers__/files/overrides-implements.ts',
      './__tests__/__helpers__/files/overrides-implements-base.ts',
    ])
    const generator = new TestGenerator()
    const processor = new FileProcessor<{}>(generator, '', '', typechecker)
    processor.process(sourceFile)
    const classB3 = generator.classes[1]
    expect(classB3.ctor).not.toBeNull()
    expect(classB3.ctor).toStrictEqual(
      constructor_(
        [
          ctorArg('a', int32_()),
          ctorArg('b', null_(arr_(ref_('R', [int32_()]), true))),
          ctorArg('c', arr_(str_(), true)),
          ctorArg('d', ref_('R', [int32_()])),
          ctorArg('e', fun_([ref_('R', [int32_()])], str_())),
        ],
        TSVisibility.Public,
        true,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super } as TSSuperExpression,
              fnDeclarationHint: undefined,
              args: [
                { kind: TSExpressionKind.Identifier, name: 'a' } as TSIdentifier,
                {
                  kind: TSExpressionKind.Identifier,
                  name: 'b',
                  nullableTypeHint: arr_(ref_('R', [int32_()]), true),
                } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'c' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'd' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'e' } as TSIdentifier,
              ],
              nullableTypeHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )
    const classB4 = generator.classes[2]
    expect(classB4.ctor).not.toBeNull()
    expect(classB4.ctor).toStrictEqual(
      constructor_(
        [
          ctorArg('a', int32_()),
          ctorArg('c', arr_(str_(), true)),
          ctorArg('d', ref_('R', [int32_()])),
          ctorArg('e', fun_([ref_('R', [int32_()])], str_())),
        ],
        TSVisibility.Public,
        false,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super } as TSSuperExpression,
              fnDeclarationHint: undefined,
              args: [
                { kind: TSExpressionKind.Identifier, name: 'a' } as TSIdentifier,
                { kind: TSExpressionKind.Literal, value: null } as TSLiteralExpression,
                { kind: TSExpressionKind.Identifier, name: 'c' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'd' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'e' } as TSIdentifier,
              ],
              nullableTypeHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )
    const classB5 = generator.classes[3]
    expect(classB5.ctor).not.toBeNull()
    expect(classB5.ctor).toStrictEqual(
      constructor_(
        [ctorArg('a', int32_())],
        TSVisibility.Public,
        false,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super } as TSSuperExpression,
              fnDeclarationHint: undefined,
              args: [],
              nullableTypeHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )
    const classB6 = generator.classes[4]
    expect(classB6.ctor).not.toBeNull()
    expect(classB6.ctor).toStrictEqual(
      constructor_(
        [
          ctorArg('a', int32_()),
          ctorArg('b', null_(arr_(ref_('R', [str_()]), true))),
          ctorArg('c', arr_(str_(), true)),
          ctorArg('d', ref_('R', [int32_()])),
          ctorArg('e', fun_([ref_('R', [int32_()])], str_())),
        ],
        TSVisibility.Public,
        false,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super } as TSSuperExpression,
              args: [
                { kind: TSExpressionKind.Identifier, name: 'a' } as TSIdentifier,
                {
                  kind: TSExpressionKind.Identifier,
                  name: 'b',
                  nullableTypeHint: arr_(ref_('R', [str_()]), true),
                } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'c' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'd' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'e' } as TSIdentifier,
              ],
              nullableTypeHint: undefined,
              fnDeclarationHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )
    // B7 covers a case of ultimate base class being an implementor of an interface only
    const classB7 = generator.classes[5]
    expect(classB7.ctor).not.toBeNull()
    expect(classB7.ctor).toStrictEqual(
      constructor_(
        [ctorArg('a', int32_())],
        TSVisibility.Public,
        true,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super } as TSSuperExpression,
              args: [{ kind: TSExpressionKind.Identifier, name: 'a' } as TSIdentifier],
              nullableTypeHint: undefined,
              fnDeclarationHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )

    // StringFlag covers a case of generic constructor overrides
    const classStringFlag = generator.classes[6]
    expect(classStringFlag.ctor).not.toBeNull()
    expect(classStringFlag.ctor).toStrictEqual(
      constructor_(
        [ctorArg('name', str_()), ctorArg('value', str_())],
        TSVisibility.Public,
        true,
        block_([
          {
            kind: TSStatementKind.ExpressionStatement,
            expression: {
              kind: TSExpressionKind.Call,
              expression: { kind: TSExpressionKind.Super } as TSSuperExpression,
              args: [
                { kind: TSExpressionKind.Identifier, name: 'name' } as TSIdentifier,
                { kind: TSExpressionKind.Identifier, name: 'value' } as TSIdentifier,
              ],
              nullableTypeHint: undefined,
              fnDeclarationHint: undefined,
              returnTypeHint: void_(),
              typeArguments: [],
              optionalChaining: false,
            } as TSCallExpression,
          } as TSExpressionStatement,
        ]),
      ),
    )
  })
  it('should prohibit parameters with default values in overriding methods', () => {
    const [sourceFile, typechecker] = makeSourceFileWithTypechecker(['./__tests__/__helpers__/files/default-args.ts'])
    const processor = new FileProcessor<{}>(new TestGenerator(), '', '', typechecker)
    expect(() => processor.process(sourceFile)).toThrowError(
      'Default values for parameters are not supported in overriding methods',
    )
  })
})
