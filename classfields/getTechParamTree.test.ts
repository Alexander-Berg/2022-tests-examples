import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import {
    configurationCoupeMock,
    configurationHatchbackMock,
} from 'auto-core/react/dataDomain/techSpecifications/mocks/configuration.mock';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';

import block from './getTechParamTree';

let context: TDescriptContext;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('вернул 2 кузова', () => {
    publicApi.get('/1.0/reference/catalog/cars/tech-param-tree?mark=FORD&model=MUSTANG')
        .reply(200, { status: 'SUCCESS', super_generations: [ configurationCoupeMock, configurationHatchbackMock ] });
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
        } })
        .then((result) => {
            expect(result.super_generations).toHaveLength(2);
        });
});

it('вернул 1 кузов', () => {
    publicApi.get('/1.0/reference/catalog/cars/tech-param-tree?mark=TOYOTA&model=CELICA')
        .reply(200, { status: 'SUCCESS', super_generations: [ configurationCoupeMock ] });
    return de.run(block, {
        context,
        params: {
            mark: 'TOYOTA',
            model: 'CELICA',
        } })
        .then((result) => {
            expect(result.super_generations).toHaveLength(1);
        });
});
