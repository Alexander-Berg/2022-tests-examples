import { map } from 'lodash';
import { getTestCaseOptions } from './getTestCaseOptions';
import type { RunTestCase, TestCase, TestRun, TestRunTestGroup } from '@yandex-int/testpalm-api';
import type { RunTestCaseWrapT, TestCaseWrapT, TestRunTestGroupWrapT, TestRunWrapT } from './model';

export const patchTestCase = (testCase: TestCase): TestCaseWrapT => {
  return {
    ...testCase,
    options: getTestCaseOptions(testCase.automation ?? ''),
  };
};

export const patchRunTestCase = (runTestCase: RunTestCase): RunTestCaseWrapT => {
  return {
    ...runTestCase,
    testCase: patchTestCase(runTestCase.testCase),
  };
};

export const patchTestCases = (testCases: Array<RunTestCase>): Array<RunTestCaseWrapT> => {
  return map(testCases, patchRunTestCase);
};

export const patchTestGroup = (testGroup: TestRunTestGroup): TestRunTestGroupWrapT => {
  return {
    ...testGroup,
    testCases: patchTestCases(testGroup.testCases),
  };
};

export const patchTestGroups = (
  testGroups: Array<TestRunTestGroup>,
): Array<TestRunTestGroupWrapT> => {
  return map(testGroups, patchTestGroup);
};

export const patchTestRun = (testRun: TestRun): TestRunWrapT => {
  return {
    ...testRun,
    testGroups: patchTestGroups(testRun.testGroups),
  };
};
