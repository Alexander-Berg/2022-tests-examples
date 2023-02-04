const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const offerGroupedMock = require('autoru-frontend/mockData/responses/offer-new-group.mock.json');

const cardGroupComplectations = require('./cardGroupComplectations');

const catalogTechParamMock = {
    entities: [
        {
            mark_info: {
            },
            model_info: {
            },
            super_gen: {
            },
            configuration: {
            },
            tech_param: {
                id: '21551925',
            },
        },
    ],
};

let params;
let context;
beforeEach(() => {
    params = {
        category: 'cars',
        section: 'new',
        catalog_filter: [
            {
                mark: 'KIA',
                model: 'SOUL',
                generation: '21551393',
                configuration: '21551904',
            },
        ],
    };
    context = createContext({
        req: createHttpReq(),
        res: createHttpRes(),
    });
});

it('должен вернуть список комплектаций', () => {
    const offers = Array(2).fill(offerGroupedMock);

    publicApi
        .get('/1.0/search/cars')
        .query(true)
        .reply(200, { offers });

    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(200, catalogTechParamMock);

    return de.run(cardGroupComplectations, { context, params }).then((result) => expect(result).toMatchSnapshot());
});
