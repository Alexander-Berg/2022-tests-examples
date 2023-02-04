const getLinkParamsToGroup = require('./getLinkParamsToGroup');
const offer = require('autoru-frontend/mockData/state/groupCard.mock');

it('должен вернуть параметры ссылки на группу, если передан только оффер', () => {
    expect(getLinkParamsToGroup(offer)).toStrictEqual(
        {
            category: 'cars',
            configuration_id: '21398651',
            mark: 'BMW',
            model: '3ER',
            section: 'new',
            super_gen: '21398591',
        },
    );
});

it('должен добавить поисковые параметры за исключением запрещенных в параметры ссылки на группу', () => {
    const searchParams = {
        transmission: [ 'AUTO' ],
        sort: 'fresh_relevance_1-desc',
        tech_param_id: '123',
        complectation_id: '777',
        complectation_name: 'test',
        group_by: [ 'CONFIGURATION' ],
        filters_mode: 'guru',
        page: 7,
    };
    expect(getLinkParamsToGroup(offer, searchParams)).toStrictEqual(
        {
            category: 'cars',
            configuration_id: '21398651',
            mark: 'BMW',
            model: '3ER',
            section: 'new',
            super_gen: '21398591',
            transmission: [ 'AUTO' ],
        },
    );
});

it('не должен включать вопросы автогуру в параметры ссылки на группу', () => {
    const searchParams = {
        gurua0: [ 'answer' ],
        gurua1: [],
        gurua2: [],
    };
    expect(getLinkParamsToGroup(offer, searchParams)).toStrictEqual(
        {
            category: 'cars',
            configuration_id: '21398651',
            mark: 'BMW',
            model: '3ER',
            section: 'new',
            super_gen: '21398591',
        },
    );
});

it('не должен включать catalog_filter в параметры ссылки на группу по умолчанию', () => {
    const searchParams = {
        catalog_filter: [ { mark: 'BMW', model: '3ER' } ],
    };
    expect(getLinkParamsToGroup(offer, searchParams)).toStrictEqual(
        {
            category: 'cars',
            configuration_id: '21398651',
            mark: 'BMW',
            model: '3ER',
            section: 'new',
            super_gen: '21398591',
        },
    );
});

it(`должен добавить catalog_filter в параметры ссылки на группу, если передан true третьим параметром`, () => {
    const searchParams = {
        catalog_filter: [ { mark: 'BMW', model: '3ER' } ],
    };
    expect(getLinkParamsToGroup(offer, searchParams, true)).toStrictEqual(
        {
            category: 'cars',
            configuration_id: '21398651',
            mark: 'BMW',
            model: '3ER',
            section: 'new',
            super_gen: '21398591',
            catalog_filter: [
                'mark=BMW,model=3ER,generation=21398591,configuration=21398651,tech_param=21605511,complectation_name=320i xDrive',
            ],
        },
    );
});
