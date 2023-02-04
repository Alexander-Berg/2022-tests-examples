const { Request, Response, nextFunction } = require('./mocks');

const trafficDetector = require('../traffic-detector');
const router = require('realty-router').desktop;
const searchRoute = router.getRouteByName('search');

const defaultParams = {
    rgid: '587795',
    type: 'SELL',
    category: 'APARTMENT'
};

describe('trafficDetector', () => {
    const testCases = [
        {
            utm_source: 'yandex_direct',
            result: 'yandex',
            text: 'пришли с яндекс рекламы',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=yandex_direct',
            referer: 'https://yandex.ru/'
        },
        {
            utm_source: 'web_block',
            result: 'morda',
            text: 'пришли с морды десктоп',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=web_block',
            referer: 'https://yandex.ru/'
        },
        {
            utm_source: 'm_web_block',
            result: 'morda',
            text: 'пришли с морды тач',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=m_web_block',
            referer: 'https://yandex.ru/'
        },
        {
            utm_source: 'realty_web_block',
            result: 'morda',
            text: 'пришли с морды под экспом на десктопе',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=realty_web_block',
            referer: 'https://yandex.ru/'
        },
        {
            utm_source: 'realty_m_web_block',
            result: 'morda',
            text: 'пришли с морды под экспом на мобилке',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=realty_m_web_block',
            referer: 'https://yandex.ru/'
        },
        {
            result: 'search',
            text: 'пришли с серпа',
            resultUrl: '/moskva/kupit/kvartira/',
            referer: 'https://yandex.ru/'
        },
        {
            utm_source: 'test',
            result: 'search',
            text: 'пришли с невалидной для метрики utm меткой',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=test',
            referer: 'https://yandex.ru/'
        },
        {
            utm_source: 'test',
            result: 'search',
            text: 'пришли с невалидной для метрики utm меткой',
            resultUrl: '/moskva/kupit/kvartira/?utm_source=test',
            referer: 'https://yandex.ru/'
        },
        {
            result: 'google-search',
            text: 'пришли с google',
            resultUrl: '/moskva/kupit/kvartira/',
            referer: 'https://www.google.com/search'
        },
        {
            result: 'other',
            text: 'пришли с ноунейм источников',
            resultUrl: '/moskva/kupit/kvartira/',
            referer: 'https://yahoo.com/'
        },
        {
            result: 'direct',
            text: 'прямой переход',
            resultUrl: '/moskva/kupit/kvartira/',
            referer: ''
        },
        {
            result: 'yandex_wizard_maps',
            text: 'пришли с колдунщика яндекс карт',
            resultUrl: '/moskva/kupit/kvartira/?from=yandex_wizard_maps',
            referer: '',
            from: 'yandex_wizard_maps'
        },
        {
            result: 'google_adwords_display',
            text: 'пришли с google_adwords_display',
            resultUrl: '/moskva/kupit/kvartira/?from=google_adwords_display',
            referer: '',
            from: 'google_adwords_display'
        },
        {
            result: 'targetmailru',
            text: 'пришли с рекламы mail.ru',
            resultUrl: '/moskva/kupit/kvartira/?from=targetmailru',
            referer: '',
            from: 'targetmailru'
        },
        {
            result: 'google_adwords',
            text: 'пришли с google_adwords',
            resultUrl: '/moskva/kupit/kvartira/?from=google_adwords',
            referer: '',
            from: 'google_adwords'
        }
    ];

    // eslint-disable-next-line camelcase
    testCases.forEach(({ utm_source, result, text, resultUrl, referer, from }) => {
        it(text, async() => {
            const params = {
                ...defaultParams,
                utm_source,
                from
            };

            const currentUrl = searchRoute.build(params);

            expect(currentUrl).toBe(resultUrl);

            const req = new Request(currentUrl);

            req.headers = {
                referer
            };

            const res = new Response();

            await trafficDetector()(req, res, nextFunction);

            expect(req.trafficFrom).toBe(result);
        });
    });
});
