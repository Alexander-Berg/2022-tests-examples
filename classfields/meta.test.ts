/*
Эти тесты выглядят странными и избыточными, но на то есть причины:
1) реклама очень важна и нам важно знать, что мы делаем правильные запросы и правильно их обрабатываем
2) мы проверяем корректность интеграции с @vertis/ads
 */

import de from 'descript';
import nock from 'nock';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';
import deLogger from 'auto-core/server/descript/loggerSingleton';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import type { Params } from './meta';
import getMeta from './meta';

const logFn = deLogger.log as jest.MockedFunction<typeof deLogger.log>;

let req: THttpRequest;
let res: THttpResponse;
let context: TDescriptContext;
let params: Params;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.env.clientIp = 'clientIp';
    req.headers['accept-language'] = 'ru-RU';
    req.headers.cookie = 'cookie=value';
    req.headers['user-agent'] = 'ua jest';
    req.headers.referer = 'https://auto.ru/referer';
    req.headers['x-forwarded-for'] = 'x-forwarded-for';
    req.url = 'https://auto.ru/current_url';

    params = {
        awaps_section: '29337',
        'imp-id': '1',
        pageId: '148383',
        'partner-stat-id': '200',
    };
});

it('должен сделать правильный запрос и подставить заголовки', () => {
    const scope = nock(`https://${ process.env.DIRECT_HOSTNAME }`, {
        reqheaders: {
            accept: 'application/json',
            'accept-language': 'ru-RU',
            cookie: 'cookie=value',
            referer: 'https://auto.ru/current_url',
            'user-agent': 'ua jest',
            'x-forwarded-for': 'x-forwarded-for',
            'x-forwarded-proto': 'https',
            'x-real-ip': 'clientIp',
            'x-yabs-rereferer': 'https://auto.ru/referer',
        },
    })
        // eslint-disable-next-line max-len
        .get('/meta/148383?awaps_section=29337&charset=utf-8&callback=json&imp-id=1&page-ref=https%3A%2F%2Fauto.ru%2Freferer&partner-stat-id=200&redir-setuniq=1&server-side=1&target-ref=https%3A%2F%2Fauto.ru%2Fcurrent_url')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    return de.run(getMeta, { context, params })
        .then(() => {
            expect(scope.isDone()).toEqual(true);
        });
});

describe('обработка ответа', () => {
    it('должен считать 404 за ошибку', async() => {
        nock(`https://${ process.env.DIRECT_HOSTNAME }`)
            .get('/meta/148383')
            .query(true)
            .reply(404);

        await expect(
            de.run(getMeta, { context, params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_404', status_code: 404 },
        });
    });

    it('должен считать 500 за ошибку', async() => {
        nock(`https://${ process.env.DIRECT_HOSTNAME }`)
            .get('/meta/148383')
            .query(true)
            .reply(504);

        await expect(
            de.run(getMeta, { context, params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_504', status_code: 504 },
        });
    });
});

describe('meta', () => {
    it('должен правильно передать мета-информацию в Logger', () => {
        nock(`https://${ process.env.DIRECT_HOSTNAME }`)
            .get('/meta/148383')
            .query(true)
            .reply(200, { status: 'SUCCESS' });

        return de.run(getMeta, { context, params })
            .then(() => {
                expect(deLogger.log).toHaveBeenCalledTimes(2);
                expect(logFn.mock.calls[0][0]).toMatchObject({
                    type: 'REQUEST_START',
                    request_options: {
                        extra: { name: 'direct:GET /meta/{page-id}' },
                    },
                });
            });
    });
});
