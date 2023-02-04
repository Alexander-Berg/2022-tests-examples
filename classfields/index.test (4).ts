import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import middleware from './index';

let req: THttpRequest;
let res: THttpResponse;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен отдать страницу с правильными ссылками', async() => {
    req.headers['x-forwarded-host'] = 'm.auto.ru';
    req.url = '/gdpr/cars/audi/used/?foo=bar';

    middleware(req, res);
    await new Promise((resolve) => setTimeout(resolve, 500));

    expect(res.end).toHaveBeenCalledTimes(1);
    expect(res.end).toMatchSnapshot();
});
