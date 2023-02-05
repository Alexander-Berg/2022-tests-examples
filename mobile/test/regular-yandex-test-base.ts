import { AccountDataPreparer } from './account-data-preparer'
import { AccountType2, TestSuite } from './mbt-test'
import { RegularTestBase } from './regular-test-base'

export abstract class RegularYandexTestBase<T extends AccountDataPreparer> extends RegularTestBase<T> {
  protected constructor(description: string, suite: TestSuite[] = [TestSuite.Fixed]) {
    super(description, AccountType2.Yandex, suite)
  }
}
