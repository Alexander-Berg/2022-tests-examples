import { FileSystem } from '../../code/file-system/file-system'
import { MobileFileSystemPath } from '../../code/file-system/mobile-file-system-path'
import { JSONSerializer } from '../../code/json/json-serializer'
import { JSONSerializerWrapper } from '../../code/json/json-serializer-wrapper'
import { JSONItem } from '../../code/json/json-types'
import { Log, Logger } from '../../code/logging/logger'
import { Network } from '../../code/network/network'
import { Result } from '../../code/result/result'
import { JSONItemFromJSONString, JSONItemToJSONString } from './json-helpers'
import { createMockInstance, tryCatchResult } from './utils'

export function MockJSONSerializer(patch: Partial<JSONSerializer> = {}): JSONSerializer {
  return Object.assign(
    {},
    {
      serialize: jest.fn(),
      deserialize: jest.fn(),
    },
    patch,
  )
}

export function MockJSONSerializerWrapper(patch: Partial<JSONSerializerWrapper> = {}): JSONSerializerWrapper {
  return createMockInstance(JSONSerializerWrapper, patch)
}

export function RealJSONSerializerWrapper(): JSONSerializerWrapper {
  return createMockInstance(JSONSerializerWrapper, {
    serialize: jest.fn((item: JSONItem): Result<string> => tryCatchResult(() => JSONItemToJSONString(item))),
    deserializeJSONItem: jest.fn(
      (contents: string): Result<JSONItem> => tryCatchResult(() => JSONItemFromJSONString(contents)),
    ),
  })
}

export function MockFileSystem(patch: Partial<FileSystem> = {}): FileSystem {
  return createMockInstance(
    FileSystem,
    Object.assign(
      {
        directories: {
          cachesDirectory: 'CACHES',
          documentDirectory: 'DOCUMENTS',
        },
        path: new MobileFileSystemPath('/'),
      },
      patch,
    ),
  )
}

export function MockLogger(patch: Partial<Logger> = {}): Logger {
  return Object.assign(
    {},
    {
      info: jest.fn(),
      warn: jest.fn(),
      error: jest.fn(),
    },
    patch,
  )
}

export function mockLogger(): void {
  return Log.registerDefaultLogger(MockLogger())
}

export function MockNetwork(patch: Partial<Network> = {}): Network {
  return Object.assign(
    {},
    {
      execute: jest.fn(),
      executeRaw: jest.fn(),
      resolveURL: jest.fn(),
    },
    patch,
  )
}
