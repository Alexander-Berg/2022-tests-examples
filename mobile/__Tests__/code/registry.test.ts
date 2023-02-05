import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { Platform } from '../../../common/code/network/platform'
import { FileSystem } from '../../../common/code/file-system/file-system'
import { I18n } from '../../code/api/I18n/I18n'
import { Metrics } from '../../code/api/logging/metrics'
import { HighPrecisionTimer } from '../../code/api/logging/perf/high-precision-timer'
import { Network } from '../../../common/code/network/network'
import { SharedPreferencesProvider } from '../../../common/code/shared-prefs/shared-preferences'
import { Storage } from '../../code/api/storage/storage'
import { Registry } from '../../code/registry'
import { ServiceLocatorItems } from '../../code/utils/service-locator'

describe(Registry, () => {
  beforeAll(Registry.drop)
  afterEach(Registry.drop)

  it('should register JSON serializer', () => {
    const serializer: JSONSerializer = {} as any
    Registry.registerJSONSerializer(serializer)
    expect(Registry.getJSONSerializer()).toBe(serializer)
  })
  it('should throw if accessing unregistered JSON serializer', () => {
    expect(() => Registry.getJSONSerializer()).toThrowError('JSON Serializer must be registered before use')
  })
  it('should register Network provider', () => {
    const network: Network = {} as any
    Registry.registerNetwork(network)
    expect(Registry.getNetwork()).toBe(network)
  })
  it('should throw if accessing unregistered Network provider', () => {
    expect(() => Registry.getNetwork()).toThrowError('Network must be registered before use')
  })
  it('should register Storage provider', () => {
    const storage: Storage = {} as any
    Registry.registerStorage(storage)
    expect(Registry.getStorage()).toBe(storage)
  })
  it('should throw if accessing unregistered Storage provider', () => {
    expect(() => Registry.getStorage()).toThrowError('Storage must be registered before use')
  })
  it('should register Metrics', () => {
    const metrics: Metrics = {} as any
    Registry.registerMetrics(metrics)
    expect(Registry.getMetrics()).toBe(metrics)
  })
  it('should throw if accessing unregistered Metrics', () => {
    expect(() => Registry.getMetrics()).toThrowError('Metrics must be registered before use')
  })
  it('should register High Precision Timer', () => {
    const timer: HighPrecisionTimer = {} as any
    Registry.registerHighPrecisionTimer(timer)
    expect(Registry.getHighPrecisionTimer()).toBe(timer)
  })
  it('should throw if accessing unregistered High Precision Timer', () => {
    expect(() => Registry.getHighPrecisionTimer()).toThrowError('High Precision Timer must be registered before use')
  })
  it('should register File System', () => {
    const fileSystem: FileSystem = {} as any
    Registry.registerFileSystem(fileSystem)
    expect(Registry.getFileSystem()).toBe(fileSystem)
  })
  it('should throw if accessing unregistered File System', () => {
    expect(() => Registry.getFileSystem()).toThrowError('File System must be registered before use')
  })
  it('should register Shared Preferences Provider', () => {
    const sharedPreferencesProvider: SharedPreferencesProvider = {} as any
    Registry.registerSharedPreferencesProvider(sharedPreferencesProvider)
    expect(Registry.getSharedPreferencesProvider()).toBe(sharedPreferencesProvider)
  })
  it('should throw if accessing unregistered Shared Preferences Provider', () => {
    expect(() => Registry.getSharedPreferencesProvider()).toThrowError(
      'Shared Preferences Provider must be registered before use',
    )
  })
  it('should register Platform', () => {
    const platform: Platform = {} as any
    Registry.registerPlatform(platform)
    expect(Registry.getPlatform()).toBe(platform)
  })
  it('should throw if accessing unregistered Platform', () => {
    expect(() => Registry.getPlatform()).toThrowError('Platform must be registered before use')
  })
  it('should register Service Locator', () => {
    const sample = { key: 'value' }
    Registry.registerServiceLocatorItems(
      new Map<ServiceLocatorItems, () => any>([[ServiceLocatorItems.blockingDeque, () => sample]]),
    )
    expect(Registry.getServiceLocator().locate(ServiceLocatorItems.blockingDeque)).toBe(sample)
  })
  it('should throw if accessing Locator without registering', () => {
    expect(() => Registry.getServiceLocator()).toThrowError('Service Locator must be registered before use')
  })
  it('should register I18n', () => {
    const i18n: I18n = {} as any
    Registry.registerI18n(i18n)
    expect(Registry.getI18n()).toBe(i18n)
  })
  it('should throw if accessing unregistered I18n', () => {
    expect(() => Registry.getI18n()).toThrowError('I18n must be registered before use')
  })
  it('should clear registered items if drop is invoked', () => {
    const serializer: JSONSerializer = {} as any
    Registry.registerJSONSerializer(serializer)
    const network: Network = {} as any
    Registry.registerNetwork(network)
    const storage: Storage = {} as any
    Registry.registerStorage(storage)
    const metrics: Metrics = {} as any
    Registry.registerMetrics(metrics)
    const timer: HighPrecisionTimer = {} as any
    Registry.registerHighPrecisionTimer(timer)
    const fileSystem: FileSystem = {} as any
    Registry.registerFileSystem(fileSystem)
    const sharedPreferencesProvider: SharedPreferencesProvider = {} as any
    Registry.registerSharedPreferencesProvider(sharedPreferencesProvider)
    const platform: Platform = {} as any
    Registry.registerPlatform(platform)
    Registry.registerServiceLocatorItems(new Map())
    const i18n: I18n = {} as any
    Registry.registerI18n(i18n)

    Registry.drop()

    expect(() => Registry.getJSONSerializer()).toThrowError('JSON Serializer must be registered before use')
    expect(() => Registry.getNetwork()).toThrowError('Network must be registered before use')
    expect(() => Registry.getStorage()).toThrowError('Storage must be registered before use')
    expect(() => Registry.getMetrics()).toThrowError('Metrics must be registered before use')
    expect(() => Registry.getHighPrecisionTimer()).toThrowError('High Precision Timer must be registered before use')
    expect(() => Registry.getFileSystem()).toThrowError('File System must be registered before use')
    expect(() => Registry.getSharedPreferencesProvider()).toThrowError(
      'Shared Preferences Provider must be registered before use',
    )
    expect(() => Registry.getPlatform()).toThrowError('Platform must be registered before use')
    expect(() => Registry.getServiceLocator()).toThrowError('Service Locator must be registered before use')
    expect(() => Registry.getI18n()).toThrowError('I18n must be registered before use')
  })
})
