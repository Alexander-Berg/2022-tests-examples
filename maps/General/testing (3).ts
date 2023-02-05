import {AppConfig} from './interface';

import {production} from './production';
import {TvmDestination} from '../types/tvm';

export const testing: AppConfig = {
    ...production,
    general: {
        ...production.general,
        network: '_MAPSTESTQNETS_'
    },
    database: {
        ...production.database,
        hosts: ['sas-qyd3s3tud2jw424c.db.yandex.net', 'vla-v370li0f2hlsloj3.db.yandex.net'],
        cluster: 'mdbcgvtpagk2bkonkbj6'
    },
    tvm: {
        enabled: true,
        enabledCheck: true,
        clientProjectsMap: new Map([
            [2001692, 'maps-front-maps'],
            [2009603, 'maps-front-maps-development'],
            [2013874, 'maps-front-mobmaps-proxy-api'],
            [2001636, 'clean-web-sender']
        ]),
        destinations: {
            [TvmDestination.BVM_INT]: 2020032,
            [TvmDestination.CLEAN_WEB]: 2016563,
            [TvmDestination.AVATARS]: 2002148
        },
        environment: 'Test',
        source: 2024327
    },
    secret: {
        id: 'sec-01en60r9f6emaj14d1ywsdbbd4',
        version: 'ver-01g3ty17q1w5m9w3ydfrmysxyw'
    },
    upstreams: {
        bvmInt: 'https://bvm-int.tst.geosmb.maps.yandex.net',
        storiesInt: 'https://stories-int.tst.c.maps.yandex.net',
        geosearchSnippets: 'http://addrs-testing.search.yandex.net/fast-snippets-test',
        cleanWeb: 'https://cw-router-dev.common.yandex.net',
        avatarsInt: 'http://avatars-int.mdst.yandex.net:13000',
        avatars: 'https://avatars.mdst.yandex.net'
    },
    rejectUnauthorizedCert: false
};
