import fs from 'fs';
import moment from 'moment';
import { SchemaGenerator, fetchByChunk } from 'generate-data';
import { TUSClient } from '@yandex-int/tus-client';

import { getExampleTasksHashes } from './request/example-tasks';
import { depotNameRecord, depotNumberRecord } from 'constants/depots';
import { courierNameRecord, courierNumberRecord } from 'constants/couriers';
import { values, forEach, map } from 'lodash';
import { routeNumberRecord, sharedRouteNumberRecord } from 'constants/routes';
import { getAccounts, SUPER_USER_LOGIN } from 'constants/accounts';
import { companyNameRecord, additionalCompaniesNames } from 'constants/companies';
import { writeToFixture, readFromFixture } from './common';
import selectors from 'constants/selectors';
import { CompanySlugEnum, CommonCompanySlugEnum } from './schema';
import tokenator from '@yandex-int/tokenator';
import { CompanyT, UserT, GenerateSchemaT, OneCompanyGenerateSchemaT } from 'generate-data';

const { E2E_TESTS_API_TOKEN, API_URL, API_KEY } = process.env;
const TUS_CONSUMER = 'ya-courier-frontend';
const TUS_ENV = 'prod';

if (!E2E_TESTS_API_TOKEN) {
  throw new Error('Api token not set. Please set E2E_TESTS_API_TOKEN environment variable');
}
if (!API_URL) {
  throw new Error('Api url not set. Please set API_URL environment variable');
}
if (!API_KEY) {
  throw new Error('Api key not set. Please set API_KEY environment variable');
}

const localDataPathname = './company-data.json';
const accounts = getAccounts();

const client = new SchemaGenerator(E2E_TESTS_API_TOKEN, { apiUrl: API_URL, apiKey: API_KEY });

function constructLocalData(hashes: Record<string, string>): void {
  fs.writeFileSync('./example-task-hashes.json', JSON.stringify(hashes, null, 2));

  const parentExampleTaskHashes = JSON.parse(
    fs.readFileSync('../example-task-hashes.json', 'utf8'),
  );
  writeToFixture('parent-example-task-hashes.json', parentExampleTaskHashes);

  writeToFixture('example-task-hashes.json', hashes);
}

async function generateAdditionalData(): Promise<void> {
  const tasksHashes = await getExampleTasksHashes();
  constructLocalData(tasksHashes);
}

export const createUsers = async (
  companyId: number,
  companyRecord: Record<CompanySlugEnum, CompanyT>,
  newUsers: OneCompanyGenerateSchemaT<CompanySlugEnum>['users'],
): Promise<Array<UserT>> => {
  const { tus: TUS_OAUTH_TOKEN } = await tokenator('tus');
  const tusClient = new TUSClient(TUS_OAUTH_TOKEN, { tus_consumer: TUS_CONSUMER, env: TUS_ENV });

  const users = await fetchByChunk(
    newUsers,
    async ({ data, sharedToCompany }) => {
      const user = await client.createUser(companyId, data);

      if (data.login.match(/^yndx-/) && user.login !== SUPER_USER_LOGIN) {
        try {
          await tusClient.createPortalAccount({ login: data.login });
        } catch (e) {
          await tusClient.getAccount({ login: data.login });
        }
      }

      await fetchByChunk(
        sharedToCompany,
        companySlug =>
          client.editUserSharedCompanies(companyId, user.id, [
            companyId,
            companyRecord[companySlug].id,
          ]),
        {
          comment: 'editUserSharedCompanies',
          chunkSize: 50,
        },
      );

      return user;
    },
    {
      chunkSize: 50,
      comment: 'createUsers',
    },
  );

  return users;
};

const bindPhone = async (uid: string): Promise<void> => {
  const { tus: TUS_OAUTH_TOKEN } = await tokenator('tus');
  const tusClient = new TUSClient(TUS_OAUTH_TOKEN, { tus_consumer: TUS_CONSUMER, env: TUS_ENV });
  try {
    await tusClient.bindPhone({ uid, phone_number: '+70001112233' });
  } catch (e) {
    console.info(e);
  }
};

const createTusAccounts = async (users: Record<string, UserT>): Promise<void> => {
  const { tus: TUS_OAUTH_TOKEN } = await tokenator('tus');
  const tusClient = new TUSClient(TUS_OAUTH_TOKEN, { tus_consumer: TUS_CONSUMER, env: TUS_ENV });

  await fetchByChunk(
    values(users),
    async ({ login }) => {
      if (login.match(/^yndx-/) && login !== SUPER_USER_LOGIN) {
        let uid = null;

        try {
          const newUser = await tusClient.createPortalAccount({ login });
          uid = newUser.account.uid;
        } catch (e) {
          const userData = await tusClient.getAccount({ login });
          uid = userData.account.uid;
        }
        await bindPhone(uid);
      }
    },
    {
      comment: 'createTusAccounts',
    },
  );
};

type OptionsT = {
  onlyForNewCompanies: boolean;
};
const generateDataBySchema = async (
  schema: GenerateSchemaT<string>,
  options?: OptionsT,
): Promise<void> => {
  const companiesData = await client.generateDataBySchema(schema, options);

  await fetchByChunk(
    values(companiesData),
    async ({ users }) => {
      await createTusAccounts(users);
    },
    {
      comment: 'generateDataBySchema tusUsersByCompany',
    },
  );

  const localCompaniesData: Record<string, CompanyDataT> = {};
  forEach(companiesData, ({ company, couriers }, slug) => {
    localCompaniesData[slug] = {
      companyId: company.id,
      date: moment().format('YYYY-MM-DD'),
      companyName: company.name,
      couriers: map(Object.keys(couriers), k => couriers[k]),
    };
  });

  let existData: Record<string, CompanyDataT> = {
    ...localCompaniesData,
  };

  try {
    const existCompaniesData = readFromFixture('company-data.json');
    existData = {
      ...existCompaniesData,
      ...existData,
    };
  } catch {
    console.info('file not exist');
  }
  fs.writeFileSync(localDataPathname, JSON.stringify(existData, null, 2));
  writeToFixture('company-data.json', existData);
};

export const runGenerateDataBySchema = async (
  schema: GenerateSchemaT<string>,
  checkOnExistSchema?: GenerateSchemaT<CommonCompanySlugEnum>,
): Promise<void> => {
  try {
    console.time('generateData by schema');
    await generateDataBySchema(schema);
    if (checkOnExistSchema) {
      await generateDataBySchema(checkOnExistSchema, { onlyForNewCompanies: true });
    }
    await generateAdditionalData();
    console.info('------------------------------------------');
    console.info('Baseline data generation completed!');
    console.timeEnd('generateData by schema');
    console.info('------------------------------------------');
    console.info();
  } catch (error) {
    console.error(error);
    process.exit(1);
  }
};

const removeData = async (schema: GenerateSchemaT<string>): Promise<void> => {
  const entriesSchema = Object.entries(schema) as Array<
    [CompanySlugEnum, OneCompanyGenerateSchemaT<CompanySlugEnum>]
  >;
  await fetchByChunk(
    entriesSchema,
    ([
      ,
      {
        data: { name },
      },
    ]) => client.deleteCompanyWithDeps({ name }),
    {
      comment: 'removeData deleteCompanyWithDeps',
    },
  );
};

export const runRemoveData = async (schema: GenerateSchemaT<string>): Promise<void> => {
  try {
    console.time('removeData');
    await removeData(schema);
    console.info('------------------------------------------');
    console.info('Deletion of basic data is complete!');
    console.timeEnd('removeData');
    console.info('------------------------------------------');
    console.info();
  } catch (error) {
    console.error(error);
    process.exit(1);
  }
};

export const writeTestData = async (): Promise<void> => {
  const testData = {
    accounts,
    companyNameRecord,
    additionalCompaniesNames,
    courierNameRecord,
    courierNumberRecord,
    depotNameRecord,
    depotNumberRecord,
    routeNumberRecord,
    sharedRouteNumberRecord,
    selectors,
  };

  writeToFixture('testData.json', testData);
};
