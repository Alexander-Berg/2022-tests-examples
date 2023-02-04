const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./convert-listing-url-to-publicapi-params');

function successResponseForCardGroupComplectations() {
    publicApi
        .get('/1.0/salon/inkom_avto_moskva')
        .query({})
        .reply(200, {
            salon: {
                dealer_id: 777,
            },
        });
}

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    successResponseForCardGroupComplectations();
});

it('должен вернуть 404 для пустого урла', () => {
    return de.run(block).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

it('должен вернуть 404 для неизвестный урлов', () => {
    return de.run(block, {
        context,
        params: { url: '/this-is-404-route/' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

describe('listing', () => {
    it('должен вернуть параметры поиска для листинга', () => {
        return de.run(block, {
            context,
            // eslint-disable-next-line max-len
            params: { url: '/cars/bmw/x6/new/?in_stock=IN_STOCK&catalog_equipment=automatic-lighting-control&catalog_equipment=high-beam-assist&from=marketing-campaign' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        catalog_equipment: [ 'automatic-lighting-control', 'high-beam-assist' ],
                        catalog_filter: [ 'mark=BMW,model=X6' ],
                        currency: undefined,
                        in_stock: 'IN_STOCK',
                        only_official: true,
                        rid: [],
                        sort: undefined,
                        state_group: 'NEW',
                        top_days: undefined,
                        with_discount: 'true',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга с mark_model_nameplate', () => {
        return de.run(block, {
            context,
            // eslint-disable-next-line max-len
            params: { url: '/cars/new/?mark_model_nameplate=BMW#X5&mark_model_nameplate=BMW#X6&in_stock=IN_STOCK&catalog_equipment=automatic-lighting-control&catalog_equipment=high-beam-assist&from=marketing-campaign' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        catalog_equipment: [ 'automatic-lighting-control', 'high-beam-assist' ],
                        catalog_filter: [ 'mark=BMW,model=X5', 'mark=BMW,model=X6' ],
                        currency: undefined,
                        in_stock: 'IN_STOCK',
                        only_official: true,
                        rid: [],
                        sort: undefined,
                        state_group: 'NEW',
                        top_days: undefined,
                        with_discount: 'true',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга с configuration_id', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=MAZDA%23CX_5%2320939955%2320940096')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: '20940096', configuration: { body_type_group: 'ALLROAD_5_DOORS' } },
                        ],
                        meta_level: 'CONFIGURATION_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        return de.run(block, {
            context,
            params: { url: '/moskva/cars/mazda/cx_5/20939955/20940096/used/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        body_type_group: [ 'ALLROAD_5_DOORS' ],
                        catalog_filter: [ 'mark=MAZDA,model=CX_5,generation=20939955,configuration=20940096' ],
                        offer_grouping: 'false',
                        rid: [ 213 ],
                        state_group: 'USED',
                        with_delivery: 'BOTH',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга с configuration_id и tech_param_id', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=MAZDA%236%232307584%232307585')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: '2307585', configuration: { body_type_group: 'SEDAN' } },
                        ],
                        meta_level: 'CONFIGURATION_LEVEL',
                    },
                    {
                        entities: [
                            {
                                id: '2414702',
                                tech_params: {
                                    engine_type: 'GASOLINE',
                                    displacement: 1999,
                                    gear_type: 'FORWARD_CONTROL',
                                    transmission: 'AUTOMATIC',
                                    power: 147,
                                    power_kvt: '108.0',
                                    year_start: 2007,
                                    year_stop: 2009,
                                    human_name: '2.0 AT (147 л.с.)',
                                },
                            },
                        ],
                        meta_level: 'TECH_PARAM_LEVEL',
                    },
                ],
                status: 'SUCCESS',
            });

        return de.run(block, {
            context,
            params: { url: '/moskva/cars/mazda/6/2307584/2307585/2414702/used/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        body_type_group: [ 'SEDAN' ],
                        catalog_filter: [ 'mark=MAZDA,model=6,generation=2307584,configuration=2307585,tech_param=2414702' ],
                        displacement_from: 2000,
                        displacement_to: 2000,
                        engine_group: [ 'GASOLINE' ],
                        gear_type: [ 'FORWARD_CONTROL' ],
                        offer_grouping: 'false',
                        power_from: 147,
                        power_to: 147,
                        rid: [ 213 ],
                        state_group: 'USED',
                        transmission: [ 'AUTOMATIC' ],
                        with_delivery: 'BOTH',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга с configuration_id, если крошки не отвечают', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=MAZDA%23CX_5%2320939955%2320940096')
            .times(2)
            .reply(500, {});

        return de.run(block, {
            context,
            params: { url: '/moskva/cars/mazda/cx_5/20939955/20940096/used/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        catalog_filter: [ 'mark=MAZDA,model=CX_5,generation=20939955,configuration=20940096' ],
                        offer_grouping: 'false',
                        rid: [ 213 ],
                        state_group: 'USED',
                        with_delivery: 'BOTH',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга мото /atv/yamaha/all/', () => {
        return de.run(block, {
            context,
            params: { url: '/atv/yamaha/all/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'moto',
                    query: {
                        catalog_filter: [ 'mark=YAMAHA' ],
                        moto_category: 'ATV',
                        state_group: 'ALL',

                        // defaults
                        rid: [],
                        sort: undefined,
                        top_days: undefined,
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга мото /moskva/motorcycle/honda/shadow/new/', () => {
        return de.run(block, {
            context,
            params: { url: '/moskva/motorcycle/honda/shadow/new/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'moto',
                    query: {
                        catalog_filter: [ 'mark=HONDA,model=SHADOW' ],
                        moto_category: 'MOTORCYCLE',
                        state_group: 'NEW',

                        // defaults
                        rid: [ 213 ],
                        sort: undefined,
                        top_days: undefined,
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга trucks /moskva/truck/man/all/', () => {
        return de.run(block, {
            context,
            params: { url: '/moskva/truck/man/all/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'trucks',
                    query: {
                        catalog_filter: [ 'mark=MAN' ],
                        trucks_category: 'TRUCK',
                        state_group: 'ALL',

                        // defaults
                        rid: [ 213 ],
                        sort: undefined,
                        top_days: undefined,
                        with_delivery: 'BOTH',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга trucks /moskva/lcv/citroen/jumper/new/?gear_type=FULL_PLUG', () => {
        return de.run(block, {
            context,
            params: { url: '/moskva/lcv/citroen/jumper/new/?gear_type=FULL_PLUG' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'trucks',
                    query: {
                        catalog_filter: [ 'mark=CITROEN,model=JUMPER' ],
                        trucks_category: 'LCV',
                        state_group: 'NEW',
                        gear_type: 'FULL_PLUG',

                        // defaults
                        rid: [ 213 ],
                        sort: undefined,
                        top_days: undefined,
                        with_delivery: 'BOTH',
                    },
                });
            });
    });

    it('должен вернуть параметры поиска для листинга trucks со старой категорией /light_trucks/all/', () => {
        return de.run(block, {
            context,
            params: { url: '/light_trucks/all/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'trucks',
                    query: {
                        trucks_category: 'LCV',
                        state_group: 'ALL',

                        // defaults
                        rid: [],
                        sort: undefined,
                        top_days: undefined,
                        with_delivery: 'BOTH',
                    },
                });
            });
    });

    it('должен поддерживаеть recommended=true', () => {
        return de.run(block, {
            context,
            params: { url: '/moskva/cars/all/?recommended=true' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        recommended: 'true',
                        state_group: 'ALL',

                        // defaults
                        currency: undefined,
                        rid: [ 213 ],
                        sort: undefined,
                        top_days: undefined,
                        with_delivery: 'BOTH',
                        with_discount: 'true',
                    },
                });
            });
    });
});

describe('card-group-old', () => {
    it('должен вернуть параметры для групповой карточки', () => {
        return de.run(block, {
            context,
            params: { url: '/cars/new/group/bmw/x6/20158772/21075602/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        context: 'group_card',
                        currency: undefined,
                        grouping_id: `tech_param_id=20158772,complectation_id=21075602`,
                        offer_grouping: false,
                        only_official: true,
                        rid: [],
                        sort: undefined,
                        state_group: 'NEW',
                        top_days: undefined,
                        with_discount: true,
                    },
                });
            });
    });

    it('должен вернуть параметры для групповой карточки с поисковым запросом', () => {
        return de.run(block, {
            context,
            // eslint-disable-next-line max-len
            params: { url: '/cars/new/group/bmw/x6/20158772/21075602/?in_stock=IN_STOCK&catalog_equipment=automatic-lighting-control&catalog_equipment=high-beam-assist' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        catalog_equipment: [ 'automatic-lighting-control', 'high-beam-assist' ],
                        context: 'group_card',
                        currency: undefined,
                        grouping_id: `tech_param_id=20158772,complectation_id=21075602`,
                        in_stock: 'IN_STOCK',
                        offer_grouping: false,
                        only_official: true,
                        rid: [],
                        sort: undefined,
                        state_group: 'NEW',
                        top_days: undefined,
                        with_discount: true,
                    },
                });
            });
    });
});

describe('card-group', () => {
    it('должен вернуть параметры для групповой карточки', () => {
        return de.run(block, {
            context,
            params: { url: '/cars/new/group/bmw/x6/21610786-21610913/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        catalog_filter: [
                            'mark=BMW,model=X6,generation=21610786,configuration=21610913',
                        ],
                        context: 'group_card',
                        currency: undefined,
                        offer_grouping: false,
                        only_official: true,
                        rid: [],
                        state_group: 'NEW',
                        with_discount: true,
                    },
                });
            });
    });

    it('должен вернуть параметры для групповой карточки с поисковым запросом', () => {
        return de.run(block, {
            context,
            // eslint-disable-next-line max-len
            params: { url: '/cars/new/group/bmw/x6/21610786-21610913/?in_stock=IN_STOCK&catalog_equipment=automatic-lighting-control&catalog_equipment=high-beam-assist&page_from=page_listing%2Cblock_listing%2Ctype_group' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        catalog_equipment: [ 'automatic-lighting-control', 'high-beam-assist' ],
                        catalog_filter: [
                            'mark=BMW,model=X6,generation=21610786,configuration=21610913',
                        ],
                        context: 'group_card',
                        currency: undefined,
                        in_stock: 'IN_STOCK',
                        offer_grouping: false,
                        only_official: true,
                        rid: [],
                        sort: undefined,
                        state_group: 'NEW',
                        top_days: undefined,
                        with_discount: true,
                    },
                });
            });
    });
});

describe('страницы дилеров', () => {
    it('должен вернуть параметры листинга неофициального дилера вместе с id дилера', () => {
        return de.run(block, {
            context,
            params: { url: '/diler/cars/used/inkom_avto_moskva/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        currency: undefined,
                        dealer_id: 777,
                        offer_grouping: 'false',
                        rid: [],
                        sort: undefined,
                        state_group: 'USED',
                        top_days: undefined,
                        with_delivery: 'BOTH',
                        is_from_qr: false,
                    },
                });
            });
    });

    it('должен вернуть параметры листинга официального дилера вместе с id дилера', () => {
        return de.run(block, {
            context,
            params: { url: '/diler-oficialniy/cars/used/inkom_avto_moskva/' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        currency: undefined,
                        dealer_id: 777,
                        offer_grouping: 'false',
                        rid: [],
                        sort: undefined,
                        state_group: 'USED',
                        top_days: undefined,
                        with_delivery: 'BOTH',
                        is_from_qr: false,
                    },
                });
            });
    });

    it('должен вернуть is_from_qr true, если в ссылке есть параметр qr_code=1', () => {
        return de.run(block, {
            context,
            params: { url: '/diler-oficialniy/cars/used/inkom_avto_moskva/?qr_code=1' },
        })
            .then(() => {
                expect(res.send).toHaveBeenCalledWith({
                    category: 'cars',
                    query: {
                        currency: undefined,
                        dealer_id: 777,
                        offer_grouping: 'false',
                        rid: [],
                        sort: undefined,
                        state_group: 'USED',
                        top_days: undefined,
                        with_delivery: 'BOTH',
                        is_from_qr: true,
                    },
                });
            });
    });
});
