import type { Request } from 'express';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import RedirectError from 'auto-core/lib/handledErrors/RedirectError';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import redirectCredit from './redirect-credit';

let req: Request & THttpRequest ;
let res: THttpResponse;

beforeEach(() => {
    req = createHttpReq();
    req.geoIds = [];
    res = createHttpRes();

    if (req.router) {
        req.router.params = {
            category: 'cars',
            section: 'all',
            on_credit: true,
        };

        (req.router.route.getData as jest.Mock).mockImplementation(jest.fn(() => ({
            controller: 'listing',
        })));
    }
});

it('делаем редирект, когда есть on_credit в параметрах', () => {
    req.url = '/cars/all/?on_credit=true';

    redirectCredit(req, res, (error) => {
        expect(error).toMatchObject({
            code: RedirectError.CODES.PAYMENT_TYPE,
            data: {
                location: '/cars/all/on-credit/',
                status: 301,
            },
        });
    });
});

it('делаем редирект, когда есть on-credit в чпу и super_gen в урле', () => {
    req.url = '/cars/bmw/3er-320/20548423/all/on-credit/';
    if (req.router) {
        req.router.params = {
            category: 'cars',
            section: 'all',
            on_credit: true,
            catalog_filter: [ { mark: 'BMW', model: '3ER', nameplate_name: '320', generation: '20548423' } ],
        };
    }

    redirectCredit(req, res, (error) => {
        expect(error).toMatchObject({
            code: RedirectError.CODES.PAYMENT_TYPE,
            data: {
                location: '/cars/bmw/3er-320/20548423/all/?on_credit=true',
                status: 301,
            },
        });
    });
});

it('не делаем редирект с правильным чпу на листинге', () => {
    req.url = '/cars/all/on-credit/';

    redirectCredit(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не делаем редирект не на листинге', () => {
    req.url = '/?on_credit=true';
    if (req.router) {
        (req.router.route.getData as jest.Mock).mockImplementation(jest.fn(() => ({
            controller: 'index',
        })));
    }

    redirectCredit(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});
