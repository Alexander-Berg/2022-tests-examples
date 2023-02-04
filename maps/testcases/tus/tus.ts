import dotenv from 'dotenv';
import { TUSClient } from '@yandex-int/tus-client';
import { keyBy, pick, Dictionary, map, filter } from 'lodash';
import { getLogin } from '../common/getLogin';
import { BrowserEnum } from '../common/constants';
import { getBrowserByEnvTitle } from '../common/getBrowserByEnv';
import { TestRunWrapT } from '../testpalm/model';
import { fetchByChunk } from 'generate-data';

dotenv.config();
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

type UserDataT = {
  login: string;
  password: string;
  uid: string;
};

const { TUS_OAUTH_TOKEN } = process.env;
if (!TUS_OAUTH_TOKEN) {
  throw new Error('TUS_OAUTH_TOKEN not set. Please set TUS_OAUTH_TOKEN environment variable');
}

const TUS_CONSUMER = 'ya-courier-frontend';
const TUS_ENV = 'prod';

const client = new TUSClient(TUS_OAUTH_TOKEN, { tus_consumer: TUS_CONSUMER, env: TUS_ENV });

const getUser = async (prefix: number, postfix: BrowserEnum): Promise<UserDataT> => {
  const name = getLogin(prefix, postfix);
  try {
    const userData = await client.getAccount({ login: name });
    console.info('get user', name);

    return pick(userData.account, ['login', 'password', 'uid']);
  } catch {
    const userData = await client.createPortalAccount({
      login: name,
    });
    console.info('create user', name);

    return pick(userData.account, ['login', 'password', 'uid']);
  }
};

export type OneRecordUserDataT = {
  uuid: string;
  user: UserDataT;
};

export type GetUsersResponseT = Array<Dictionary<OneRecordUserDataT>>;

export const getUsers = async (testRuns: Array<TestRunWrapT>): Promise<GetUsersResponseT> => {
  const data = await Promise.all(
    map(testRuns, async data => {
      const { currentEnvironment } = data;
      const browser = getBrowserByEnvTitle(currentEnvironment.title);

      const results = await fetchByChunk(
        data.testGroups[0].testCases,
        async ({ uuid, testCase: { id, options } }): Promise<OneRecordUserDataT | null> => {
          const { skipGeneration } = options;

          if (skipGeneration) {
            return null;
          }

          const user = await getUser(id, browser);

          return {
            uuid,
            user,
          };
        },
        {
          comment: 'getUser',
        },
      );

      const filteredResults = filter(results, Boolean) as Array<OneRecordUserDataT>;

      return keyBy(filteredResults, data => data.uuid);
    }),
  );

  return data;
};
