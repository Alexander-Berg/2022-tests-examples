import ts from 'typescript'
import { YandexScriptCompilerOptions } from '../src/compiler-options'
import { TSAlias, TSClass, TSFunction, TSGlobalTypeKind } from '../src/generators-model/global-types'
import { extractGlobalType } from '../src/parsing-helpers/type-extractors'
import { buildEnvironmentDependentFilePath, gen_, int32_, ref_, refs_ } from './__helpers__/test-helpers'

function getDeclarationsWithTypechecker(fileName: string): [readonly ts.Declaration[], ts.TypeChecker] {
  const realFileName = buildEnvironmentDependentFilePath(fileName)
  const program = ts.createProgram([realFileName], YandexScriptCompilerOptions)
  const typeChecker = program.getTypeChecker()
  const result: (ts.ClassDeclaration | ts.FunctionDeclaration | ts.TypeAliasDeclaration)[] = []
  for (const statement of program.getSourceFile(realFileName)!.statements) {
    if (
      ts.isClassDeclaration(statement) ||
      ts.isFunctionDeclaration(statement) ||
      ts.isTypeAliasDeclaration(statement)
    ) {
      const text = statement.name!.getText()
      if (!text.startsWith('C') && !text.startsWith('Z')) {
        result.push(statement)
      }
    }
  }
  return [result, typeChecker]
}

let declarations: readonly ts.Declaration[]
let typechecker: ts.TypeChecker
beforeAll(() => {
  ;[declarations, typechecker] = getDeclarationsWithTypechecker('./__tests__/__helpers__/files/generics-constraints.ts')
})

// CLASSES
test('should extract interface type constraint from class declaration', () => {
  const declaration = declarations[0] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A1')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('I1'))])
})
test('should extract several interface types constraint from class declaration', () => {
  const declaration = declarations[1] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A2')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('I1', 'I2'))])
})
test('should extract class type constraint from class declaration', () => {
  const declaration = declarations[2] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A3')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1'))])
})
test('should extract class type constraint and interface constraint from class declaration', () => {
  const declaration = declarations[3] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A4')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1', 'I1'))])
})
test('should extract class type constraint and multiple interface constraints from class declaration', () => {
  const declaration = declarations[4] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A5')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1', 'I1', 'I2'))])
})
test('should extract multiple generics with class type and interface constraints from class declaration', () => {
  const declaration = declarations[5] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A6')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1')), gen_('U', refs_('C1', 'I1'))])
})
test('should extract generic with complete generic class constraint from class declaration', () => {
  const declaration = declarations[6] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A7')
  expect(extracted.generics).toMatchObject([gen_('T', [ref_('C3', [int32_()])])])
})

// FUNCTIONS
test('should extract interface type constraint from function declaration', () => {
  const declaration = declarations[7] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f1')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('I1'))])
})
test('should extract several interface types constraint from function declaration', () => {
  const declaration = declarations[8] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f2')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('I1', 'I2'))])
})
test('should extract class type constraint from function declaration', () => {
  const declaration = declarations[9] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f3')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1'))])
})
test('should extract class type constraint and interface constraint from function declaration', () => {
  const declaration = declarations[10] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f4')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1', 'I1'))])
})
test('should extract class type constraint and multiple interface constraints from function declaration', () => {
  const declaration = declarations[11] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f5')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1', 'I1', 'I2'))])
})
test('should extract multiple generics with class type and interface constraints from function declaration', () => {
  const declaration = declarations[12] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f6')
  expect(extracted.generics).toMatchObject([gen_('T', refs_('C1')), gen_('U', refs_('C1', 'I1'))])
})
test('should extract generic with complete generic class constraint from function declaration', () => {
  const declaration = declarations[13] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f7')
  expect(extracted.generics).toMatchObject([gen_('T', [ref_('C3', [int32_()])])])
})

// ALIASES
test('should extract interface type constraint from alias declaration and throw', () => {
  const declaration = declarations[14] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})
test('should extract several interface types constraint from alias declaration and throw', () => {
  const declaration = declarations[15] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})
test('should extract class type constraint from alias declaration and throw', () => {
  const declaration = declarations[16] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})
test('should extract class type constraint and interface constraint from alias declaration and throw', () => {
  const declaration = declarations[17] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})
test('should extract class type constraint and multiple interface constraints from alias declaration and throw', () => {
  const declaration = declarations[18] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})
// tslint:disable-next-line: max-line-length
test('should extract multiple generics with class type and interface constraints from alias declaration and throw', () => {
  const declaration = declarations[19] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})
test('should extract generic with complete generic class constraint from alias declaration and throw', () => {
  const declaration = declarations[20] as ts.TypeAliasDeclaration
  expect(() => extractGlobalType(declaration, typechecker) as TSAlias).toThrowError(
    'Generic constraints are not supported with aliases',
  )
})

// NO CONSTRAINT
test('should extract generic parameter if not constraints from class declaration', () => {
  const declaration = declarations[21] as ts.ClassDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSClass

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Class)
  expect(extracted.name).toStrictEqual('A0')
  expect(extracted.generics).toMatchObject([gen_('T')])
})
test('should extract generic parameter if not constraints from function declaration', () => {
  const declaration = declarations[22] as ts.FunctionDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSFunction

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Function)
  expect(extracted.name).toStrictEqual('f0')
  expect(extracted.generics).toMatchObject([gen_('T')])
})
test('should extract generic parameter if not constraints from alias declaration', () => {
  const declaration = declarations[23] as ts.TypeAliasDeclaration
  const extracted = extractGlobalType(declaration, typechecker) as TSAlias

  expect(extracted).not.toBeNull()
  expect(extracted.kind).toBe(TSGlobalTypeKind.Alias)
  expect(extracted.name).toStrictEqual('T0')
  expect(extracted.generics).toMatchObject([gen_('T')])
})
test('should throw if class generic parameter is constraints with non-Reference', () => {
  const declaration = declarations[24] as ts.ClassDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Type Number is not supported as constraint')
})
test('should throw if function generic parameter is constraints with non-Reference', () => {
  const declaration = declarations[25] as ts.FunctionDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Type String is not supported as constraint')
})
test('should throw if alias generic parameter is constraints with non-Reference', () => {
  const declaration = declarations[26] as ts.TypeAliasDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Type Function is not supported as constraint')
})
test('should throw if class generic parameter is constraints intersection type with non-Reference', () => {
  const declaration = declarations[27] as ts.ClassDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Type Number is not supported as constraint')
})
test('should throw if function generic parameter is constraints intersection type with non-Reference', () => {
  const declaration = declarations[28] as ts.FunctionDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Type String is not supported as constraint')
})
test('should throw if alias generic parameter is constraints intersection type with non-Reference', () => {
  const declaration = declarations[29] as ts.TypeAliasDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Type Function is not supported as constraint')
})
test('should throw if class generic parameter is constraints union type with non-Reference', () => {
  const declaration = declarations[30] as ts.ClassDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Unsupported type of complex type')
})
test('should throw if function generic parameter is constraints union type with non-Reference', () => {
  const declaration = declarations[31] as ts.FunctionDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Unsupported type of complex type')
})
test('should throw if alias generic parameter is constraints union type with non-Reference', () => {
  const declaration = declarations[32] as ts.TypeAliasDeclaration
  expect(() => {
    extractGlobalType(declaration, typechecker)
  }).toThrowError('Unsupported type of complex type')
})
