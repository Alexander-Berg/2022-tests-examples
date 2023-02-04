import type { NextFunction, Request } from 'express';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import { middleware } from './redirect-remove-domain-m.auto.ru';

let req: Request &THttpRequest ;
let res: THttpResponse;
let next: NextFunction;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    req.url = '/foo/?foo=bar';
    next = jest.fn();
});

it('должен средиректить из m.auto.ru в auto.ru', () => {
    req.headers['x-forwarded-host'] = 'm.autoru_frontend.base_domain';

    middleware(req, res, next);
    expect(res.redirect).toHaveBeenCalledTimes(1);
    expect(res.redirect).toHaveBeenCalledWith(301, 'https://autoru_frontend.base_domain/foo/?foo=bar');
    expect(res.end).toHaveBeenCalledTimes(1);
    expect(next).not.toHaveBeenCalled();
});

it('не должен средиректить домен отличный от m.auto.ru', () => {
    req.headers['x-forwarded-host'] = 'autoru_frontend.base_domain';

    middleware(req, res, next);
    expect(res.redirect).not.toHaveBeenCalled();
    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
});
