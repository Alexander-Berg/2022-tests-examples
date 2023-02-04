import app from 'app/app';
import nock from 'nock';
import {appPort} from './constants';
import {testRequest as request} from './test-request';
import {logger} from 'app/lib/logger';
import db from 'app/lib/db-client';
import {wait} from 'utils/promise-utils';
import chai from 'chai';
import setupChai from './setup-chai';

const server = app.listen(appPort);

async function mochaGlobalSetup(): Promise<void> {
    console.time('mochaSetup');
    setupChai(chai);

    let isServerReady = false;

    while (!isServerReady) {
        await request('ping')
            .then((res) => res.statusCode)
            .catch(() => 500)
            .then((statusCode) => {
                if (statusCode !== 200) {
                    logger.error(`Server is not ready. GET /ping -> ${statusCode}`);
                    return wait(500);
                }
                isServerReady = true;
            });
    }

    logger.info(`Server is listening on port ${appPort}`);

    logger.info('Disabling net...');

    nock.disableNetConnect();
    nock.enableNetConnect('localhost');

    logger.info('Done!');
    console.timeEnd('mochaSetup');
}

async function mochaGlobalTeardown(): Promise<void> {
    await new Promise((res, rej) => server.close((err: unknown) => (err ? rej(err) : res(null))));
    await db.terminate();
}

export {mochaGlobalSetup, mochaGlobalTeardown};
