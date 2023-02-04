const redirectCardGroupOptionsCompare = require('./redirect-card-group-options-compare');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего делать для роута не card-group-options', () => {
    expect.assertions(1);

    req.router = {
        route: {
            getName: () => 'listing',
        },
        params: {},
    };
    redirectCardGroupOptionsCompare(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не должен средиректить для роута card-group-options без page_from', () => {
    expect.assertions(1);

    req.router = {
        route: {
            getName: () => 'card-group-options',
        },
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [
                {
                    mark: 'SKODA',
                    model: 'KAROQ',
                    generation: '21010081',
                    configuration: '21010112',
                },
            ],
        },
    };

    redirectCardGroupOptionsCompare(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('не должен средиректить для роута card-group-options c tab_id=compare и page_from=card_compare', () => {
    expect.assertions(1);

    req.router = {
        route: {
            getName: () => 'card-group-options',
        },
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [
                {
                    mark: 'SKODA',
                    model: 'KAROQ',
                    generation: '21010081',
                    configuration: '21010112',
                },
            ],
            page_from: 'card_compare',
            tab_id: 'compare',
        },
    };

    redirectCardGroupOptionsCompare(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить для роута card-group-options c page_from=card_compare', () => {
    expect.assertions(1);

    req.router = {
        route: {
            getName: () => 'card-group-options',
        },
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [
                {
                    mark: 'SKODA',
                    model: 'KAROQ',
                    generation: '21010081',
                    configuration: '21010112',
                },
            ],
            page_from: 'card_compare',
        },
    };

    redirectCardGroupOptionsCompare(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'CARD_GROUP_OPTIONS_COMPARE',
            data: {
                location: '/cars/new/group/skoda/karoq/21010081-21010112/options/compare/',
                status: 301,
            },
        });
    });
});
