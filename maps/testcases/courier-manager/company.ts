import dotenv from 'dotenv';
import { Dictionary, filter, includes, keyBy, map } from 'lodash';
import { BrowserEnum } from '../common/constants';
import { checkIsTestCompanyName, getCompanyName } from '../common/getCompanyName';
import { getLogin, isTestLogin } from '../common/getLogin';
import type { TestCase } from '@yandex-int/testpalm-api';
import { getBrowserByEnvTitle } from '../common/getBrowserByEnv';
import { ArgumentsT, TestRunWrapT } from '../testpalm/model';
import { CourierClient, fetchByChunk } from 'generate-data';

dotenv.config();
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

type CompanyT = {
  id: number;
  name: string;
};

const { B2BGEO_COURIER_API_TOKEN, B2BGEO_COURIER_API_KEY } = process.env;
const TEST_COMPANY_ID = 598;

const B2BGEO_COURIER_API_URL = 'https://courier.yandex.ru/api/v1';

if (!B2BGEO_COURIER_API_TOKEN) {
  throw new Error(
    'B2BGEO_COURIER_API_TOKEN not set. Please set B2BGEO_COURIER_API_TOKEN environment variable',
  );
}
if (!B2BGEO_COURIER_API_KEY) {
  throw new Error(
    'B2BGEO_COURIER_API_KEY not set. Please set B2BGEO_COURIER_API_KEY environment variable',
  );
}

const client = new CourierClient(B2BGEO_COURIER_API_TOKEN, {
  apiUrl: B2BGEO_COURIER_API_URL,
  apiKey: B2BGEO_COURIER_API_KEY,
});

const deleteTestCompanies = async (): Promise<void> => {
  const companies = await client.getCompanies();
  const testCompanies = filter(companies, ({ name }) => checkIsTestCompanyName(name));

  console.info('deleteTestCompanies start');
  await fetchByChunk(testCompanies, client.deleteCompanyWithDeps, {
    comment: 'deleteOldTestCompanies deleteCompanyWithDeps',
  });
};

const deleteManagersFromTestCompanies = async (): Promise<void> => {
  const companyData = {
    id: TEST_COMPANY_ID,
    name: 'test apple',
  };
  const allUsers = await client.getUsers(companyData);
  const testUsers = filter(allUsers, user => isTestLogin(user.login));

  console.info('deleteManagersFromTestCompanies start');
  await client.deleteUsers(companyData, testUsers);
  console.info('deleteManagersFromTestCompanies complete');
};

const generateCompanyData = async (id: number, env: BrowserEnum): Promise<CompanyT> => {
  const companyName = getCompanyName(id, env);
  const initialLogin = getLogin(id, env);

  await client.deleteCompanyWithDeps({ name: companyName });

  const createdCompany = await client.createCompany({
    name: companyName,
    initial_login: initialLogin,
    services: [
      {
        enabled: true,
        name: 'courier',
      },
      {
        enabled: true,
        name: 'mvrp',
      },
    ],
  });

  return createdCompany;
};

const addManagerToTestCompany = async (
  id: number,
  env: BrowserEnum,
  allDepotsIds: Array<number>,
  role: 'admin' | 'manager' | 'dispatcher' = 'manager',
): Promise<CompanyT> => {
  const initialLogin = getLogin(id, env);

  const company = await client.getCompanyById(TEST_COMPANY_ID);
  if (!company) {
    throw new Error('null test company');
  }

  const user = await client.createUser(TEST_COMPANY_ID, {
    login: initialLogin,
    role,
  });

  await client.addDepotsToUser(TEST_COMPANY_ID, user.id, allDepotsIds);

  return company;
};

export type OneRecordCompanyDataT = {
  uuid: string;
  company: CompanyT | null;
};

export type CreateCmfDataResponseT = Array<Dictionary<OneRecordCompanyDataT>>;

export const createCmfData = async (
  testRuns: Array<TestRunWrapT>,
): Promise<CreateCmfDataResponseT> => {
  const allDepotsList = await client.getCompanyDepots({ id: TEST_COMPANY_ID, name: 'test' });
  const allDepotsIds = map(allDepotsList, depot => depot.id);

  const data = await Promise.all(
    map(testRuns, async data => {
      const { currentEnvironment } = data;
      const browser = getBrowserByEnvTitle(currentEnvironment.title);

      const results = await fetchByChunk(
        data.testGroups[0].testCases,
        async ({ uuid, testCase: { id, options } }): Promise<OneRecordCompanyDataT> => {
          const { skipGeneration, useTestCompany, userRole } = options;
          if (skipGeneration) {
            return null;
          }

          if (useTestCompany) {
            const company = await addManagerToTestCompany(id, browser, allDepotsIds, userRole);

            return {
              uuid,
              company,
            };
          }

          const company = await generateCompanyData(id, browser);

          return {
            uuid,
            company,
          };
        },
        {
          comment: 'createCompanies generateCompanyData',
        },
      );

      const filteredResults = filter(results, Boolean) as Array<OneRecordCompanyDataT>;

      return keyBy(filteredResults, data => data.uuid);
    }),
  );

  return data;
};

export const removeCompanies = async (data: Array<TestCase>, flags: ArgumentsT): Promise<void> => {
  await deleteManagersFromTestCompanies();
  await deleteTestCompanies();
};
