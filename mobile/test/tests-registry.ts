import { Int32, range, undefinedToNull } from '../../../../../common/ys'
import { ArrayJSONItem, MapJSONItem } from '../../../../common/code/json/json-types'
import { Log } from '../../../../common/code/logging/logger'
import { UserAccount } from '../../users/user-pool'
import { getSliceIndexesForBuckets, valuesArray } from '../../utils/utils'
import { FeatureID } from '../mbt-abstractions'
import { DeviceType, MBTPlatform, MBTTest, TestSettings } from './mbt-test'

export enum TestMandatory {
  REGULAR,
  IGNORED,
  DEBUG,
}

export class TestsRegistry<T> {
  private readonly tests: Map<string, MBTTest<T>> = new Map<string, MBTTest<T>>()
  private readonly testMandatories: Map<string, TestMandatory> = new Map<string, TestMandatory>()
  private readonly passedTests: Set<string> = new Set<string>()
  private readonly failedTests: Map<string, Int32> = new Map<string, Int32>()
  private testResults: ArrayJSONItem = new ArrayJSONItem()
  private ignoreLogs: Set<string> = new Set<string>()
  private ignoreLogEvents: Set<string> = new Set<string>()
  private ignoreTestingVsModelTests: Set<string> = new Set<string>()
  private bucketIndex: Int32 = 0
  private bucketsTotal: Int32 = 1
  private retriesCount: Int32 = 0

  public regular(test: MBTTest<T>, testVsModel: boolean = true): TestsRegistry<T> {
    if (!testVsModel) {
      this.ignoreTestingVsModel(test)
    }
    return this.register(test, TestMandatory.REGULAR)
  }

  public regularAll(tests: MBTTest<T>[]): TestsRegistry<T> {
    for (const test of tests) {
      this.regular(test)
    }
    return this
  }

  public debug(test: MBTTest<T>): TestsRegistry<T> {
    return this.register(test, TestMandatory.DEBUG)
  }

  public ignore(test: MBTTest<T>): TestsRegistry<T> {
    return this.register(test, TestMandatory.IGNORED)
  }

  public onlyTestsWithCaseIds(include: boolean, ids: Int32[], platform: MBTPlatform): TestsRegistry<T> {
    const testsToForget = include
      ? this.getTestsExcludingTestsWithCaseIds(ids, platform)
      : this.getTestsByCaseIds(ids, platform)
    for (const test of testsToForget.concat(this.getTestsWithoutId(platform))) {
      this.tests.delete(test.description)
    }
    return this
  }

  public onlyWithTag(tag: DeviceType, platform: MBTPlatform): TestsRegistry<T> {
    const testsToForget: MBTTest<T>[] = []
    for (const test of this.tests.values()) {
      if (!this.getTestSettings(test).hasTag(tag)) {
        testsToForget.push(test)
      }
    }
    for (const test of testsToForget) {
      this.tests.delete(test.description)
    }
    return this
  }

  public bucket(bucketIndex: Int32, totalBuckets: Int32): TestsRegistry<T> {
    this.bucketIndex = bucketIndex
    this.bucketsTotal = totalBuckets
    return this
  }

  public screenOnly(isOnlyScreenTests: boolean): TestsRegistry<T> {
    const testsToForget: MBTTest<T>[] = []
    for (const test of valuesArray(this.tests)) {
      const isTestScreen = test
        .scenario(this.getFakeAccounts(test), null, [])
        .isActionInTestPlan('AssertSnapshotAction')
      if ((isTestScreen && !isOnlyScreenTests) || (!isTestScreen && isOnlyScreenTests)) {
        testsToForget.push(test)
      }
    }
    for (const test of testsToForget) {
      this.tests.delete(test.description)
    }
    return this
  }

  public ignoreLogsTesting(test: MBTTest<T>): TestsRegistry<T> {
    this.ignoreLogs.add(test.description)
    return this
  }

  public ignoreLogEvent(eventName: string): TestsRegistry<T> {
    this.ignoreLogEvents.add(eventName)
    return this
  }

  public ignoreTestingVsModel(test: MBTTest<T>): TestsRegistry<T> {
    this.ignoreTestingVsModelTests.add(test.description)
    return this
  }

  public retries(count: Int32): TestsRegistry<T> {
    this.retriesCount = count
    return this
  }

  public getTestsPossibleToRun(
    platform: MBTPlatform,
    modelFeatures: FeatureID[],
    applicationFeatures: FeatureID[],
  ): MBTTest<T>[] {
    const result: MBTTest<T>[] = []
    const testsToDebug = this.getTests(TestMandatory.DEBUG)
    if (testsToDebug.length > 0) {
      for (const test of testsToDebug) {
        for (const _ of range(0, 1 + this.retriesCount)) {
          result.push(test)
        }
      }
      return result
    }
    for (const test of this.getTestBucket(platform, modelFeatures, applicationFeatures)) {
      for (const _ of range(0, 1 + this.retriesCount)) {
        result.push(test)
      }
    }
    return result
  }

  public passed(test: MBTTest<T>): void {
    this.passedTests.add(test.description)
  }

  public failed(test: MBTTest<T>): void {
    if (this.failedTests.has(test.description)) {
      this.failedTests.set(test.description, this.failedTests.get(test.description)! + 1)
    } else {
      this.failedTests.set(test.description, 1)
    }
    if (this.passedTests.has(test.description)) {
      this.passedTests.delete(test.description)
    }
  }

  public isPassed(test: MBTTest<T>): boolean {
    return this.passedTests.has(test.description)
  }

  public isNeedTryMore(test: MBTTest<T>): boolean {
    const failedNum = this.failedTests.get(test.description)
    if (undefinedToNull(failedNum) !== null) {
      return this.failedTests.get(test.description)! <= this.retriesCount
    }
    return true
  }

  public isTestEnabled(
    test: MBTTest<T>,
    platform: MBTPlatform,
    modelFeatures: FeatureID[],
    applicationFeatures: FeatureID[],
  ): boolean {
    if (this.isTestToDebug(test)) {
      return true
    }
    if (this.isIgnored(test)) {
      return false
    }
    const fakeAccounts = this.getFakeAccounts(test)
    const unsupportedActions = test
      .scenario(fakeAccounts, null, applicationFeatures)
      .unsupportedActions(modelFeatures, applicationFeatures)
    if (unsupportedActions.length > 0) {
      let s = ''
      for (const action of unsupportedActions) {
        s += `;${action.tostring()}`
      }
      Log.info(`'${test.description}': application should support actions: ${s}`)
    }
    const settings = this.getTestSettings(test)
    const ignored = settings.isIgnored(platform)
    if (ignored) {
      Log.info(
        `'${test.description}': помечен как нерабочий. ` +
          'Пожалуйста, почините это - тесты должны включаться/отключаться через поддержку фичей',
      )
    }
    return unsupportedActions.length === 0 && !ignored
  }

  public isTestToDebug(test: MBTTest<T>): boolean {
    return this.testMandatories.get(test.description) === TestMandatory.DEBUG
  }

  public isIgnored(test: MBTTest<T>): boolean {
    return this.testMandatories.get(test.description) === TestMandatory.IGNORED
  }

  public isIgnoredLogsTesting(test: MBTTest<T>): boolean {
    return this.ignoreLogs.has(test.description)
  }

  public isIgnoredLogEvent(eventName: string): boolean {
    return this.ignoreLogEvents.has(eventName)
  }

  public isIgnoredVsModelTesting(test: MBTTest<T>): boolean {
    return this.ignoreTestingVsModelTests.has(test.description)
  }

  public getTestSettings(test: MBTTest<T>): TestSettings {
    const settings = new TestSettings()
    test.setupSettings(settings)
    return settings
  }

  public setTestResult(result: MapJSONItem): void {
    this.testResults.add(result)
  }

  public getTestResults(): ArrayJSONItem {
    return this.testResults
  }

  public clearTestResults(): void {
    this.testResults = new ArrayJSONItem()
  }

  private getTestBucket(
    platform: MBTPlatform,
    modelFeatures: FeatureID[],
    applicationFeatures: FeatureID[],
  ): MBTTest<T>[] {
    const ignoredTests: MBTTest<T>[] = valuesArray(this.tests)
      .filter((item) => !this.isTestEnabled(item, platform, modelFeatures, applicationFeatures))
      .sort((l, r) => (l.description < r.description ? -1 : l.description === r.description ? 0 : 1))
    const testsBucket: MBTTest<T>[] = valuesArray(this.tests)
      .filter((item) => !ignoredTests.map((test) => test.description).includes(item.description))
      .sort((l, r) => (l.description < r.description ? -1 : l.description === r.description ? 0 : 1))
    let runTestsCount: Int32 = testsBucket.length
    let ignoredTestsCount: Int32 = ignoredTests.length
    Log.info(
      `${runTestsCount} tests total to run, ${ignoredTestsCount} tests total to ignore, ${this.bucketsTotal} buckets`,
    )
    const ignoredBucketSliceIndexes = getSliceIndexesForBuckets(ignoredTestsCount, this.bucketsTotal)
    const bucketSliceIndexes = getSliceIndexesForBuckets(runTestsCount, this.bucketsTotal)
    const testsToRun = testsBucket.slice(bucketSliceIndexes[this.bucketIndex], bucketSliceIndexes[this.bucketIndex + 1])
    const testsToIgnore = ignoredTests.slice(
      ignoredBucketSliceIndexes[this.bucketIndex],
      ignoredBucketSliceIndexes[this.bucketIndex + 1],
    )
    runTestsCount = testsToRun.length
    ignoredTestsCount = testsToIgnore.length
    Log.info(`${testsToRun}`)
    Log.info(`${runTestsCount} tests to run, ${ignoredTestsCount} test to ignore in current bucket`)
    return testsToRun.concat(testsToIgnore)
  }

  private register(test: MBTTest<T>, mandatory: TestMandatory): TestsRegistry<T> {
    this.tests.set(test.description, test)
    this.testMandatories.set(test.description, mandatory)
    return this
  }

  private getTests(mandatory: TestMandatory): MBTTest<T>[] {
    const tests: MBTTest<T>[] = []
    for (const test of this.tests.values()) {
      if (mandatory === this.testMandatories.get(test.description)) {
        tests.push(test)
      }
    }
    return tests
  }

  private getTestsExcludingTestsWithCaseIds(ids: Int32[], platform: MBTPlatform): MBTTest<T>[] {
    const tests: MBTTest<T>[] = []
    for (const test of this.tests.values()) {
      if (!ids.includes(this.getTestSettings(test).getCaseIDForPlatform(platform))) {
        tests.push(test)
      }
    }
    return tests
  }

  private getTestsByCaseIds(ids: Int32[], platform: MBTPlatform): MBTTest<T>[] {
    const tests: MBTTest<T>[] = []
    for (const test of this.tests.values()) {
      if (ids.includes(this.getTestSettings(test).getCaseIDForPlatform(platform))) {
        tests.push(test)
      }
    }
    return tests
  }

  private getTestsWithoutId(platform: MBTPlatform): MBTTest<T>[] {
    const tests: MBTTest<T>[] = []
    for (const test of this.tests.values()) {
      if (this.getTestSettings(test).getCaseIDForPlatform(platform) === 0) {
        tests.push(test)
      }
    }
    return tests
  }

  private getFakeAccounts(test: MBTTest<T>): UserAccount[] {
    return test.requiredAccounts().map((_) => new UserAccount('', ''))
  }
}
