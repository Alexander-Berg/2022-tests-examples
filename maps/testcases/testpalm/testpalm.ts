import dotenv from 'dotenv';
import Client, { ProjectEnvironment, TestSuite, TestCase, Version } from '@yandex-int/testpalm-api';
import { map, filter, some, includes, toLower } from 'lodash';
import { getBranchName, getBuildTag, getPreparedBranchName } from '../../utils';
import { CreateCmfDataResponseT, OneRecordCompanyDataT } from '../courier-manager/company';
import { GetUsersResponseT, OneRecordUserDataT } from '../tus/tus';
import type { ArgumentsT, TestRunWrapT } from './model';
import { patchTestRun } from './insertOptionToTestcase';

dotenv.config();

const { TESTPALM_OAUTH_TOKEN } = process.env;

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

if (!TESTPALM_OAUTH_TOKEN) {
  throw new Error(
    'TESTPALM_OAUTH_TOKEN not set. Please set TESTPALM_OAUTH_TOKEN environment variable',
  );
}

const branchName = getBranchName({ orElse: process.env.TEST_RUN_BRANCH });
const taskNumber = branchName.match(/\d+/)[0];
const taskLink = `https://st.yandex-team.ru/BBGEO-${taskNumber}`;
const preparedBranchName = getPreparedBranchName(branchName);
const buildTag = getBuildTag(preparedBranchName);

const PROJECT_ID = 'courier';
const TAG_NOT_SUITABLE_FOR_FARM = 'notSuitableForFarm';
const TESTSUITE_ID = '5fda1df8c79113008cf8a0db';
const client = new Client(TESTPALM_OAUTH_TOKEN);

const filterEnvironments = (
  environments: Array<ProjectEnvironment>,
  allowedEnvironments: ArgumentsT['environments'],
): Array<ProjectEnvironment> => {
  return filter(environments, projectEnv => {
    const title = toLower(projectEnv.title);

    return some(allowedEnvironments, environment => {
      return includes(title, environment);
    });
  });
};

export const getTestSuite = async (): Promise<TestSuite> => {
  return client.getTestSuite(PROJECT_ID, TESTSUITE_ID);
};

export const getTestSuiteTestCases = async (): Promise<Array<TestCase>> => {
  const testSuite = await getTestSuite();

  const { filter } = testSuite;

  return client.getTestCases(PROJECT_ID, {
    ...filter,
    expression: filter?.expression ? JSON.stringify(filter.expression) : undefined,
  });
};

export const addTestRuns = async (
  flags: ArgumentsT,
): Promise<{
  version: Version;
  testRuns: Array<TestRunWrapT>;
}> => {
  const project = await client.getProject(PROJECT_ID);
  console.info('project', project);
  const environments = filterEnvironments(project.settings.environments, flags.environments);
  console.info('environments', environments);
  const testSuite = await getTestSuite();
  console.info('testSuite', testSuite);

  const version = await client.addVersion(PROJECT_ID, {
    id: `[robot] Web-acceptance_${buildTag}`,
    description: `<p><a href="${taskLink}">${taskLink}</a></p>`,
  });

  console.info('version', version);

  const testRunsIds = await client.addTestRun(PROJECT_ID, {
    title: `[robot] Web-acceptance ${buildTag}`,
    version: version.id,
    assignee: [],
    currentEnvironment: null,
    environments: map(environments, env => ({
      title: env.title,
      description: env.description ?? env.title,
    })),
    ignoreSuiteOrder: true,
    properties: [],
    tags: [TAG_NOT_SUITABLE_FOR_FARM],
    // @ts-expect-error bad types
    testSuite: {
      id: testSuite.id,
    },
  });
  console.info('testRunsIds', testRunsIds);

  const testRuns = await Promise.all(
    map(testRunsIds, async ({ id }): Promise<TestRunWrapT> => {
      const testRun = await client.getTestRun(PROJECT_ID, id);

      return patchTestRun(testRun);
    }),
  );

  console.info('testRuns', testRuns);

  return {
    version,
    testRuns,
  };
};

const insertDataIntoPreconditions = (
  preconditions: string,
  user: OneRecordUserDataT['user'],
  company: OneRecordCompanyDataT['company'],
): string => {
  return `${preconditions}
----
User
**login**: ${user.login}
**password**: ${user.password}
--
Company
**Company id**: ${company.id}
**Company Name**: ${company.name}
`;
};

export const insertDataIntoPreconditionsFormatted = (
  preconditionsFormatted: string,
  user: OneRecordUserDataT['user'],
  company: OneRecordCompanyDataT['company'],
): string => {
  return `${preconditionsFormatted}
  <p>------------------</p>
  <p>[robot]</p>
  <p><strong>For assessor</strong></p>
  <p>
    - User<br>
    <strong>login</strong>: ${user.login}@yandex.ru<br>
    <strong>password</strong>: ${user.password}<br>
  </p>
  <p>
    - Company<br>
    <strong>Company id</strong>: ${company.id}<br>
    <strong>Company Name</strong>: ${company.name}<br>
  </p>
  `;
};

export const updateTestRuns = async (
  testRuns: Array<TestRunWrapT>,
  users: GetUsersResponseT,
  companies: CreateCmfDataResponseT,
): Promise<void> => {
  await Promise.all(
    map(testRuns, (testRunRaw, index) => {
      const { testGroups } = testRunRaw;
      const testCasesData = testGroups[0];

      const testCases = map(testCasesData.testCases, ({ testCase, ...testCaseData }) => {
        const { uuid } = testCaseData;
        const {
          options: { skipGeneration },
        } = testCase;

        const userData = users[index][uuid];
        const companyData = companies[index][uuid];

        let { preconditions, preconditionsFormatted } = testCase;

        if (!skipGeneration) {
          preconditions = insertDataIntoPreconditions(
            testCase.preconditions,
            userData.user,
            companyData.company,
          );

          preconditionsFormatted = insertDataIntoPreconditionsFormatted(
            testCase.preconditionsFormatted,
            userData.user,
            companyData.company,
          );
        }

        return {
          ...testCaseData,
          testCase: {
            ...testCase,
            preconditions,
            preconditionsFormatted,
          },
        };
      });

      const testRun = {
        ...testRunRaw,
        testGroups: [
          {
            ...testCasesData,
            testCases,
          },
        ],
      };

      return client.updateTestRun(PROJECT_ID, testRun);
    }),
  );
};
