const shouldShowGearTags = require('auto-core/react/lib/listingSearchParameters/shouldShowGearTags');

it('False если категория не cars', () => {
    const searchParams = {
        section: 'all',
        category: 'trucks',
    };

    expect(shouldShowGearTags(searchParams)).toEqual(false);
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

    expect(shouldShowGearTags(searchParams)).toEqual(true);
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

    expect(shouldShowGearTags(searchParams)).toEqual(true);
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

    expect(shouldShowGearTags(searchParams)).toEqual(false);
});

it('True если выбран один тип привода', () => {
    const searchParams = {
        section: 'all',
        category: 'cars',
        sort: 'price-asc',
        catalog_filter: [
            {
                mark: 'AUDI',
            },
        ],
        gear_type: [
            'FORWARD_CONTROL',
        ],
    };

    expect(shouldShowGearTags(searchParams)).toEqual(true);
});
