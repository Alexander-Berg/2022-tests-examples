const shouldShowEngineTags = require('auto-core/react/lib/listingSearchParameters/shouldShowEngineTags');

const APPROVED_ENGINE_TYPES = [ 'GASOLINE', 'DIESEL' ];

it('False если категория не cars', () => {
    const searchParams = {
        section: 'all',
        category: 'trucks',
    };

    expect(shouldShowEngineTags(searchParams, APPROVED_ENGINE_TYPES)).toEqual(false);
});

it('True если указано поколение', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        catalog_filter:
            [
                {
                    mark: 'MERCEDES',
                    model: 'E_KLASSE',
                    generation: '3484110',
                },
            ],
    };

    expect(shouldShowEngineTags(searchParams, APPROVED_ENGINE_TYPES)).toEqual(true);
});

it('True если указан шильд', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        catalog_filter: [
            {
                mark: 'BMW',
                model: '3ER',
                nameplate_name: '520',
            },
        ],
    };

    expect(shouldShowEngineTags(searchParams, APPROVED_ENGINE_TYPES)).toEqual(true);
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

    expect(shouldShowEngineTags(searchParams, APPROVED_ENGINE_TYPES)).toEqual(false);
});

it('False если выбраны все разрешенные типы двигателей', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter: [
            {
                mark: 'AUDI',
            },
        ],
        engine_group: [
            'GASOLINE',
            'DIESEL',
            'HYBRID',
        ],
    };

    expect(shouldShowEngineTags(searchParams, APPROVED_ENGINE_TYPES)).toEqual(false);
});

it('True если выбран один разрешенный тип двигателя', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter: [
            {
                mark: 'AUDI',
            },
        ],
        engine_group: [
            'GASOLINE',
            'HYBRID',
        ],
    };

    expect(shouldShowEngineTags(searchParams, APPROVED_ENGINE_TYPES)).toEqual(true);
});
