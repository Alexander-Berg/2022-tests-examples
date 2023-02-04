import { TestRun, TestRunTestGroup, RunTestCase, TestCase } from '@yandex-int/testpalm-api';

export type ArgumentsT = {
  environments: Array<string>;
};

export type TestCaseOptionsT = {
  skipGeneration?: boolean;
  useTestCompany?: boolean;
  userRole?: 'admin' | 'manager' | 'dispatcher';
};

export type TestCaseWrapT = TestCase & {
  options: TestCaseOptionsT;
};

export type RunTestCaseWrapT = RunTestCase & {
  testCase: TestCaseWrapT;
};
export type TestRunTestGroupWrapT = TestRunTestGroup & {
  testCases: Array<RunTestCaseWrapT>;
};
export type TestRunWrapT = TestRun & {
  testGroups: Array<TestRunTestGroupWrapT>;
};
