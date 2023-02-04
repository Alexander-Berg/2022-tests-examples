const mockGetBunkerDict = jest.fn();

jest.mock('auto-core/lib/util/getBunkerDict', () => mockGetBunkerDict);

mockGetBunkerDict.mockImplementation(() => ([ '213' ]));

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const MockDate = require('mockdate');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const searcher = require('auto-core/server/resources/yaAuto/getResource.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const offerMock = require('autoru-frontend/mockData/responses/offer.mock').offer;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const baseOffer = cloneOfferWithHelpers(offerMock).withMatchApplicationContexts([ 'mixed-listing' ]).value();

const searchListing = require('./searchListing');

let context;
let req;
let res;
beforeEach(() => {
    publicApi
        .get('/1.0/search/cars')
        .query((query) => query.dealer_id)
        .reply(200, {
            count: 487407,
            pagination: {
                page: 1,
                page_size: 20,
                total_offers_count: 487407,
                total_page_count: 24371,
            },
            sorting: {
                name: 'fresh_relevance_1',
                desc: true,
            },
            search_parameters: {
                currency: 'RUR',
                has_image: true,
                offer_grouping: false,
                state_group: 'ALL',
                dealer_id: '12345',
            },
            offers: [
                { ...baseOffer,
                    car_info: {
                        configuration: {
                            id: '6501722',
                        },
                        complectation: {
                            id: '0',
                        },
                        equipment: {
                            usb: true,
                        },
                        mark_info: {
                            code: 'VOLKSWAGEN',
                            name: 'Volkswagen',
                        },
                        model_info: {
                            code: 'AMAROK',
                            name: 'Amarok',
                        },
                        super_gen: {
                            id: '6500211',
                            name: 'I',
                        },
                    } } ],
            status: 'SUCCESS',
        });

    // soldOffers
    publicApi
        .get('/1.0/search/cars')
        .query((query) => query.page_size === 1 && query.with_revoked === 'ONLY')
        .reply(200, {});

    // без ППН
    publicApi
        .get(/\/1.0\/search\/(cars|trucks|moto)/)
        // проверяем на наличие with_autoru_expert в запросе, чтобы сюда не попал запрос счетчика объявлений "для профессионалов"
        .query((query) => query.with_revoked !== 'ONLY' && query.with_autoru_expert !== 'ONLY')
        .times(2)
        .reply(200, {
            count: 487407,
            pagination: {
                page: 1,
                page_size: 20,
                total_offers_count: 487407,
                total_page_count: 24371,
            },
            sorting: {
                name: 'fresh_relevance_1',
                desc: true,
            },
            search_parameters: {
                currency: 'RUR',
                has_image: true,
                offer_grouping: false,
                state_group: 'ALL',
            },
            offers: [ baseOffer ],
            status: 'SUCCESS',
        });

    searcher
        .get('/dealers?page_size=4')
        .reply(200, { data: [ { dealers: [] } ] });

    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(200, { entities: [] });

    publicApi
        .get('/1.0/search/cars')
        .query((query) => {
            return (
                query.catalog_filter === 'mark=VOLKSWAGEN,model=AMAROK,generation=6500211,configuration=6501722' &&
                query.category === 'cars' &&
                query.context === 'group_card' &&
                query.dealer_id === '12345' &&
                query.offer_grouping === 'false' &&
                query.only_official === 'true' &&
                query.page_size === '7' &&
                query.page === '1' &&
                query.sort === 'fresh_relevance_1-desc' &&
                query.state_group === 'NEW' &&
                query.with_discount === 'true'
            );
        })
        .reply(200, {
            count: 487407,
            pagination: {
                page: 1,
                page_size: 20,
                total_offers_count: 487407,
                total_page_count: 24371,
            },
            sorting: {
                name: 'fresh_relevance_1',
                desc: true,
            },
            search_parameters: {
                currency: 'RUR',
                has_image: true,
                offer_grouping: false,
                state_group: 'ALL',
                dealer_id: '12345',
            },
            offers: [
                { ...baseOffer,
                    car_info: {
                        configuration: {
                            id: '6501722',
                        },
                        complectation: {
                            id: '0',
                        },
                        equipment: {
                            usb: true,
                        },
                        mark_info: {
                            code: 'VOLKSWAGEN',
                            name: 'Volkswagen',
                        },
                        model_info: {
                            code: 'AMAROK',
                            name: 'Amarok',
                        },
                        super_gen: {
                            id: '6500211',
                            name: 'I',
                        },
                    } } ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/count')
        .query({
            category: 'cars',
            context: 'listing',
        })
        .reply(200, {
            count: 100,
            pagination: {
                page: 1,
                page_size: 20,
                total_offers_count: 100,
                total_page_count: 10,
            },
            search_parameters: {},
        });

    MockDate.set('2019-07-29');

    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

afterEach(() => {
    // jest.resetModules();
    MockDate.reset();
});

describe('робот', () => {
    beforeEach(() => {
        req.isRobot = true;
        req.cookies.autoru_sid = 'moderator_autoru_auth';
    });

    it('должен ответить данными без ППН, если пришел ответ от серчера', () => {
        return de.run(searchListing, {
            context,
            params: { category: 'cars' },
        }).then(result => {
            expect(result).toMatchSnapshot();
        });
    });
});

describe('не робот', () => {
    beforeEach(() => {
        req.isRobot = false;
        req.cookies.autoruuid = 'TEST_DEVICE_ID';
    });

    describe('пользователь без ППН', () => {
        beforeEach(() => {
            publicApi.get('/1.0/session/').reply(200, sessionFixtures.user_auth());

            req.cookies.autoru_sid = 'user_auth';
        });

        it('должен ответить данными без права первой ночи, если пришел ответ от серчера', () => {
            return de.run(searchListing, {
                context,
                params: { category: 'cars' },
            }).then(result => {
                expect(result).toMatchSnapshot();
            });
        });
    });
});

it('должен добавить dealer_code в search_parameters, если это запро для дилера', () => {
    return de.run(searchListing, {
        context,
        params: { category: 'cars', dealer_id: '12345', dealer_code: 'diler_super_puper' },
    }).then(result => {
        expect(result.search_parameters).toEqual({
            category: 'cars',
            dealer_code: 'diler_super_puper',
            dealer_id: '12345',
            section: 'all',
        });
    });
});

it('должен отдать shouldShowListingBestPriceBlock true, если все условия соблюдены', () => {
    context = createContext({
        req: {
            ...req,
            geoIdsParents: {
                '213': [ '213' ],
            },
        },
        res: res,
        config: {},
    });

    return de.run(searchListing, {
        context,
        params: { category: 'cars', dealer_id: '12345', dealer_code: 'diler_super_puper' },
    }).then(result => {
        expect(result.search_parameters).toEqual({
            category: 'cars',
            dealer_code: 'diler_super_puper',
            dealer_id: '12345',
            section: 'all',
        });
    });
});

it('должен отдать equipmentDictionary и listingOneConfigurationGroup, если все условия соблюдены', () => {
    context = createContext({
        req: {
            ...req,
            geoIdsParents: {
                '213': [ '213' ],
            },
            experimentsData: {
                has: (expFlag) => expFlag !== 'VTF-1625_yes302_redirect',
            },
        },
        res: res,
        config: {},
    });

    return de.run(searchListing, {
        context,
        params: { category: 'cars', section: 'new', dealer_id: '12345', dealer_code: 'diler_super_puper' },
    }).then(result => {
        expect(result.listingOneConfigurationGroup).toBeDefined();
        expect(result.equipmentDictionary).toBeDefined();
    });
});

it('не должен отдать equipmentDictionary и listingOneConfigurationGroup, если section used', () => {
    context = createContext({
        req: {
            ...req,
            geoIdsParents: {
                '213': [ '213' ],
            },
            experimentsData: {
                has: (expFlag) => expFlag !== 'VTF-1625_yes302_redirect',
            },
        },
        res: res,
        config: {},
    });

    return de.run(searchListing, {
        context,
        params: { category: 'cars', section: 'used', dealer_id: '12345', dealer_code: 'diler_super_puper' },
    }).then(result => {
        expect(result.listingOneConfigurationGroup).toBeUndefined();
        expect(result.equipmentDictionary).toBeUndefined();
    });
});
