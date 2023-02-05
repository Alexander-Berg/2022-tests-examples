import ts from 'typescript'
import { error, setAssociatedNode } from '../src/parsing-helpers/error'
import { buildEnvironmentDependentFilePath, getExpressionsWithTypechecker } from './__helpers__/test-helpers'

describe(error, () => {
  let expressions: readonly ts.Expression[]
  let typechecker: ts.TypeChecker
  const filePath = '__tests__/__helpers__/files/example-for-error.ts'
  const checkFilePath = buildEnvironmentDependentFilePath(filePath)
  beforeAll(() => {
    ;[expressions, typechecker] = getExpressionsWithTypechecker(filePath)
  })

  it('should create an error for a regular node', () => {
    const callExpression = expressions[0] as ts.CallExpression
    const errorValue = error(callExpression, 'Error Message')

    // tslint:disable-next-line:max-line-length
    expect(errorValue).toEqual(new Error(`Error Message: file '${checkFilePath}'; char: 1; line: 2; text: 'foo()'`))
  })

  it('should create an error for a node without a source file', () => {
    const callExpression = expressions[0] as ts.CallExpression
    const returnTypeNode = extractRetuenTypeNode(callExpression, typechecker)
    const errorValue = error(returnTypeNode, 'Error Message')

    // tslint:disable-next-line:max-line-length
    expect(errorValue).toEqual(new Error("Error Message: file 'unknown'; char: NaN; line: NaN; text: 'unknown'"))
  })

  it('should create an error for a node without a source file which has an associated node', () => {
    const callExpression = expressions[0] as ts.CallExpression
    const returnTypeNode = extractRetuenTypeNode(callExpression, typechecker)
    setAssociatedNode(returnTypeNode, callExpression)
    const errorValue = error(returnTypeNode, 'Error Message')

    // tslint:disable-next-line:max-line-length
    expect(errorValue).toEqual(new Error(`Error Message: file '${checkFilePath}'; char: 1; line: 2; text: 'foo()'`))
  })
})

function extractRetuenTypeNode(node: ts.Node, typechecker: ts.TypeChecker): ts.TypeNode {
  const type = typechecker.getTypeAtLocation(node)
  const typeNode = typechecker.typeToTypeNode(type)
  return typeNode!
}
