/*
Эти тесты выглядят странными и избыточными, но на то есть причины:
1) реклама очень важна и нам важно знать, что мы делаем правильные запросы и правильно их обрабатываем
2) мы проверяем корректность интеграции с @vertis/ads
 */

import de from 'descript';
import nock from 'nock';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import getWidgetSettings from 'auto-core/server/resources/direct/methods/widgetSettings';
import createContext from 'auto-core/server/descript/createContext';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import deLogger from 'auto-core/server/descript/loggerSingleton';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

const logFn = deLogger.log as jest.MockedFunction<typeof deLogger.log>;

let req: THttpRequest;
let res: THttpResponse;
let context: TDescriptContext;
let params: { 'imp-id': string };
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    params = {
        'imp-id': '123-123',
    };
});

it('должен сделать правильный запрос', () => {
    const scope = nock(`https://${ process.env.DIRECT_HOSTNAME }`)
        .get('/widget_settings?charset=utf-8&imp-id=123-123')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    return de.run(getWidgetSettings, { context, params })
        .then(() => {
            expect(scope.isDone()).toEqual(true);
        });
});

describe('обработка ответа', () => {
    it('должен считать 404 за ошибку', async() => {
        nock(`https://${ process.env.DIRECT_HOSTNAME }`)
            .get('/widget_settings?charset=utf-8&imp-id=123-123')
            .reply(404);

        await expect(
            de.run(getWidgetSettings, { context, params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_404', status_code: 404 },
        });
    });

    it('должен считать 500 за ошибку и делать ретрай', async() => {
        nock(`https://${ process.env.DIRECT_HOSTNAME }`)
            .get('/widget_settings?charset=utf-8&imp-id=123-123')
            .times(2)
            .reply(504);

        await expect(
            de.run(getWidgetSettings, { context, params }),
        ).rejects.toMatchObject({
            error: { id: 'HTTP_504', status_code: 504 },
        });
    });
});

describe('meta', () => {
    it('должен правильно передать мета-информацию в Logger', () => {
        nock(`https://${ process.env.DIRECT_HOSTNAME }`)
            .get('/widget_settings?charset=utf-8&imp-id=123-123')
            .reply(200, { status: 'SUCCESS' });

        return de.run(getWidgetSettings, { context, params })
            .then(() => {
                expect(deLogger.log).toHaveBeenCalledTimes(2);
                expect(logFn.mock.calls[0][0]).toMatchObject({
                    type: 'REQUEST_START',
                    request_options: {
                        extra: { name: 'direct:GET /widget_settings' },
                    },
                });
            });
    });
});
