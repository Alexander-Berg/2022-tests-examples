const { readFileSync } = require('fs');
const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const getBreadcrumbs = require('./getBreadcrumbs');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

import { loadAllMessages } from 'auto-core/proto/schema-registry';

let context;
let req;
let res;
beforeEach(async() => {
    await loadAllMessages();
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('cache', () => {
    it('должен закешировать запрос с правильным ключом', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query({
                bc_lookup: 'AUDI',
            })
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        const cache = new de.Cache();

        return de.run(getBreadcrumbs({
            options: { cache },
        }), {
            context,
            params: { bc_lookup: 'AUDI', foo: 'bar' },
        })
            .then(() => {
                expect(
                    cache.get({ key: 'descript3-publicApiCard://1.0/search/{category}/breadcrumbs?bc_lookup=AUDI&category=cars' }),
                ).toBeDefined();
            });
    });
});

describe('params', () => {
    it('должен оставить bc_lookup, если он есть', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query({
                bc_lookup: 'AUDI',
            })
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        return de.run(getBreadcrumbs, {
            context,
            params: { bc_lookup: 'AUDI' },
        })
            .then(() => {
                expect(nock.isDone()).toEqual(true);
            });
    });

    it('должен преобразовать mark_model_nameplate в bc_lookup', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query({
                bc_lookup: 'AUDI',
            })
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        return de.run(getBreadcrumbs, {
            context,
            params: { mark_model_nameplate: [ 'AUDI' ] },
        })
            .then(() => {
                expect(nock.isDone()).toEqual(true);
            });
    });

    it('должен преобразовать catalog_filter в bc_lookup', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query({
                bc_lookup: 'AUDI',
            })
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        return de.run(getBreadcrumbs, {
            context,
            params: { catalog_filter: [ { mark: 'AUDI' } ] },
        })
            .then(() => {
                expect(nock.isDone()).toEqual(true);
            });
    });

    it('должен удалить ненужные параметры', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query({
                bc_lookup: 'AUDI',
            })
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        return de.run(getBreadcrumbs, {
            context,
            params: { bc_lookup: 'AUDI', foo: 'bar' },
        })
            .then(() => {
                expect(nock.isDone()).toEqual(true);
            });
    });
});

describe('preparer', () => {
    // eslint-disable-next-line jest/no-disabled-tests
    it.skip('должен обработать protobuf и прогнать данные через preparer', async() => {
        const proto = readFileSync(require.resolve('auto-core/proto/__messages__/auto.api.BreadcrumbsResponse.bin'));
        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=HONDA%23CIVIC%234569475%236470343')
            .reply(200, proto, {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.BreadcrumbsResponse',
            });

        await expect(
            de.run(getBreadcrumbs, {
                context,
                params: { bc_lookup: 'HONDA#CIVIC#4569475#6470343' },
            }),
        ).resolves.toMatchSnapshot();
    });
});
