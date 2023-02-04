import * as url from 'url';
import {Config} from '@yandex-int/pup';

const defaultConfig: Config = {
    retryTimes: 1,
    timeout: 30000,
    roots: ['./tests/'],
    testMatch: ['./tests/func/tests/**/*.test.ts?(x)'],
    reportPath: './tests/func/reports/index.html',
    staticMocks: [],
    dynamicMocks: [
        {
            match: (req): boolean => req.url().includes('/geoadv/agency/api/'),
            buildKey: (req): string => {
                const reqUrl = req.url();
                const postData = req.postData();
                const body = postData ? {...JSON.parse(postData), csrfToken: undefined} : postData;
                const parsedUrl = new url.URL(reqUrl);

                return `${parsedUrl.pathname}${parsedUrl.search}${JSON.stringify(body)}`;
            }
        }
    ],
    setupFilesAfterEnv: ['./node_modules/@yandex-int/tests-to-testpalm/out/puppeteer/setup.js']
};

export default defaultConfig;
