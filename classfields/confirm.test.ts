import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import middleware from './confirm';

let req: THttpRequest;
let res: THttpResponse;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен выставить куку и отправить по retpath, если урл валиден', () => {
    req.query.retpath = 'https://frontend.aandrosov.dev.avto.ru/?foo=bar';

    middleware(req, res);

    expect(res.cookie).toHaveBeenCalledTimes(1);
    expect(res.cookie).toHaveBeenCalledWith('autoru_gdpr', '1', { httpOnly: false, maxAge: 94608000000, sameSite: 'none' });

    expect(res.redirect).toHaveBeenCalledTimes(1);
    expect(res.redirect).toHaveBeenCalledWith(302, 'https://frontend.aandrosov.dev.avto.ru/?foo=bar');

    expect(res.end).toHaveBeenCalledTimes(1);
});

it('должен выставить куку и отправить в https://auto.ru, если урл невалиден', () => {
    req.query.retpath = 'https://evil.com/?foo=bar';

    middleware(req, res);

    expect(res.cookie).toHaveBeenCalledTimes(1);
    expect(res.cookie).toHaveBeenCalledWith('autoru_gdpr', '1', { httpOnly: false, maxAge: 94608000000, sameSite: 'none' });

    expect(res.redirect).toHaveBeenCalledTimes(1);
    expect(res.redirect).toHaveBeenCalledWith(302, 'https://auto.ru/');

    expect(res.end).toHaveBeenCalledTimes(1);
});
