const getLinkParamsToGroupWithNameplates = require('./getLinkParamsToGroupWithNameplates');
const OFFER = require('autoru-frontend/mockData/state/groupCard.mock');

const SEARCH_PARAMS = {
    catalog_filter: [
        {
            mark: 'BMW',
            model: '3ER',
            nameplate_name: '320',
        }, {
            mark: 'BMW',
            model: '3ER',
            nameplate_name: '318',
        },
    ],
    section: 'new',
    category: 'cars',
};

it('Должен добавить шильды в catalog_filter', () => {
    const expected = {
        section: 'new',
        category: 'cars',
        mark: 'BMW',
        model: '3ER',
        configuration_id: '21398651',
        super_gen: '21398591',
        catalog_filter: [
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21605511',
            },
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21398903',
            },
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21398869',
            },
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21398791',
            },
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21605643',
            },
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21592423',
            },
            {
                mark: 'BMW',
                model: '3ER',
                generation: '21398591',
                configuration: '21398651',
                tech_param: '21592343',
            },
        ],
    };
    const result = getLinkParamsToGroupWithNameplates(OFFER, SEARCH_PARAMS);
    expect(result).toEqual(expected);
});

it('Должен вернуть без catalog_filter, если нет шильдов', () => {
    const expected = {
        category: 'cars',
        configuration_id: '21398651',
        mark: 'BMW',
        model: '3ER',
        section: 'new',
        super_gen: '21398591',
    };
    const searchParams = { ...SEARCH_PARAMS, catalog_filter: [ { mark: 'BMW', model: '3ER' } ] };
    const result = getLinkParamsToGroupWithNameplates(OFFER, searchParams);
    expect(result).toEqual(expected);
});
