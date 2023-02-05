/* eslint-disable @typescript-eslint/unbound-method */
import { resolve } from '../../../../common/xpromise-support'
import { YSError } from '../../../../common/ys'
import { MockFileSystem, MockJSONSerializerWrapper } from '../../../common/__tests__/__helpers__/mock-patches'
import { ArrayJSONItem } from '../../../common/code/json/json-types'
import { getVoid, Result } from '../../../common/code/result/result'
import { FlagsConfiguration, FlagsConfigurationSource } from '../../code/api/flags-configuration'
import { FlagsResponse } from '../../code/api/flags-response'
import {
  DefaultFlagConfigurationsPaths,
  FileSystemFlagConfigurationsStore,
  FlagConfigurationsStore,
} from '../../code/flag-configurations-store'
import { JSONItemFromJSON } from '../../../common/__tests__/__helpers__/json-helpers'

export function MockFlagConfigurationsStore(patch: Partial<FlagConfigurationsStore> = {}): FlagConfigurationsStore {
  return Object.assign(
    {},
    {
      storeRawConfigurations: jest.fn(),
      activatePendingConfigurations: jest.fn(),
      fetchActivatedResponse: jest.fn(),
      activate: jest.fn(),
    },
    patch,
  )
}

describe(DefaultFlagConfigurationsPaths, () => {
  it('should build correct paths', () => {
    const fs = MockFileSystem({
      directories: {
        documentDirectory: '/documents',
        cachesDirectory: '/caches',
      },
    })
    const paths = DefaultFlagConfigurationsPaths.buildFlagConfigurationsPaths(fs)

    expect(paths.flagsFolderPath).toBe('/documents/xmail_flags')
    expect(paths.activatedConfigPath).toBe('/documents/xmail_flags/activated_flags.json')
    expect(paths.pendingConfigPath).toBe('/documents/xmail_flags/pending_flags.json')
  })
})

describe(FileSystemFlagConfigurationsStore, () => {
  const paths = {
    flagsFolderPath: '/documents/xmail_flags',
    activatedConfigPath: '/documents/xmail_flags/activated_flags.json',
    pendingConfigPath: '/documents/xmail_flags/pending_flags.json',
  }

  it('should return empty items array if activated file is missing', (done) => {
    const fs = MockFileSystem({
      exists: jest.fn().mockReturnValue(resolve(false)),
    })
    const serializer = MockJSONSerializerWrapper()
    const configurationsStore = new FileSystemFlagConfigurationsStore(fs, paths, serializer)

    expect.assertions(2)
    configurationsStore.fetchActivatedResponse().then((response) => {
      expect(response).toStrictEqual(new FlagsResponse([], new Map()))
      expect(fs.exists).toBeCalledWith('/documents/xmail_flags/activated_flags.json')
      done()
    })
  })
  it('should fail to fetch activated configurations if the file could not be parsed', (done) => {
    const fs = MockFileSystem({
      exists: jest.fn().mockReturnValue(resolve(true)),
      readAsString: jest.fn().mockReturnValue(resolve('Some JSON value')),
    })
    const serializer = MockJSONSerializerWrapper({
      deserializeJSONItem: jest.fn().mockReturnValue(new Result(JSONItemFromJSON({}), null)),
    })
    const configurationsStore = new FileSystemFlagConfigurationsStore(fs, paths, serializer)

    expect.assertions(1)
    configurationsStore.fetchActivatedResponse().failed((error) => {
      expect(error).toStrictEqual(new YSError('Failed to parse FlagsResponse:\n<JSONItem kind: map, value: {}>'))
      done()
    })
  })
  it('should actually fetch activated configurations', (done) => {
    const fs = MockFileSystem({
      exists: jest.fn().mockReturnValue(resolve(true)),
      readAsString: jest.fn().mockReturnValue(resolve('Some JSON value')),
    })
    const serializer = MockJSONSerializerWrapper({
      deserializeJSONItem: jest.fn().mockReturnValue(
        new Result(
          JSONItemFromJSON({
            configurations: [
              {
                HANDLER: 'MOBMAIL',
                CONDITION: 'condition',
                CONTEXT: {
                  MOBMAIL: {
                    source: 'experiment',
                    logs: { test_id: 'value' },
                    flags: {},
                  },
                },
              },
            ],
          }),
          null,
        ),
      ),
    })
    const configurationsStore = new FileSystemFlagConfigurationsStore(fs, paths, serializer)

    expect.assertions(4)
    configurationsStore.fetchActivatedResponse().then((response) => {
      expect(response).toStrictEqual(
        new FlagsResponse(
          [
            new FlagsConfiguration(
              FlagsConfigurationSource.experiment,
              'condition',
              new Map([['test_id', 'value']]),
              new Map(),
            ),
          ],
          new Map(),
        ),
      )
      expect(fs.exists).toBeCalledWith('/documents/xmail_flags/activated_flags.json')
      expect(fs.readAsString).toBeCalledWith('/documents/xmail_flags/activated_flags.json')
      expect(serializer.deserializeJSONItem).toBeCalledWith('Some JSON value')
      done()
    })
  })
  it('should store raw configurations', (done) => {
    const serializer = MockJSONSerializerWrapper({
      serialize: jest.fn().mockReturnValue(new Result('serialized value', null)),
    })
    const fs = MockFileSystem({
      ensureFolderExists: jest.fn().mockReturnValue(resolve(getVoid())),
      writeAsString: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const configurationsStore = new FileSystemFlagConfigurationsStore(fs, paths, serializer)

    const configurations = JSONItemFromJSON([
      {
        source: 'experiment',
        condition: 'condition',
        logs: { test_id: 'value' },
        flags: {},
      },
    ]) as ArrayJSONItem

    expect.assertions(3)
    configurationsStore.storeRawConfigurations(configurations).then(() => {
      expect(serializer.serialize).toBeCalledWith(configurations)
      expect(fs.ensureFolderExists).toBeCalledWith('/documents/xmail_flags')
      expect(fs.writeAsString).toBeCalledWith('/documents/xmail_flags/pending_flags.json', 'serialized value', 0, true)
      done()
    })
  })
  it('should activate pending response items if they exist', (done) => {
    const serializer = MockJSONSerializerWrapper()
    const fs = MockFileSystem({
      exists: jest.fn().mockReturnValue(resolve(true)),
      delete: jest.fn().mockReturnValue(resolve(getVoid())),
      move: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const configurationsStore = new FileSystemFlagConfigurationsStore(fs, paths, serializer)

    expect.assertions(2)
    configurationsStore.activatePendingConfigurations().then(() => {
      expect(fs.exists).toBeCalledWith('/documents/xmail_flags/pending_flags.json')
      expect(fs.move).toBeCalledWith(
        '/documents/xmail_flags/pending_flags.json',
        '/documents/xmail_flags/activated_flags.json',
        true,
        true,
      )
      done()
    })
  })
  it('should skip activation if pending response items do not exist', (done) => {
    const serializer = MockJSONSerializerWrapper()
    const fs = MockFileSystem({
      exists: jest.fn().mockReturnValue(resolve(false)),
    })
    const configurationsStore = new FileSystemFlagConfigurationsStore(fs, paths, serializer)

    expect.assertions(3)
    configurationsStore.activatePendingConfigurations().then(() => {
      expect(fs.exists).toBeCalledWith('/documents/xmail_flags/pending_flags.json')
      expect(fs.delete).not.toBeCalled()
      expect(fs.move).not.toBeCalled()
      done()
    })
  })
})
