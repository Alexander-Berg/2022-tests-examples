const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const equipmentFilters = require('./equipmentFilters');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

[
    'catalog_equipment',
    'currency',
    'page',
    'page_size',
    'pinned_offer_id',
    'sort',
    'top_days',
].forEach((param) => {
    it(`должен исключить параметр ${ param } из запроса`, () => {
        publicApi
            .get('/1.0/search/cars/equipment-filters')
            .query({
                category: 'cars',
                context: 'listing',
            })
            .reply(200, {
                categories: [ 'categories' ],
                popular: [ 'popular' ],
            });

        const params = {
            category: 'cars',
            [ param ]: 'value',
        };

        return de.run(equipmentFilters, { context, params })
            .then((result) => {
                expect(result).toEqual({
                    categories: [ 'categories' ],
                    popular: [ 'popular' ],
                });
            });
    });

});
