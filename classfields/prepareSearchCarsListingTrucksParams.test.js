const prepare = require('./prepareSearchCarsListingTrucksParams');

it('должен вернуть дефолтные параметры для пустого объекта', () => {
    const result = prepare({});
    expect(result).toEqual({ category: 'trucks', image: true, trucks_category: 'LCV', with_delivery: 'BOTH' });
});

it('должен убрать поколения из МММ', () => {
    const result = prepare({
        params: {
            catalog_filter: [ { mark: 'FORD', model: 'FOCUS', generation: '123' } ],
            section: 'used',
        },
    });
    expect(result).toEqual({
        category: 'trucks',
        catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
        section: 'used',
        image: true,
        trucks_category: 'LCV',
        with_delivery: 'BOTH',
    });
});

it('должен убрать множественный МММ', () => {
    const result = prepare({
        params: {
            catalog_filter: [ { mark: 'FORD', model: 'FOCUS', generation: '123' }, { mark: 'FORD', model: 'FOCUS', generation: '124' } ],
            section: 'used',
        },
    });
    expect(result).toEqual({
        category: 'trucks',
        catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
        section: 'used',
        image: true,
        trucks_category: 'LCV',
        with_delivery: 'BOTH',
    });
});

it('должен заnullить параметры, которые не нужны', () => {
    const result = prepare({
        params: {
            gear_type: 'REAR_DRIVE',
            catalog_filter: [ { mark: 'VAZ', model: 'LARGUS' } ],
            section: 'used',
        },
    });
    expect(result).toEqual({
        category: 'trucks',
        gear_type: null,
        catalog_filter: [ { mark: 'VAZ', model: 'LARGUS' } ],
        section: 'used',
        image: true,
        trucks_category: 'LCV',
        with_delivery: 'BOTH',
    });
});
