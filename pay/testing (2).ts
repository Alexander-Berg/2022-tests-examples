import { YaEnv } from '../typings/common';
import { paths, getEnv } from '../utils';

import { policies } from './csp/policies';
import * as csp from './csp/presets';

export default class TestingConfig {
    env: YaEnv = 'testing';

    version = getEnv('BUILD_VERSION', '0.0.0');

    systemTemplatesPath = paths.resolve('../templates');

    services = {
        addrs: { url: 'http://addrs-testing.search.yandex.net/search/stable/yandsearch' },
        'pay-webapi': { url: 'https://test.pay.yandex.ru' },
        'console-webapi': { url: 'https://test.console.pay.yandex.ru' },
        passport: { url: 'https://api.passport-test.yandex.ru' },
        avatars: { url: 'https://avatars.mdst.yandex.net' },
        blackbox: { host: 'blackbox-test.yandex.net' },
        'blackbox-api': { url: 'http://blackbox-test.yandex.net' },
        uaas: { url: 'http://uaas.search.yandex.net' },
        laas: { url: 'http://laas.yandex.ru' },
        bunker: {
            url: 'http://bunker-api-dot.yandex.net/v1',
            project: 'yandexpay-front/config',
            version: 'latest',
        },
    };

    avatarUrl = 'https://avatars.mdst.yandex.net';

    passportAuthUrl = 'https://passport-test.yandex.ru';
    passportAuthOrigin = 'pay';

    metrika = {
        host: 'https://mc.yandex.ru',
        counterId: '80508160',
        pageUrl: 'https://test.pay.yandex.ru',
        consoleCounterId: '86950181',
    };

    clientServices = {
        trust: 'https://trust-test.yandex.ru',
        console: 'https://test.console.pay.yandex.ru',
        // NB: Токен Маркета мы используем для тестирования 3DS
        trustServiceToken: 'blue_market_payments_5fac16d65c83b948a5b10577f373ea7c',
        geo: 'https://suggest-maps-test.n.yandex-team.ru',
        mapApiKey: 'afe8f728-7a72-4b37-b4f3-7da6d1e1e311',
    };

    uatraits = {
        browser: '/usr/share/uatraits/browser.xml',
        profiles: '/usr/share/uatraits/profiles.xml',
        extra: '/usr/share/uatraits/extra.xml',
    };

    abt = {
        service: 'trust',
        handler: 'TRUST',
        serviceId: 10002,
    };

    laas = {
        service: 'yandex-pay',
    };

    disabledMerchantId = ['1003b7ce-f225-4675-bf4e-e49c3ac5747c'];

    withoutPersonalizeMerchantId = [
        'a8f1c405-675c-48a5-a4a4-a20920ca28ad',
        '18b3b6ac-6d17-41ff-ad6a-cbed3775c3e3',
    ];

    shortCashbackViewMerchantId = [
        'a8f1c405-675c-48a5-a4a4-a20920ca28ad',
        '18b3b6ac-6d17-41ff-ad6a-cbed3775c3e3',
    ];

    split = {
        merchantId: [
            'a751322c-bd3b-4133-b823-ed460f6440fb', // Testing BrandShop
        ],
        amountRange: [1000, 100000],
    };

    monochromeLogoMerchantId: string[] = [
        // 'a751322c-bd3b-4133-b823-ed460f6440fb', // Testing BrandShop
    ];

    playground = {
        devUsername: '',
    };

    yaCSP = {
        presets: [
            csp.self,
            csp.trust,
            csp.trustTest,
            csp.bnpl.test,
            csp.auth3ds.test,
            csp.geo,
            csp.geoTest,
            csp.rum,
            csp.metrika,
            csp.yastatic,
            csp.passport,
            csp.cspUrl,
            csp.yamaps,
            csp.consoleApiTest,
        ],
        policies,
        reportUri: 'https://csp.yandex.net/csp',
        project: 'pay',
        serviceName: 'pay',
    };

    monochromeBackgroundMerchantIds = [
        '7bdd561e-f949-4d42-b2cd-72ab46687113', // testing-tinkoff
    ];
}
