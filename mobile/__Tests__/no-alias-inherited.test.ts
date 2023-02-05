import FileProcessor from '../src/fileprocessor'
import { TestGenerator } from './__helpers__/generator'
import { makeSourceFileWithTypechecker } from './__helpers__/test-helpers'

test('should not allow aliases in inheritance chain', () => {
  const [sourceFile, typechecker] = makeSourceFileWithTypechecker([
    './__tests__/__helpers__/files/no-alias-inherited.ts',
  ])
  const generator = new TestGenerator()
  const processor = new FileProcessor<{}>(generator, '', '', typechecker)
  expect(() => processor.process(sourceFile)).toThrowError('Type aliases in inheritance chain are not supported')
})
