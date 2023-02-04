const prepareRequestParams = require('./prepareRequestParams');

const contextMock = {
    req: {
        cookies: {},
        regionByIp: {},
        experimentsData: { has: () => false },
    },
};

describe('section -> state', () => {
    const TESTS = [
        {
            params: { section: 'all' },
            result: { state_group: 'ALL' },
        },
        {
            params: { section: 'new' },
            result: { state_group: 'NEW' },
        },
        {
            params: { section: 'used' },
            result: { state_group: 'USED' },
        },
    ];

    TESTS.forEach(testCase => {
        it(`should convert ${ JSON.stringify(testCase.params) } to ${ JSON.stringify(testCase.result) }`, () => {
            const requestParams = prepareRequestParams({
                params: testCase.params,
                context: contextMock,
            }, false);
            expect(requestParams).toEqual(testCase.result);
        });
    });
});

describe('body_type_group', () => {
    const TESTS = [
        {
            params: { body_type_group: [ 'HATCHBACK' ] },
            result: { body_type_group: [] },
        },
        {
            params: { section: 'new' },
            result: { state_group: 'NEW' },
        },
        {
            params: { section: 'used' },
            result: { state_group: 'USED' },
        },
    ];

    TESTS.forEach(testCase => {
        it(`should convert ${ JSON.stringify(testCase.params) } to ${ JSON.stringify(testCase.result) }`, () => {
            const requestParams = prepareRequestParams({
                params: testCase.params,
                context: contextMock,
            }, false);
            expect(requestParams).toEqual(testCase.result);
        });
    });
});

describe('color', () => {
    const TESTS = [
        {
            params: { body_type_group: [ 'SEDAN' ] },
            result: { body_type_group: [ 'SEDAN' ] },
        },
        {
            params: { color: 'EE1D19' },
            result: { color: [ 'EE1D19' ] },
        },
        {
            params: { color: [ 'EE1D19' ] },
            result: { color: [ 'EE1D19' ] },
        },
    ];

    TESTS.forEach(testCase => {
        it(`should convert ${ JSON.stringify(testCase.params) } to ${ JSON.stringify(testCase.result) }`, () => {
            const requestParams = prepareRequestParams({
                params: testCase.params,
                context: contextMock,
            }, false);
            expect(requestParams).toEqual(testCase.result);
        });
    });
});

describe('has_video', () => {
    it('должен преобразовать has_video=true в search_tag["video"]', () => {
        const params = { has_video: true };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'video' ] });
    });

    it('должен преобразовать has_video="true" в search_tag["video"]', () => {
        const params = { has_video: 'true' };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'video' ] });
    });

    it('должен преобразовать {has_video: "true", search_tag: ["certificate_manufacturer"] } в search_tag["certificate_manufacturer", "video"]', () => {
        const params = { has_video: 'true', search_tag: [ 'certificate_manufacturer' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'certificate_manufacturer', 'video' ] });
    });

    it('должен преобразовать {has_video: "true", search_tag: "certificate_manufacturer" } в search_tag["certificate_manufacturer", "video"]', () => {
        const params = { has_video: 'true', search_tag: [ 'certificate_manufacturer' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'certificate_manufacturer', 'video' ] });
    });
});

describe('online_view', () => {
    it('должен преобразовать online_view=true в search_tag["online_view_available"]', () => {
        const params = { online_view: true };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'online_view_available' ] });
    });

    it('должен преобразовать online_view="true" в search_tag["online_view_available"]', () => {
        const params = { online_view: 'true' };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'online_view_available' ] });
    });

    it('должен преобразовать {online_view: "true", search_tag: ["vin_checked"] } в search_tag["vin_checked", "online_view_available"]', () => {
        const params = { online_view: 'true', search_tag: [ 'vin_checked' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'vin_checked', 'online_view_available' ] });
    });

    it('должен преобразовать {online_view: "true", search_tag: "vin_checked" } в search_tag["vin_checked", "online_view_available"]', () => {
        const params = { online_view: 'true', search_tag: [ 'vin_checked' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'vin_checked', 'online_view_available' ] });
    });
});

describe('has_history', () => {
    it('должен преобразовать has_history=true в search_tag["vin_offers_history"]', () => {
        const params = { has_history: true };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'vin_offers_history' ] });
    });

    it('должен преобразовать has_history="true" в search_tag["vin_offers_history"]', () => {
        const params = { has_history: 'true' };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'vin_offers_history' ] });
    });

    // eslint-disable-next-line max-len
    it('должен преобразовать {has_history: "true", search_tag: "certificate_manufacturer" } в search_tag["certificate_manufacturer", "vin_offers_history"]', () => {
        const params = { has_history: 'true', search_tag: [ 'certificate_manufacturer' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'certificate_manufacturer', 'vin_offers_history' ] });
    });
});

describe('certificate', () => {
    it('должен преобразовать search_tag: ["certificate_manufacturer"] в search_tag["certificate_manufacturer"]', () => {
        const params = { search_tag: [ 'certificate_manufacturer' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'certificate_manufacturer' ] });
    });

    it('должен преобразовать search_tag: ["certificate_audi"] в search_tag["certificate_manufacturer"]', () => {
        const params = { search_tag: [ 'certificate_audi' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'certificate_manufacturer' ] });
    });

    it('должен преобразовать search_tag: ["certificate_autoru"] в search_tag[]', () => {
        const params = { search_tag: [ 'certificate_autoru' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [] });
    });

    it('должен преобразовать search_tag: ["certificate_autoru", "vin_checked"] в search_tag["vin_checked"]', () => {
        const params = { search_tag: [ 'certificate_autoru', 'vin_checked' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'vin_checked' ] });
    });
});

describe('on_credit', () => {
    it('должен преобразовать on_credit=true в search_tag["allowed_for_credit"]', () => {
        const params = { on_credit: true };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'allowed_for_credit' ], on_credit: true });
    });

    it('должен преобразовать on_credit="true" в search_tag["allowed_for_credit"]', () => {
        const params = { on_credit: 'true' };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'allowed_for_credit' ], on_credit: true });
    });

    // eslint-disable-next-line max-len
    it('должен преобразовать {on_credit: "true", search_tag: "certificate_manufacturer" } в search_tag["certificate_manufacturer", "allowed_for_credit"]', () => {
        const params = { on_credit: 'true', search_tag: [ 'certificate_manufacturer' ] };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ search_tag: [ 'certificate_manufacturer', 'allowed_for_credit' ], on_credit: true });
    });
});

describe('only_official', () => {
    it('должен оставить true', () => {
        const params = { category: 'cars', section: 'new', only_official: true };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ category: 'cars', state_group: 'NEW', only_official: true });
    });

    it('должен заменить false на true', () => {
        const params = { category: 'cars', section: 'new', only_official: false };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ category: 'cars', state_group: 'NEW', only_official: true });
    });

    it('не должен подставлять true для used', () => {
        const params = { category: 'cars', section: 'used' };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ category: 'cars', state_group: 'USED' });
    });

    it('не должен подставлять true для moto', () => {
        const params = { category: 'moto', section: 'new' };
        const requestParams = prepareRequestParams({
            params: params,
            context: contextMock,
        }, false);
        expect(requestParams).toEqual({ category: 'moto', state_group: 'NEW' });
    });
});

describe('moto_type', () => {
    const TESTS = [
        {
            params: { moto_type: [ 'CHOPPER', 'MINIBIKE' ] },
            result: { moto_type: [ 'CHOPPER', 'MINIBIKE' ] },
        },

        {
            params: { moto_type: [ 'CHOPPER', 'MINIBIKE', 'SPORTBIKE', 'SPORTTOURISM', 'SUPERSPORT', 'SPORT_GROUP' ] },
            result: { moto_type: [ 'CHOPPER', 'MINIBIKE', 'SPORTBIKE', 'SPORTTOURISM', 'SUPERSPORT' ] },
        },

        {
            params: { moto_type: [ 'CHOPPER', 'MINIBIKE', 'SPORTBIKE', 'SPORT_GROUP' ] },
            result: { moto_type: [ 'CHOPPER', 'MINIBIKE', 'SPORTBIKE', 'SPORTTOURISM', 'SUPERSPORT' ] },
        },
    ];

    TESTS.forEach(test =>
        it(JSON.stringify(test.params), () => {
            expect(prepareRequestParams({
                params: test.params,
                context: contextMock,
            }, false)).toEqual(test.result);
        }),
    );
});

describe('catalog_filter', () => {
    const TESTS = [
        {
            params: { catalog_filter: [ {} ] },
            result: { catalog_filter: [] },
        },
        {
            params: { exclude_catalog_filter: [ {} ] },
            result: { exclude_catalog_filter: [] },
        },
        {
            params: { catalog_filter: [ { mark: 'AUDI' } ] },
            result: { catalog_filter: [ 'mark=AUDI' ] },
        },
        {
            params: { exclude_catalog_filter: [ { mark: 'AUDI' } ] },
            result: { exclude_catalog_filter: [ 'mark=AUDI' ] },
        },
        {
            params: { catalog_filter: [ { mark: 'AUDI' }, { mark: 'BMW' } ], exclude_catalog_filter: [ { mark: 'BMW', model: 'X5' } ] },
            result: { catalog_filter: [ 'mark=AUDI', 'mark=BMW' ], exclude_catalog_filter: [ 'mark=BMW,model=X5' ] },
        },
        {
            params: {
                catalog_filter: [ { mark: 'AUDI', model: 'A8', nameplate_name: 'a8' } ],
                exclude_catalog_filter: [ { mark: 'BMW', model: 'X5', nameplate_name: 'x5' } ],
            },
            result: { catalog_filter: [ 'mark=AUDI,model=A8,nameplate_name=--' ], exclude_catalog_filter: [ 'mark=BMW,model=X5,nameplate_name=--' ] },
        },
        {
            params: {
                catalog_filter: [ { mark: 'AUDI', model: 'A8', nameplate_name: 'long' } ],
                exclude_catalog_filter: [ { mark: 'BMW', model: 'X5', nameplate_name: 'x5-new' } ],
            },
            result: { catalog_filter: [ 'mark=AUDI,model=A8,nameplate_name=long' ], exclude_catalog_filter: [ 'mark=BMW,model=X5,nameplate_name=x5-new' ] },
        },
    ];
    TESTS.forEach((test) => it(JSON.stringify(test.params), () => {
        expect(prepareRequestParams({
            context: contextMock,
            params: test.params,
        }, false)).toEqual(test.result);
    }));
});

describe('trailer_type', () => {
    const TESTS = [
        {
            params: { trailer_type: [
                'CONTAINER_TANK',
                'SWAP_BODY_ALL',
            ] },
            result: { trailer_type: [
                'CONTAINER_TANK',
                'ISOTHERMAL',
                'BULK_CARGO',
                'SB_TARPAULIN',
                'SB_PLATFORM',
                'SB_REFRIGERATOR',
                'SB_VAN',
                'SPECIAL',
            ] },
        },
    ];

    TESTS.forEach((test) => it(JSON.stringify(test.params), () => {
        expect(prepareRequestParams({
            params: test.params,
            context: contextMock,
        }, false)).toEqual(test.result);
    }));
});

describe('transmission', () => {
    const TESTS = [
        {
            params: { transmission: 'AUTO' },
            result: { transmission: [
                'AUTOMATIC',
                'ROBOT',
                'VARIATOR',
            ] },
        },
        {
            params: { transmission: [ 'AUTO', 'ROBOT' ] },
            result: { transmission: [
                'ROBOT',
                'AUTOMATIC',
                'VARIATOR',
            ] },
        },
    ];

    TESTS.forEach((test) => it(JSON.stringify(test.params), () => {
        expect(prepareRequestParams({
            params: test.params,
            context: contextMock,
        }, false)).toEqual(test.result);
    }));
});

describe('сортировки', () => {
    it('должен подставить дефолтную сортировку', () => {
        const result = prepareRequestParams({
            params: {},
            context: contextMock,
        }, true);
        expect(result.sort).toEqual('fresh_relevance_1-desc');
    });

    it('должен сохранять сортировку, если она не дефолтная', () => {
        const result = prepareRequestParams({
            params: { sort: 'alphabet-asc' },
            context: contextMock,
        }, true);
        expect(result.sort).toEqual('alphabet-asc');
    });
});
