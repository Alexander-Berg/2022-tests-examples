import { TSBuiltInType } from '../../src/generators-model/basic-types'
import { TypeMappingProvider, TypeMapping } from '../../src/generators-model/type-mapping'

export function testTypeMappingsProviderGenerator(): TypeMappingProvider {
  function id(name: string): string {
    return name
  }
  return (type: TSBuiltInType): TypeMapping => {
    switch (type) {
      case TSBuiltInType.Array:
      case TSBuiltInType.ReadonlyArray:
        return {
          name: 'YSArray',
          constructorName: 'mutableListOf',
          mapFunctionName: (name): string => {
            switch (name) {
              case 'length':
                return 'count'
              case 'includes':
                return 'contains'
              default:
                return name
            }
          },
        }
      case TSBuiltInType.Boolean:
        return {
          name: 'YSBoolean',
          constructorName: 'YSBoolean',
          mapFunctionName: id,
        }
      case TSBuiltInType.Date:
        return {
          name: 'YSDate',
          constructorName: 'YSDate',
          mapFunctionName: id,
        }
      case TSBuiltInType.Map:
      case TSBuiltInType.ReadonlyMap:
        return {
          name: 'YSMap',
          constructorName: 'mutableMapOf',
          mapFunctionName: id,
        }
      case TSBuiltInType.Math:
        return {
          name: 'YSMath',
          constructorName: 'YSMath',
          mapFunctionName: id,
        }
      case TSBuiltInType.Number:
      case TSBuiltInType.BigInt:
        return {
          name: 'YSNumber',
          constructorName: 'YSNumber',
          mapFunctionName: id,
        }
      case TSBuiltInType.Set:
      case TSBuiltInType.ReadonlySet:
        return {
          name: 'YSSet',
          constructorName: 'YSSet',
          mapFunctionName: id,
        }
      case TSBuiltInType.String:
        return {
          name: 'YSString',
          constructorName: 'YSString',
          mapFunctionName: id,
        }
    }
  }
}
