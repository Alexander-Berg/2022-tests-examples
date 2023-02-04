import * as path from 'path';
import {Config, Engine} from '@yandex-int/tests-to-testpalm/out/types';
import {getBrowserAttributesStub} from './browser-stubs/browser-attributes-stub';
import {getBrowserDescriptionStub} from './browser-stubs/browser-description-stub';
import {getBrowserEstimateStub} from './browser-stubs/browser-estimate-stub';
import {getBrowserPreconditionsStub} from './browser-stubs/browser-preconditions-stub';
import {Platform} from './types';
import selectors from '../../tests/func/common/selectors-description';

const platform = (process.env.PLATFORM as Platform) || Platform.DESKTOP;

const config: Config = {
    testPalm: {
        projectId: 'agency_cabinet-test',
        token: process.env.TESTPALM_OAUTH_TOKEN!
    },
    filePatterns: [path.join(__dirname, '../../tests/func/**/*.test.ts')],
    browserId: 'chrome',
    getComponentByFile: (filePath) => {
        const filename = path.parse(filePath).name;

        return filename.replace(/\.?test\.?$/gi, '');
    },
    browserStubs: {
        attributes: getBrowserAttributesStub(platform),
        description: getBrowserDescriptionStub(platform),
        estimate: getBrowserEstimateStub(platform),
        preconditions: getBrowserPreconditionsStub(platform)
    },
    // Минимальное время выполнения кейса асессорами - 2 мин
    minEstimate: 2 * 60 * 1000,
    getSelectorsDescriptions: () => selectors,
    getTicket: () => Promise.resolve(''),
    report: {
        folder: path.join(__dirname, '../../reports/sync-testcases'),
        name: `output-${platform}`
    },
    testcop: {
        project: 'agency-cabinet',
        tool: 'pup',
        branch: 'dev'
    },
    engine: Engine.PUPPETEER,
    trackerQueue: 'GEODISPLAY'
};

export default config;
