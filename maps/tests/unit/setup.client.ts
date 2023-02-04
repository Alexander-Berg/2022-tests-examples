import {ClientValuesConfig, ClientConfig, ClientValuesCampaignConfig} from '../../src/common/general-types';
import {TopLevelDomain} from '../../src/common/top-level-domain';
import {Locale, Language, Currency, Location} from '../../src/common/locale';
import {Role} from '../../src/common/enums';
import {MediaplanProductType} from '../../src/common/mediaplan-types';

const APP_BASE_PATH = '/geoadv/agency';

const mockClientConfig: ClientConfig = {
    version: '0.0.0',
    tld: TopLevelDomain.RU,
    locale: Locale.RU,
    lang: Language.RU,
    defaultCurrency: Currency.RUB,
    defaultLocation: Location.MOSCOW,
    safeOriginalUrl: 'stub',
    appBasePath: APP_BASE_PATH,
    apiBasePath: `${APP_BASE_PATH}/api`,
    downloadBasePath: `${APP_BASE_PATH}/downloads`,
    needDevTools: false,
    security: {
        csrfToken: '12345',
        nonce: 'XAdstAio=='
    },
    auth: {
        user: {
            login: 'yndx-ya',
            userName: 'Вася Пупкин',
            uid: '987654321',
            email: 'yndx-ya@yandex.ru',
            avatarId: '0/0-0',
            normalizedLogin: 'yndx-ya'
        }
    },
    whoami: {
        email: 'yndx-ya',
        globalRole: Role.ACCOUNT,
        companies: undefined
    },
    companies: [
        {
            id: '-1',
            name: 'Яндекс.Гео',
            hasAcceptedOffer: true,
            isAgency: true
        }
    ],
    metrika: {
        counterId: 123
    },
    clientValues: {} as ClientValuesConfig,
    featureFlagsArray: [],
    featureFlagsSet: new Set(),
    mapsApiUrl: 'stub',
    mapsApiAreaUrl: 'stub',
    requestIdHeader: 'x-request-id',
    skipRefillLimit: true,
    announcement: {
        expirationDate: '2019-03-19T00:00:00+03:00',
        showHeader: false,
        hasDetails: true
    },
    shouldDisableRubricAuto: true,
    mediaplanProductRate: {} as Record<MediaplanProductType, Record<Currency, number>>,
    uploadApiUrl: 'stub',
    avatarsMdsUrl: 'stub',
    passportUrl: 'stub',
    features: {},
    accounts: [],
    avatarsUrl: 'stub',
    frontendErrorCounterProject: 'geoadv-agency-frontend-testing',
    frontendErrorCounterEnvironment: 'development'
};

jest.mock('lib/config', () => ({
    config: mockClientConfig,
    getCampaignConfig: {} as ClientValuesCampaignConfig
}));
