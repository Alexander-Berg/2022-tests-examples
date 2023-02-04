const shouldShowBodyTags = require('auto-core/react/lib/listingSearchParameters/shouldShowBodyTags');

it('False если категория не cars', () => {
    const searchParams = {
        section: 'all',
        category: 'trucks',
        sort: 'price-asc',
    };

    expect(shouldShowBodyTags(searchParams)).toEqual(false);
});

it('True если указано поколение', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter:
            [
                {
                    mark: 'MERCEDES',
                    model: 'E_KLASSE',
                    generation: '3484110',
                },
            ],
    };

    expect(shouldShowBodyTags(searchParams)).toEqual(true);
});

it('True если указан шильд', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter: [
            {
                mark: 'BMW',
                model: '5ER',
                nameplate_name: '520',
            },
        ],
    };

    expect(shouldShowBodyTags(searchParams)).toEqual(true);
});

it('False если больше одного фильтра по марке', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter: [
            {
                mark: 'VAZ',
            },
            {
                mark: 'AUDI',
            },
        ],
    };

    expect(shouldShowBodyTags(searchParams)).toEqual(false);
});

it('False если больше одного типа кузова из разных групп', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'SEDAN' ],
    };
    expect(shouldShowBodyTags(searchParams)).toEqual(false);
});

it('False если все типы кузова из одной группы, но группа не полная', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS' ],
    };
    expect(shouldShowBodyTags(searchParams)).toEqual(false);
});

it('True если один тип кузова', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        body_type_group: [ 'SEDAN' ],
    };
    expect(shouldShowBodyTags(searchParams)).toEqual(true);
});

it('True если все типы кузова из одной группы', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        body_type_group: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
    };
    expect(shouldShowBodyTags(searchParams)).toEqual(true);
});

it('True если нет типа кузова', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
    };
    expect(shouldShowBodyTags(searchParams)).toEqual(true);
});

it('True если один фильтр по марке', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter: [
            {
                mark: 'VAZ',
            },
        ],
    };

    expect(shouldShowBodyTags(searchParams)).toEqual(true);
});
