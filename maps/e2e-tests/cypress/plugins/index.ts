// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

import * as fs from 'fs';
import resolve from 'resolve';
import { TUSClient } from '@yandex-int/tus-client';
import { SUPER_USER_LOGIN } from '../../src/constants/accounts';
import tokenator from '@yandex-int/tokenator';
import { schemes } from './schemes';
import { runGenerateDataBySchema, runRemoveData } from 'generating-data/data-generator';

const readXlsxFile = require('read-excel-file/node');

const TUS_CONSUMER = 'ya-courier-frontend';
const TUS_ENV = 'prod';

const plugins = (on: any, config: Cypress.ResolvedConfigOptions): Cypress.ResolvedConfigOptions => {
  // todo is it necessary?
  config.env = {
    ...config.env,
    ...process.env,
  };

  const options = {
    typescript: resolve.sync('typescript', { baseDir: (config as any).projectRoot } as any),
  };

  on('task', {
    readFileMaybe(filename: string) {
      if (fs.existsSync(filename)) {
        const contents = fs.readFileSync(filename, 'utf8');
        return contents ? JSON.parse(contents) : {};
      }
      return {};
    },
    async tusGetAuthData(login: string) {
      if (login === SUPER_USER_LOGIN) {
        const { E2E_TEST_USER_PASSWORD } = process.env;

        return Promise.resolve({
          account: {
            login,
            password: E2E_TEST_USER_PASSWORD,
          },
        });
      }
      const { tus: TUS_OAUTH_TOKEN } = await tokenator('tus');

      const client = new TUSClient(TUS_OAUTH_TOKEN, { tus_consumer: TUS_CONSUMER, env: TUS_ENV });

      try {
        const data = await client.getAccount({ login });

        return data;
      } catch (e) {
        const data = await client.createPortalAccount({ login });

        return data;
      }
    },
  });

  on('task', {
    deleteFolder(folderName: string) {
      console.log('deleting folder %s', folderName);

      return new Promise((resolve, reject) => {
        fs.rmdir(folderName, { maxRetries: 10, recursive: true }, err => {
          if (err && err.code !== 'ENOENT') {
            console.error(err);

            return reject(err);
          }

          resolve(null);
        });
      });
    },
    readExcelFile(filename: string) {
      return readXlsxFile(filename);
    },

    makeData(dataKey: GenerateDataTypes) {
      const scheme = schemes[dataKey];

      return new Promise(async (res, rej) => {
        try {
          await runRemoveData(scheme);
          await runGenerateDataBySchema(scheme);
          res(true);
        } catch (error) {
          rej(error);
        }

        res(true);
      });
    },

    removeData(dataKey: GenerateDataTypes) {
      const scheme = schemes[dataKey];

      return new Promise(async (res, rej) => {
        try {
          await runRemoveData(scheme);
          res(true);
        } catch (error) {
          rej(error);
        }
      });
    },
  });

  return config;
};

export default plugins;
