import { FileGenerator, Generator as Gen } from '../../src/generator'
import { TSAlias, TSClass, TSEnum, TSFunction, TSInterface } from '../../src/generators-model/global-types'
import { TSGlobalType, TSGlobalTypeKind } from '../../src/generators-model/global-types'
import { TSImportedTypes } from '../../src/generators-model/imported-types'

export class TestGenerator implements Gen<{}> {
  public readonly interfaces: TSInterface[] = []
  public readonly classes: TSClass[] = []
  public readonly imports: TSImportedTypes = new TSImportedTypes()
  public readonly aliases: TSAlias[] = []
  public readonly functions: TSFunction[] = []
  public readonly enums: TSEnum[] = []

  public getName(): string {
    return 'test-generator'
  }

  public createFile(_inputPath: string, _sourceFileName: string, _outputPath: string, _config?: {}): FileGenerator {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const generator = this
    return {
      writeHeader(): void {
        // Do nothing
      },
      write(item: TSGlobalType): void {
        switch (item.kind) {
          case TSGlobalTypeKind.Class:
            generator.classes.push(item as TSClass)
            break
          case TSGlobalTypeKind.Interface:
            generator.interfaces.push(item as TSInterface)
            break
          case TSGlobalTypeKind.Alias:
            generator.aliases.push(item as TSAlias)
            break
          case TSGlobalTypeKind.Enum:
            generator.enums.push(item as TSEnum)
            break
          case TSGlobalTypeKind.Function:
            generator.functions.push(item as TSFunction)
            break
        }
      },
      extendImportedTypes(names: readonly string[], source: string): void {
        generator.imports.extend(names, source)
      },
      flush(): void {
        // Do nothing
      },
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  public allFilesProcessed(_inputPath: string, _outputPath: string): void {}
}
