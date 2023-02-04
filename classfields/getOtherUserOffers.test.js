const de = require('descript');
const mockdate = require('mockdate');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getOtherUserOffers = require('./getOtherUserOffers');

let context;
let req;
let res;
beforeEach(() => {
    mockdate.set('2020-06-18');
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

afterEach(() => {
    mockdate.reset();
});

describe('search_parameters', () => {
    describe('sorting', () => {
        it('не должен вычислять sort, если не пришел sorting', () => {
            publicApi
                .get(`/1.0/user/123_321/offers/all`)
                .query(true)
                .reply(200, {
                    offers: [],
                    filters: { status: [ 'ACTIVE' ] },
                    status: 'SUCCESS',
                });

            const params = {
                category: 'all',
                encrypted_user_id: '123_321',
                creationDate: String(Date.now()),
            };

            return de.run(getOtherUserOffers, {
                context,
                params,
            }).then((result) => expect(result.search_parameters.sort).toBeUndefined());
        });

        it('должен вычислить sort, если пришел sorting', () => {
            publicApi
                .get(`/1.0/user/123_321/offers/all`)
                .query(true)
                .reply(200, {
                    offers: [],
                    filters: { status: [ 'ACTIVE' ] },
                    sorting: { name: 'sort_name' },
                    status: 'SUCCESS',
                });

            const params = {
                category: 'all',
                encrypted_user_id: '123_321',
                creationDate: String(Date.now()),
            };

            return de.run(getOtherUserOffers, {
                context,
                params,
            }).then((result) => expect(result.search_parameters.sort).toEqual('sort_name-asc'));
        });

        it('должен вычислить sort, если пришел sorting с desc', () => {
            publicApi
                .get(`/1.0/user/123_321/offers/all`)
                .query(true)
                .reply(200, {
                    offers: [],
                    filters: { status: [ 'ACTIVE' ] },
                    sorting: { name: 'sort_name', desc: true },
                    status: 'SUCCESS',
                });

            const params = {
                category: 'all',
                encrypted_user_id: '123_321',
                creationDate: String(Date.now()),
            };

            return de.run(getOtherUserOffers, {
                context,
                params,
            }).then((result) => expect(result.search_parameters.sort).toEqual('sort_name-desc'));
        });

        it('должен заменить create_date на cr_date', () => {
            publicApi
                .get(`/1.0/user/123_321/offers/all`)
                .query(true)
                .reply(200, {
                    offers: [],
                    filters: { status: [ 'ACTIVE' ] },
                    sorting: { name: 'create_date' },
                    status: 'SUCCESS',
                });

            const params = {
                category: 'all',
                encrypted_user_id: '123_321',
                creationDate: String(Date.now()),
            };

            return de.run(getOtherUserOffers, {
                context,
                params,
            }).then((result) => expect(result.search_parameters.sort).toEqual('cr_date-asc'));
        });
    });

    it('добавит encrypted_user_id и category из params', () => {
        publicApi
            .get(`/1.0/user/123_321/offers/all`)
            .query(true)
            .reply(200, {
                offers: [],
                filters: { status: [ 'ACTIVE' ] },
                sorting: { name: 'sort_name', desc: true },
                status: 'SUCCESS',
            });

        const params = {
            category: 'all',
            encrypted_user_id: '123_321',
            creationDate: String(Date.now()),
        };

        return de.run(getOtherUserOffers, {
            context,
            params,
        }).then((result) => {
            expect(result.search_parameters.encrypted_user_id).toEqual('123_321');
            expect(result.search_parameters.category).toEqual('all');
        });
    });
});
