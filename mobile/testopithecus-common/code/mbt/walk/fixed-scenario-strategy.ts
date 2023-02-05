import { all, reject } from '../../../../../common/xpromise-support'
import {
  Int32,
  int32ToString,
  Int64,
  int64,
  int64ToInt32,
  Nullable,
  range,
  Throwing,
  YSError,
} from '../../../../../common/ys'
import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { MapJSONItem } from '../../../../common/code/json/json-types'
import { Logger } from '../../../../common/code/logging/logger'
import { XPromise } from '../../../../common/code/promise/xpromise'
import { Result } from '../../../../common/code/result/result'
import { maxInt32 } from '../../../../common/code/utils/math'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import { SyncNetwork } from '../../client/network/sync-network'
import { CrossPlatformLogsParser } from '../../event-logs/cross-platform-logs-parser'
import { OAuthApplicationCredentialsRegistry, OauthHostsConfig, OauthService } from '../../users/oauth-service'
import { OAuthUserAccount, UserLock } from '../../users/user-pool'
import { UserService } from '../../users/user-service'
import { UserServiceEnsemble } from '../../users/user-service-ensemble'
import { assertStringEquals } from '../../utils/assert'
import { ReportIntegration } from '../../utils/report-integration'
import { StatNetworkRequest } from '../../utils/stat-network-request'
import { getYSError, requireNonNull } from '../../utils/utils'
import { AssertAction } from '../actions/assert-action'
import { DebugDumpAction } from '../actions/debug-dump-action'
import { PingAccountLockAction } from '../actions/ping-account-lock-action'
import { App, FeatureID, MBTAction, MBTComponent } from '../mbt-abstractions'
import { StateMachine, WalkStrategy } from '../state-machine'
import { AccountDataPreparer, AccountDataPreparerProvider } from '../test/account-data-preparer'
import { AccountType2, MBTPlatform, MBTTest } from '../test/mbt-test'
import { TestsRegistry } from '../test/tests-registry'

export class FixedScenarioStrategy implements WalkStrategy {
  private position: Int32 = 0

  public constructor(private scenario: MBTAction[]) {}

  public nextAction(
    _model: App,
    _applicationFeatures: FeatureID[],
    _component: MBTComponent,
  ): Throwing<Nullable<MBTAction>> {
    if (this.position === this.scenario.length) {
      return null
    }
    const action = this.scenario[this.position]
    this.position += 1
    return action
  }
}

export class TestPlan {
  private actions: MBTAction[] = []

  private constructor() {}

  /**
   * Стотический метод для вызова конструктора. При его использовании указывать LoginAction
   * надо самостоятельно.
   */
  public static empty(): TestPlan {
    return new TestPlan()
  }

  public then(action: MBTAction): TestPlan {
    return this.thenChain([action])
  }

  public thenChain(actions: MBTAction[]): TestPlan {
    for (const action of actions) {
      this.actions.push(action)
      this.actions.push(new DebugDumpAction())
    }
    return this
  }

  public unsupportedActions(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): MBTAction[] {
    const result: MBTAction[] = []
    for (const action of this.actions) {
      if (!action.supported(modelFeatures, applicationFeatures)) {
        result.push(action)
      }
    }
    return result
  }

  public getExpectedEvents(): EventusEvent[] {
    const result: EventusEvent[] = []
    for (const action of this.actions) {
      for (const event of action.events()) {
        result.push(event)
      }
    }
    return result
  }

  public isActionInTestPlan(type: string): boolean {
    for (const action of this.actions) {
      if (action.getActionType() === type) {
        return true
      }
    }
    return false
  }

  public build(accountLocks: Nullable<UserLock[]>, assertionsEnabled: boolean = true): WalkStrategy {
    const actions: MBTAction[] = []
    for (const action of this.actions) {
      if (assertionsEnabled || AssertAction.type !== action.getActionType()) {
        actions.push(action)
      }
      if (accountLocks !== null) {
        accountLocks!.forEach((accLock) => actions.push(new PingAccountLockAction(accLock)))
      }
    }
    if (!this.isActionInTestPlan('AssertSnapshotAction')) {
      actions.push(new AssertAction())
    }
    return new FixedScenarioStrategy(actions)
  }

  public tostring(): string {
    let s = 'PATH\n'
    for (const action of this.actions) {
      s += action.tostring() + '\n'
    }
    return s + 'END\n'
  }
}

export class TestopithecusTestRunner<T extends AccountDataPreparer> {
  public readonly oauthService: OauthService
  public reporter: Nullable<ReportIntegration>
  private locks: UserLock[] = []
  private modelProvider: Nullable<AppModelProvider> = null
  private testPlan: Nullable<TestPlan> = null
  private userAccountsMap: UserServiceEnsemble
  private startTime: Int64

  /**
   * Запускалка статических тестов. Умеет проверять, поддерживает ли приложение данный тест
   *
   * @param platform - платформа. на которой запускаются тесты
   * @param test - тест, который мы хотим запустить
   * @param testsRegistry - registry всех тестов, в котором сказано, что с этим тестом делать
   * @param preparerProvider - штука, которая подготавливает данные перед тестом
   * @param applicationCredentials - передайте сюда clientId и clientSecret своего приложения
   * @param network - сеть
   * @param jsonSerializer - работа с json
   * @param logger - логгер
   * @param assertionsEnabled - включены ли assert-ы, по-умолчанию, true
   * @param userTags - если задать, из TUS будут выбираться пользователи с этими тегами, вместо дефолтных
   * @param statToken - токен Стата, нужен для импорта статистики автотестов
   * @param testPalmToken - токен Пальмы, нужен для получения id trusted тестов
   * @param tusConsumer - можно переопределить
   * {@link https://wiki.yandex-team.ru/test-user-service/#tusconsumer tus_consumer},
   * @param oauthHostConfig - конфиг хостов для oauth service, можно переопределить
   * если не задан, выбираются пользователи из "testopithecus"
   */
  public constructor(
    private platform: MBTPlatform,
    private readonly test: MBTTest<T>,
    private readonly testsRegistry: TestsRegistry<T>,
    private preparerProvider: AccountDataPreparerProvider<T>,
    applicationCredentials: OAuthApplicationCredentialsRegistry,
    private network: SyncNetwork,
    private jsonSerializer: JSONSerializer,
    private logger: Logger,
    private assertionsEnabled: boolean = true,
    private userTags: string[] = [],
    public statToken: string = '',
    public testPalmToken: string = '',
    private tusConsumer: string = 'testopithecus',
    oauthHostConfig: OauthHostsConfig = new OauthHostsConfig(),
  ) {
    const userService = new UserService(network, jsonSerializer, logger)
    this.userAccountsMap = new UserServiceEnsemble(
      userService,
      this.test.requiredAccounts(),
      this.tusConsumer,
      this.userTags,
    )
    this.oauthService = new OauthService(applicationCredentials, network, jsonSerializer, oauthHostConfig)
    this.startTime = int64(0)
    this.reporter = null
  }

  /**
   * Сначала проверяем, поддерживает ли тест приложение
   *
   * @param modelFeatures - фичи, поодерживаемые моделью
   * @param applicationFeatures - фичи, поддерживаемые приложением
   */
  public isEnabled(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return this.testsRegistry.isTestEnabled(this.test, this.platform, modelFeatures, applicationFeatures)
  }

  public isPassed(): boolean {
    const passed = this.testsRegistry.isPassed(this.test)
    if (passed) {
      this.logger.info(`Тест '${this.test.description}' уже пройден, поэтому сейчас он пройден не будет`)
    }
    return passed
  }

  public clearTestResults(): void {
    this.testsRegistry.clearTestResults()
  }

  public isNeedTryMore(): boolean {
    const isNeedTryMore = this.testsRegistry.isNeedTryMore(this.test)
    if (!isNeedTryMore) {
      this.logger.info(`Тест '${this.test.description}' упал слишком много раз, ретраев не осталось`)
    }
    return isNeedTryMore
  }

  public failed(): void {
    this.testsRegistry.failed(this.test)
    let isIntermediate = false
    if (this.isNeedTryMore()) {
      isIntermediate = true
    }
    this.setTestResult(false, true, false, isIntermediate)
  }

  public lockAndPrepareAccountData(): XPromise<OAuthUserAccount[]> {
    this.logger.info(`Try to prepare account data for test ${this.test.description}`)
    const min10 = int64(10 * 60 * 1000)
    const min2 = int64(2 * 60 * 1000)
    const requiredAccounts = this.test.requiredAccounts()
    const accounts: OAuthUserAccount[] = []

    // Initially we acquire all locks
    for (const i of range(0, requiredAccounts.length)) {
      const type = requiredAccounts[i]
      const lock = this.userAccountsMap.getAccountByType(type).tryAcquire(min10, min2)
      if (lock === null) {
        // if at least one of the locks can't be acquired, we are done and should release locks acquired before
        this.releaseLocks()
        return reject(new LockAcquireError(type))
      }
      this.locks.push(lock!)
    }

    // Create all builders
    const accountDataPreparers: T[] = []
    for (const accountIndex of range(0, this.locks.length)) {
      const account = this.locks[accountIndex].lockedAccount()
      const preparer = this.preparerProvider.provide(account, requiredAccounts[accountIndex])
      accountDataPreparers.push(preparer)
    }

    this.test.prepareAccounts(accountDataPreparers)
    const promises: XPromise<void>[] = []

    for (const i of range(0, this.locks.length)) {
      const account = this.locks[i].lockedAccount()
      const accountType = requiredAccounts[i]
      let token: Nullable<string>
      try {
        token = this.oauthService.getToken(account, accountType)
      } catch (e) {
        return reject(getYSError(e))
      }
      const oauthAccount = new OAuthUserAccount(account, token, accountType)
      accounts.push(oauthAccount)
      const preparePromise = accountDataPreparers[i].prepare(oauthAccount)
      promises.push(preparePromise)
    }
    this.modelProvider = this.preparerProvider.provideModelDownloader(accountDataPreparers, accounts)
    return all(promises).then((_) => accounts)
  }

  public async runTest(
    accounts: OAuthUserAccount[],
    start: MBTComponent,
    application: Nullable<App>,
  ): Throwing<Promise<void>> {
    this.logger.info(`Test ${this.test.description} started`)
    this.startTime = int64(new Date().getTime())
    this.setTestResult(true, false, false, false)
    this.setInfoForReporter()
    const modelProvider = requireNonNull(this.modelProvider, 'Should lockAndPrepareAccountData before runTest!')
    const model = await modelProvider.takeAppModel()
    const model2 = model.copy()
    const model3 = model.copy()
    const supportedFeatures = (application !== null ? application! : model).supportedFeatures

    const testPlan = this.test.scenario(
      accounts.map((a) => a.account),
      model,
      supportedFeatures,
    )
    this.testPlan = testPlan

    this.logger.info(testPlan.tostring())
    const walkStrategyWithState1 = testPlan.build(null, true)
    const walkStrategyWithState2 = testPlan.build(this.locks, this.assertionsEnabled)

    this.logger.info('Model vs Model testing started')
    if (!this.testsRegistry.isIgnoredVsModelTesting(this.test)) {
      const modelVsModel = new StateMachine(model2, model3, walkStrategyWithState1, this.logger)
      await modelVsModel.go(start)
    }
    this.logger.info('Model vs Model testing finished')
    this.logger.info('\n')

    if (application === null) {
      return
    }

    this.logger.info('Model vs Application testing started')
    const modelVsApplication = new StateMachine(model, application, walkStrategyWithState2, this.logger)
    await modelVsApplication.go(start)
    this.logger.info('Model vs Application testing finished')
    this.logger.info('\n')
    this.logger.info(`Test ${this.test.description} finished`)
    this.testsRegistry.passed(this.test)
    this.setTestResult(false, false, false, false)
  }

  public validateLogs(logs: string): Throwing<void> {
    const testPlan = requireNonNull(this.testPlan, 'Запустите тест runTest сначала!')

    const actualEvents: EventusEvent[] = []
    const lines = logs.split('\n')
    for (const line of lines) {
      if (line.length > 0) {
        const parser = new CrossPlatformLogsParser(this.jsonSerializer)
        const event = parser.parse(line)
        if (!this.testsRegistry.isIgnoredLogEvent(event.name)) {
          actualEvents.push(event)
        }
      }
    }

    if (this.testsRegistry.isIgnoredLogsTesting(this.test)) {
      this.logger.info(`TODO: пожалуйста, почините логирование для теста '${this.test.description}'`)
      return
    }

    const expectedEvents = testPlan.getExpectedEvents()
    const expectedEventsNames = expectedEvents.map((event) => event.name)
    const actualEventsNames = actualEvents.map((event) => event.name)
    const logMessage = `\nОжидаемые события '${expectedEventsNames}'\nПолученные события '${actualEventsNames}'`
    this.logger.info(logMessage)
    for (const i of range(0, maxInt32(actualEvents.length, expectedEvents.length))) {
      if (i >= actualEvents.length) {
        throw new YSError(`Ожидалось событие '${expectedEvents[i].name}', но событий больше нет.'${logMessage}'`)
      }
      const actual = actualEvents[i]
      if (i >= expectedEvents.length) {
        throw new YSError(`Вижу событие '${actual.name}', но больше событий не должно быть.'${logMessage}'`)
      }
      const expected = expectedEvents[i]
      this.logger.info(
        `Событие №${i}: согласно действиям в сценарии должно быть '${expected.name}', в логах '${actual.name}'.'${logMessage}'`,
      )
      assertStringEquals(expected.name, actual.name, `Разные события на месте #${i}.'${logMessage}'\n`)
    }
  }

  public finish(): void {
    this.releaseLocks()
    this.logger.info(`Тест '${this.test.description}' закончился`)
  }

  public sendTestsResults(testNameWhenSend: string): void {
    if (this.statToken === '') {
      this.logger.info(`No token for stat! No statistics will be sent`)
      return
    }
    if (testNameWhenSend === this.test.description) {
      const result: Result<string> = this.network.syncExecuteWithRetries(
        2,
        'https://upload.stat.yandex-team.ru/',
        new StatNetworkRequest(this.testsRegistry.getTestResults()),
        this.statToken,
      )
      if (result.isError()) {
        this.logger.info(`Sending test results is failed with error ${result.getError().message}`)
      }
      if (result.isValue()) {
        this.logger.info(`Sending tests result is succeed, response ${result.getValue()}`)
      }
      this.testsRegistry.clearTestResults()
    } else {
      this.logger.info('Тест не последний в бакете, статистику не отправляем')
    }
  }

  public setInfoForReporter(): void {
    if (this.reporter !== null) {
      this.reporter!.addTestpalmId(this.testsRegistry.getTestSettings(this.test).getCaseIDForPlatform(this.platform))
      const indexOfFeatureEnd: number = this.test.description.indexOf('.')
      if (indexOfFeatureEnd > 0) {
        this.reporter!.addFeatureName(this.test.description.substring(0, indexOfFeatureEnd))
      }
    }
  }

  private releaseLocks(): void {
    if (this.locks.length !== 0) {
      this.locks.forEach((lock) => {
        if (lock !== null) {
          lock.release()
        }
      })
      this.locks = []
      this.logger.info(`Locks for test ${this.test.description} released`)
    }
  }

  private setTestResult(isStarted: boolean, isFailed: boolean, isSkipped: boolean, isIntermediate: boolean): void {
    const execTime: Int32 = int64ToInt32((int64(new Date().getTime()) - this.startTime) / int64(1000))
    const caseId: Int32 = this.testsRegistry.getTestSettings(this.test).getCaseIDForPlatform(this.platform)
    const year = new Date().getFullYear()
    const month = new Date().getMonth() + 1
    const day = new Date().getDate()
    const hours = new Date().getHours()
    const minutes = new Date().getMinutes()
    const seconds = new Date().getSeconds()
    let project = ''
    switch (this.platform) {
      case MBTPlatform.MobileAPI:
        project = 'mob_api'
        break
      case MBTPlatform.Desktop:
        project = 'desktop'
        break
      case MBTPlatform.Android:
        project = 'android'
        break
      case MBTPlatform.IOS:
        project = 'ios'
        break
    }
    this.testsRegistry.setTestResult(
      new MapJSONItem()
        .putString('fielddate', `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`)
        .putString('project', project)
        .putInt32('id', caseId)
        .putString('test_name', this.test.description)
        .putInt32('is_started', isStarted ? 1 : 0)
        .putInt32('is_passed', this.isPassed() ? 1 : 0)
        .putInt32('is_failed', isFailed ? 1 : 0)
        .putInt32('is_skipped', isSkipped ? 1 : 0)
        .putInt32('is_intermediate', isIntermediate ? 1 : 0)
        .putString('execution_time', execTime > 5 ? int32ToString(execTime) : ''),
    )
  }
}

/**
 * Модель еще должна быть копируемой
 */
export interface AppModel extends App {
  copy(): AppModel

  getCurrentStateHash(): Int64
}

export interface AppModelProvider {
  takeAppModel(): Promise<AppModel>
}

export class LockAcquireError extends YSError {
  public constructor(accountType: AccountType2) {
    super(`Can\'t acquire lock for ${accountType}!`)
  }
}
