import type { DescriptHttpBlock } from 'descript';
import type { Context } from '../descript/context';

import de from 'descript';
import nock from 'nock';

import { createContext } from '../descript/context';
import baseHttpBlock from './baseHttpBlock';

import createHttpReq from 'mocks/createHttpReq';
import createHttpRes from 'mocks/createHttpRes';

let context: Context;
let method: DescriptHttpBlock<Context, { id?: string}, unknown>;
let req;
let res;
beforeEach(() => {
    method = baseHttpBlock({
        block: {
            hostname: 'auto.ru',
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен добавить базовые заголовки', () => {
    const scope = nock('http://auto.ru', {
        reqheaders: {
            'x-application-id': 'autoru-app-ssr',
            'x-application-hostname': process.env._DEPLOY_HOSTNAME as string,
            'x-request-id': 'jest-request-id',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    return de.run(method, { context, params: {} })
        .then(() => {
            expect(scope.isDone()).toEqual(true);
        });
});

it('должен добавить заголовок с таймаутом, если он есть в декларации блока', () => {
    const scope = nock('http://auto.ru', {
        reqheaders: {
            'x-client-timeout-ms': '1000',
        },
    })
        .get('/testpath')
        .reply(200, { status: 'OK' }, {
            'content-type': 'application/json',
        });

    const blockWithTimeout = method({
        block: {
            timeout: 1000,
        },
    });

    return de.run(blockWithTimeout, { context }).then(() => {
        expect(scope.isDone()).toEqual(true);
    });
});
