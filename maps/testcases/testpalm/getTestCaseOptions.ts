import { TestCaseOptionsT } from './model';

export const getTestCaseOptions = (automation: string): TestCaseOptionsT => {
  if (!automation) {
    return {};
  }

  try {
    return JSON.parse(automation) as TestCaseOptionsT;
  } catch (error) {
    return {};
  }
};
