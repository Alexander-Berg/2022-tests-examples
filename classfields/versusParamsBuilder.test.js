const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const versusParamsBuilder = require('./versusParamsBuilder');

let params;
let context;
let req;

beforeEach(() => {
    params = {
        first_mark: 'hyundai',
        first_model: 'sonata',
        second_mark: 'kia',
        second_model: 'rio',
    };
    req = createHttpReq();
    context = createContext({ req });
});

describe('должен правильно сформировать параметры для versus-контроллера', () => {
    it('без nameplate', () => {
        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SONATA' },
                { mark: 'KIA', model: 'RIO' },
            ],
        });
    });

    it('с nameplate для одной модели', () => {
        params.first_nameplate = 'tagaz';

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SONATA', nameplate_name: 'tagaz' },
                { mark: 'KIA', model: 'RIO' },
            ],
        });
    });

    it('с nameplate для обеих моделей', () => {
        params.first_nameplate = 'tagaz';
        params.second_nameplate = 'x_line';

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SONATA', nameplate_name: 'tagaz' },
                { mark: 'KIA', model: 'RIO', nameplate_name: 'x_line' },
            ],
        });
    });
});

describe('должен правильно сформировать параметры для versus-контроллера с учётом кук', () => {
    const userSelectedParams = [
        {
            mark: 'HYUNDAI',
            model: 'SONATA',
            generation: '21104772',
            configuration: '21104826',
            complectation: '21105127',
            tech_param: '21104909',
        },
        {
            mark: 'KIA',
            model: 'RIO',
            generation: '21028015',
        },
    ];

    it('не должен упасть', () => {
        context.req.router.params = { 'versus-catalog-filter': 'foo-bar' };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SONATA' },
                { mark: 'KIA', model: 'RIO' },
            ],
        });
    });

    it('должен подтянуть параметры из кук', () => {
        context.req.cookies = { 'versus-catalog-filter': JSON.stringify(userSelectedParams) };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: userSelectedParams,
        });
    });

    it('не должен брать параметры из кук', () => {
        params.first_model = 'solaris';
        params.second_model = 'sorento';

        context.req.cookies = { 'versus-catalog-filter': JSON.stringify(userSelectedParams) };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SOLARIS' },
                { mark: 'KIA', model: 'SORENTO' },
            ],
        });
    });
});

describe('должен правильно сформировать параметры для versus-контроллера с учётом query-параметров', () => {
    const userSharedParams = [
        {
            mark: 'HYUNDAI',
            model: 'SONATA',
            generation: '21796089',
            configuration: '21796128',
            complectation: '21819952',
            tech_param: '21796132',
        },
        {
            mark: 'KIA',
            model: 'RIO',
            generation: '22500704',
            configuration: '22500752',
            complectation: '22501103',
            tech_param: '22500756',
        },
    ];

    it('не должен упасть, если catalog_filter не валидный', () => {
        context.req.router.params = {
            ...params,
            catalog_filter: {},
        };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SONATA' },
                { mark: 'KIA', model: 'RIO' },
            ],
        });
    });

    it('должен подтянуть из catalog_filter параметры, если они есть', () => {
        context.req.router.params = {
            ...params,
            catalog_filter: userSharedParams,
        };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: userSharedParams,
        });
    });

    it('должен подтянуть из catalog_filter параметры только для тачек из pathname', () => {
        context.req.router.params = {
            ...params,
            catalog_filter: [
                {
                    mark: 'HYUNDAI',
                    model: 'SOLARIS',
                    generation: '21796089',
                    configuration: '21796128',
                },
                userSharedParams[1],
            ],
        };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: [
                { mark: 'HYUNDAI', model: 'SONATA' },
                userSharedParams[1],
            ],
        });
    });

    it('должен подтянуть из catalog_filter только валидные параметры', () => {
        context.req.router.params = {
            ...params,
            catalog_filter: userSharedParams.map((params) => ({
                ...params,
                i_am_bad_user: 'is_true',
            })),
        };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: userSharedParams.map((params) => ({
                ...params,
                i_am_bad_user: undefined,
            })),
        });
    });

    it('не должен брать параметры из кук, если в query-параметрах есть валидный catalog_filter', () => {
        context.req.cookies = { 'versus-catalog-filter': JSON.stringify([
            {
                mark: 'HYUNDAI',
                model: 'SONATA',
                generation: '21104772',
                configuration: '21104826',
                complectation: '21105127',
                tech_param: '21104909',
            },
            {
                mark: 'KIA',
                model: 'RIO',
                generation: '21028015',
            },
        ]) };

        context.req.router.params = {
            ...params,
            catalog_filter: userSharedParams,
        };

        expect(versusParamsBuilder({ params, context })).toEqual({
            category: 'cars',
            catalog_filter: userSharedParams,
        });
    });
});
