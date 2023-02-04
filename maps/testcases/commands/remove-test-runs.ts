import { removeCompanies } from '../courier-manager/company';
import { getTestSuiteTestCases } from '../testpalm/testpalm';
import { ArgumentsT } from '../testpalm/model';

export const removeTestRuns = async (flags: ArgumentsT): Promise<void> => {
  const testCases = await getTestSuiteTestCases();

  await removeCompanies(testCases, flags);
};
