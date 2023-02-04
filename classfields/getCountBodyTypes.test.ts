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

import block from './getCountBodyTypes';

let context: TDescriptContext;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    publicApi.get('/1.0/reference/catalog/cars/tech-param-tree?mark=FORD&model=MUSTANG')
        .reply(200, { status: 'SUCCESS', super_generations: [ configurationCoupeMock, configurationHatchbackMock ] });
});

it('вернул 0 кузовов', () => {
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
            body_type_group: [ 'SEDAN' ],
        } })
        .then((result) => {
            expect(result).toEqual({ countBodyTypes: 0, status: 'SUCCESS' });
        });
});

it('вернул 1 кузов', () => {
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
            body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
        } })
        .then((result) => {
            expect(result).toEqual({ countBodyTypes: 1, status: 'SUCCESS' });
        });
});

it('вернул 2 кузова', () => {
    return de.run(block, {
        context,
        params: {
            mark: 'FORD',
            model: 'MUSTANG',
        } })
        .then((result) => {
            expect(result).toEqual({ countBodyTypes: 2, status: 'SUCCESS' });
        });
});
