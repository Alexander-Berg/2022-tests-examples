import { Int32, Nullable, undefinedToNull } from '../../../../../common/ys'
import { UserAccount } from '../../users/user-pool'
import { FeatureID } from '../mbt-abstractions'
import { AppModel, TestPlan } from '../walk/fixed-scenario-strategy'

export enum AccountType2 {
  Yandex = 'YANDEX',
  YandexTest = 'YANDEX_TEST',
  YandexTeam = 'YANDEX_TEAM',
  Yahoo = 'YAHOO',
  Google = 'GMAIL',
  Mail = 'MAIL',
  Hotmail = 'HOTMAIL',
  Rambler = 'RAMBLER',
  Outlook = 'OUTLOOK',
  Other = 'OTHER',
}

export enum MBTPlatform {
  MobileAPI,
  Android,
  IOS,
  Desktop,
}

export enum TestSuite {
  Fixed = 'Fixed_tests',
  Random = 'Random_tests',
  FullCoverage = 'Full_coverage',
}

export enum DeviceType {
  Phone,
  Tab,
}

/**
 * Базовый класс для всех тестов, параметризированный типом подготавливаемых перед тестом данных.
 * В swift нельзя делать параметризованные интерфейсы, поэтому приходится делать параметризованный абстрактный класс.
 */
export abstract class MBTTest<T> {
  protected constructor(public readonly description: string, public readonly suites: TestSuite[] = [TestSuite.Fixed]) {}

  /**
   * Если у вашего теста есть братишка в Пальме, то вы можете тут вернуть id соответсвующих кейсов
   */
  public setupSettings(settings: TestSettings): void {}

  abstract requiredAccounts(): AccountType2[]

  /**
   * Тут надо описать данные для теста
   * Этот метод вызывается перед методом scenario
   *
   * @param preparers - билдеры начального состояния данных для залоченных аккаунтов
   */
  abstract prepareAccounts(preparers: T[]): void

  /**
   * Тут надо вернуть сценарий для теста, вызывается после prepareAccounts
   *
   * @param accounts - аккаунты, которые оперируем во время теста. Может быть пустым, если тест проверя поддержку фичей
   * @param model - штука, которая может скачать ящик. Может быть null, если тест проверяет поддержку фичей
   * @param supportedFeatures - поддерживаемые приложением фичи
   */
  abstract scenario(accounts: UserAccount[], model: Nullable<AppModel>, supportedFeatures: FeatureID[]): TestPlan
}

export class TestSettings {
  private testCaseIds: Map<MBTPlatform, Int32> = new Map<MBTPlatform, Int32>()
  private tags: DeviceType[] = [DeviceType.Phone]
  private ignoredPlatforms: Set<MBTPlatform> = new Set()

  public constructor() {}

  public setTestCaseId(platform: MBTPlatform, id: Int32): TestSettings {
    this.testCaseIds.set(platform, id)
    return this
  }

  public androidCase(id: Int32): TestSettings {
    return this.setTestCaseId(MBTPlatform.Android, id)
  }

  public iosCase(id: Int32): TestSettings {
    return this.setTestCaseId(MBTPlatform.IOS, id)
  }

  public commonCase(id: Int32): TestSettings {
    return this.iosCase(id).androidCase(id)
  }

  public ignoreOn(platform: MBTPlatform): TestSettings {
    this.ignoredPlatforms.add(platform)
    return this
  }

  public isIgnored(platform: MBTPlatform): boolean {
    return this.ignoredPlatforms.has(platform)
  }

  public getCaseIDForPlatform(platform: MBTPlatform): Int32 {
    if (undefinedToNull(this.testCaseIds.get(platform)) !== null) {
      return this.testCaseIds.get(platform)!
    } else {
      return 0
    }
  }

  public setTags(tags: DeviceType[]): void {
    this.tags = tags
  }

  public hasTag(tag: DeviceType): boolean {
    return this.tags.includes(tag)
  }
}
