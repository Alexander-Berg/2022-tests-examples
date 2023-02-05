import Path from 'path'
import ts from 'typescript'
import { YandexScriptCompilerOptions } from '../../src/compiler-options'
import {
  makeNullable,
  makeThrowing,
  TSArrayType,
  TSFunctionType,
  TSLocalType,
  TSLocalTypeKind,
  TSNullable,
  TSPrimitiveType,
  TSPrimitiveTypeName,
  TSReferenceType,
  TSThrowing,
  TSVisibility,
} from '../../src/generators-model/basic-types'
import { TSExpression, TSExpressionKind, TSLiteralExpression } from '../../src/generators-model/expression'
import {
  TSConstructorArgument,
  TSFunctionArgumentType,
  TSNormalFunctionArgument,
} from '../../src/generators-model/function-argument'
import {
  makeEnum,
  TSAlias,
  TSClass,
  TSConstructor,
  TSEnum,
  TSEnumMember,
  TSExtensionList,
  TSField,
  TSFieldExtra,
  TSFunction,
  TSGenericType,
  TSGlobalTypeKind,
  TSInterface,
  TSMethod,
  TSMethodExtra,
} from '../../src/generators-model/global-types'
import { TSBlockStatement, TSStatement, TSStatementKind } from '../../src/generators-model/statement'
import { error } from '../../src/parsing-helpers/error'
import { Writable } from '../../src/utils/writable'

export function strArg(name: string, defaultValue?: string): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: str_(),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue: lit_(defaultValue) }
}
export function int32Arg(name: string, defaultValue?: number): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: int32_(),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue: lit_(defaultValue) }
}
export function int64Arg(name: string, defaultValue?: number): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: int64_(),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue: lit_(defaultValue) }
}
export function doubleArg(name: string, defaultValue?: number): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: double_(),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue: lit_(defaultValue) }
}
export function boolArg(name: string, defaultValue?: boolean): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: bool_(),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue: lit_(defaultValue) }
}
export function anyArg(name: string): TSNormalFunctionArgument {
  return {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: any_(),
  }
}
export function refArg(name: string, type: string, defaultValue?: TSExpression): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: ref_(type),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue }
}
export function funArg(name: string, args: readonly TSLocalType[], returnType: TSLocalType): TSNormalFunctionArgument {
  return {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: fun_(args, returnType),
  }
}
export function nullArg({ name, type }: TSNormalFunctionArgument, defaultValue?: null): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: null_(type),
  }

  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue: lit_(defaultValue) }
}
export function arrArg(
  name: string,
  type: TSLocalType,
  isReadonly: boolean,
  defaultValue?: TSExpression,
): TSNormalFunctionArgument {
  const argument: TSNormalFunctionArgument = {
    kind: TSFunctionArgumentType.Normal,
    name,
    type: arr_(type, isReadonly),
  }
  if (defaultValue === undefined) {
    return argument
  }
  return { ...argument, defaultValue }
}
export function str_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.String,
  }
}
export function int32_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.Int32,
  }
}
export function int64_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.Int64,
  }
}
export function double_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.Double,
  }
}
export function bool_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.Boolean,
  }
}
export function any_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.Any,
  }
}
export function void_(): TSPrimitiveType {
  return {
    kind: TSLocalTypeKind.Primitive,
    name: TSPrimitiveTypeName.Void,
  }
}
export function ref_(name: string, typeParameters?: readonly TSLocalType[]): TSReferenceType {
  const result: TSReferenceType = {
    kind: TSLocalTypeKind.Reference,
    name,
  }
  return typeParameters ? { ...result, typeParameters } : result
}
export function refs_(...names: string[]): readonly TSReferenceType[] {
  return names.map(
    (name) =>
      ({
        kind: TSLocalTypeKind.Reference,
        name,
      } as TSReferenceType),
  )
}
export function gen_(name: string, constraints: readonly TSReferenceType[] = []): TSGenericType {
  return {
    name,
    constraints,
  }
}
export function fun_(args: readonly TSLocalType[], returnType: TSLocalType): TSFunctionType {
  return {
    kind: TSLocalTypeKind.Function,
    args,
    returnType,
  }
}
export function null_(type: TSLocalType): TSNullable {
  return makeNullable(type)
}
export function throwing_(type: TSLocalType): TSThrowing {
  return makeThrowing(type)
}
export function arr_(elementType: TSLocalType, isReadonly: boolean): TSArrayType {
  return {
    kind: TSLocalTypeKind.Array,
    isReadonly,
    elementType,
  }
}
export function enum_(name: string, isExport: boolean, ...values: string[]): TSEnum {
  return makeEnum(
    name,
    values.map((value) => ({
      name: value,
    })),
    isExport,
  )
}

export function field_extra_(
  isReadonly: boolean,
  isStatic: boolean,
  isWeak: boolean,
  isOverriding: boolean,
  isImplementing: boolean,
  serializedName?: string,
): TSFieldExtra {
  return {
    isReadonly,
    isStatic,
    isWeak,
    isOverriding,
    isImplementing,
    serializedName,
  }
}
export function m_(
  isAbstract: boolean,
  isStatic: boolean,
  isOverriding: boolean,
  isImplementing: boolean,
): TSMethodExtra {
  return {
    isAbstract,
    isStatic,
    isOverriding,
    isImplementing,
  }
}
export function e_empty(): TSExtensionList {
  return {
    implementsList: [],
  }
}
export function e_extends(extendsType: string): TSExtensionList {
  return {
    extendsType: ref_(extendsType),
    implementsList: [],
  }
}
export function e_implements(implementsList: readonly string[]): TSExtensionList {
  return {
    implementsList: implementsList.map((item) => ref_(item)),
  }
}
export function e_full(extendsType: string, implementsList: readonly string[]): TSExtensionList {
  return {
    extendsType: ref_(extendsType),
    implementsList: implementsList.map((item) => ref_(item)),
  }
}

export function block_(statements: readonly TSStatement[] = []): TSBlockStatement {
  return {
    kind: TSStatementKind.Block,
    statements,
  }
}
export function interface_(
  name: string,
  extendsList: readonly TSReferenceType[],
  methods: readonly TSMethod[],
  isExport: boolean,
  properties: readonly TSField[],
): TSInterface {
  return {
    kind: TSGlobalTypeKind.Interface,
    name,
    extendsList,
    methods,
    isExport,
    properties,
  }
}

export function class_(
  name: string,
  extensionList: TSExtensionList,
  ctor: TSConstructor | undefined,
  methods: readonly TSMethod[],
  fields: readonly TSField[],
  isAbstract: boolean,
  isExport: boolean,
  generics: readonly TSGenericType[],
  hasSerializableAnnotatiton: boolean,
  hasParcelizeAnnotation: boolean,
): TSClass {
  const result: TSClass = {
    kind: TSGlobalTypeKind.Class,
    name,
    extensionList,
    fields,
    methods,
    isAbstract,
    isExport,
    generics,
    extra: { hasSerializableAnnotatiton, hasParcelizeAnnotation },
  }
  return ctor ? { ...result, ctor } : result
}
export function method_(
  name: string,
  visibility: TSVisibility,
  args: readonly TSNormalFunctionArgument[],
  returnType: TSLocalType,
  extra: TSMethodExtra,
  generics: readonly TSGenericType[],
  body?: TSBlockStatement,
): TSMethod {
  const result: TSMethod = {
    name,
    visibility,
    args,
    returnType,
    extra,
    generics,
  }
  return body ? { ...result, body } : result
}
export function constructor_(
  args: readonly TSConstructorArgument[],
  visibility: TSVisibility,
  isOverriding: boolean,
  body: TSBlockStatement,
): TSConstructor {
  return {
    visibility,
    args,
    body,
    isOverriding,
  }
}
export function ctorArg(
  name: string,
  type: TSLocalType,
  isReadonly?: boolean,
  isOverriding?: boolean,
  isImplementing?: boolean,
  visibility?: TSVisibility,
  defaultValue?: TSExpression,
): TSConstructorArgument {
  const result: Writable<TSConstructorArgument> = {
    kind: TSFunctionArgumentType.Constructor,
    name,
    type,
  }

  if (isReadonly !== undefined || isOverriding !== undefined || isImplementing !== undefined || visibility) {
    result.propertyAttributes = {
      isReadonly: isReadonly || false,
      isOverriding: isOverriding || false,
      isImplementing: isImplementing || false,
      visibility: visibility || TSVisibility.Public,
    }
  }
  if (defaultValue !== undefined) {
    result.defaultValue = defaultValue
  }
  return result
}
export function field_(
  name: string,
  type: TSLocalType,
  visibility: TSVisibility,
  extra: TSFieldExtra,
  initializer?: TSExpression,
): TSField {
  return {
    name,
    type,
    visibility,
    initializer,
    extra,
  }
}
export function alias_(
  name: string,
  isExport: boolean,
  type: TSLocalType,
  generics: readonly TSGenericType[] = [],
): TSAlias {
  return {
    kind: TSGlobalTypeKind.Alias,
    name,
    type,
    isExport,
    generics,
  }
}
export function function_(
  name: string,
  args: readonly TSNormalFunctionArgument[],
  returnType: TSLocalType,
  generics: readonly TSGenericType[],
  isExport: boolean,
  body?: TSBlockStatement,
): TSFunction {
  return {
    kind: TSGlobalTypeKind.Function,
    name,
    args,
    returnType,
    generics,
    isExport,
    body,
  }
}
export function lit_(value: TSLiteralExpression['value']): TSLiteralExpression {
  return {
    kind: TSExpressionKind.Literal,
    value,
  }
}
export function enumWithValues_(name: string, isExport: boolean, members: readonly TSEnumMember[]): TSEnum {
  return makeEnum(name, members, isExport)
}

export function makeSourceFile(script: string, filename = 'test.ts'): ts.SourceFile {
  return ts.createSourceFile(filename, script, YandexScriptCompilerOptions.target!, true)
}

export function makeSourceFileWithTypechecker(filenames: readonly string[]): [ts.SourceFile, ts.TypeChecker] {
  const realFileNames = filenames.map(buildEnvironmentDependentFilePath)
  const program = ts.createProgram(realFileNames, YandexScriptCompilerOptions)
  const sourceFile = program.getSourceFile(realFileNames[0])!
  return [sourceFile, program.getTypeChecker()]
}

export function getStatements(node: ts.SourceFile): ts.NodeArray<ts.Statement> {
  if (node.statements && node.statements.length > 0) {
    return node.statements
  }
  throw error(node, 'Unsupported type of node')
}

export function getExpression(node: ts.SourceFile): ts.Expression {
  const statement = getStatements(node)[0]
  if (ts.isExpressionStatement(statement)) {
    return statement.expression
  }
  throw error(node, 'Unsupported type of node')
}

export function buildEnvironmentDependentFilePath(fileName: string): string {
  return typeof __STANDALONE__ !== 'undefined' && __STANDALONE__ ? fileName : Path.resolve('./packages/ys', fileName)
}

export function getExpressionsWithTypechecker(fileName: string): readonly [readonly ts.Expression[], ts.TypeChecker] {
  const realFileName = buildEnvironmentDependentFilePath(fileName)
  const program = ts.createProgram([realFileName], YandexScriptCompilerOptions)
  const typeChecker = program.getTypeChecker()
  const expressions: ts.Expression[] = []
  for (const statement of program.getSourceFile(realFileName)!.statements) {
    if (ts.isExpressionStatement(statement)) {
      expressions.push(statement.expression)
    }
  }
  return [expressions, typeChecker]
}

export const RESERVED_KEYWORD__BREAK__ = 'break'
export const RESERVED_KEYWORD__THROW__ = 'throw'
