import type { DescriptHttpBlock } from 'descript';
import type { Context } from '../descript/context';

import de from 'descript';
import nock from 'nock';

import { createContext } from '../descript/context';
import baseHttpBlockPublicApi from './baseHttpBlockPublicApi';

import createHttpReq from 'mocks/createHttpReq';
import createHttpRes from 'mocks/createHttpRes';

let context: Context;
let method: DescriptHttpBlock<Context, { id?: string}, unknown>;
let req;
let res;
beforeEach(() => {
    method = baseHttpBlockPublicApi({
        block: {
            pathname: '/testpath',
        },
    });

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен добавить заголовки пабликапи и не перетереть базовые', () => {
    const scope = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        reqheaders: {
            accept: 'application/json',
            'content-type': 'application/json',
            'x-application-id': 'autoru-app-ssr',
            'x-application-hostname': process.env._DEPLOY_HOSTNAME as string,
            'x-request-id': 'jest-request-id',
            'x-real-ip': '213.180.204.188',
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
