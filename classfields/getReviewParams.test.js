const getReviewParams = require('./getReviewParams');

it('должен правильно заполнить reviews - params', () => {
    const params = {};
    const state = { breadcrumbsParams: {} };
    expect(getReviewParams(params, state)).toEqual({
        category: 'CARS',
        catalog_filter: [ {} ],
    });
});

it('должен правильно заполнить reviews - params, если есть марка, модель. поколение', () => {
    const params = {
        mark: 'audi',
        model: 'a1',
        super_gen: '123',
    };
    const state = { breadcrumbsParams: {} };
    expect(getReviewParams(params, state)).toEqual({
        mark: 'AUDI',
        model: 'A1',
        super_gen: '123',
        category: 'CARS',
        catalog_filter: [ {
            mark: 'AUDI',
            model: 'A1',
            generation: '123',
        } ],
    });
});

it('должен правильно заполнить reviews - params, если есть марка, модель и 2 поколения', () => {
    const params = {
        mark: 'audi',
        model: 'a1',
        super_gen: [ '123', '456' ],
    };
    const state = { breadcrumbsParams: {} };
    expect(getReviewParams(params, state)).toEqual({
        mark: 'AUDI',
        model: 'A1',
        super_gen: [ '123', '456' ],
        category: 'CARS',
        catalog_filter: [
            { mark: 'AUDI', model: 'A1', generation: '123' },
            { mark: 'AUDI', model: 'A1', generation: '456' },
        ],
    });
});

it('должен правильно заполнить reviews - params, если есть catalog_filter', () => {
    const params = {
        catalog_filter: [
            {
                mark: 'AUDI',
                model: 'A1',
                generation: '111',
            },
            {
                mark: 'AUDI',
                model: 'A1',
                generation: '222',
            },
        ],
    };
    const state = { breadcrumbsParams: {} };
    expect(getReviewParams(params, state)).toEqual({
        mark: 'AUDI',
        model: 'A1',
        super_gen: [ '111', '222' ],
        category: 'CARS',
        catalog_filter: [
            {
                mark: 'AUDI',
                model: 'A1',
                generation: '111',
            },
            {
                mark: 'AUDI',
                model: 'A1',
                generation: '222',
            },
        ],
    });
});
