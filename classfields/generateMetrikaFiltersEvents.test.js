const generateMetrikaFiltersEvents = require('./generateMetrikaFiltersEvents');

const TESTS = [
    {
        args: [ { price_from: 100, price_to: 10000 }, 'set' ],
        result: [ 'filter', [
            [ 'price_from', 'set', 100 ],
            [ 'price_to', 'set', 10000 ],
        ] ],
    },
    {
        args: [ { body_type_group: [ 'SEDAN', 'HATCHBACK' ], price_to: 10000 }, 'send' ],
        result: [ 'filter', [
            [ 'body_type_group', 'send', 'HATCHBACK,SEDAN' ],
            [ 'price_to', 'send', 10000 ],
        ] ],
    },
    {
        args: [ { 'do': 10000 }, 'send' ],
        result: [ 'filter', [
            [ 'price-presets', 'send', 10000 ],
        ] ],
    },
    {
        args: [ { 'do': 10000 } ],
        result: [ 'filter', [
            [ 'price-presets', 10000 ],
        ] ],
    },

    {
        args: [ { 'mmm-filter': {
            catalog_filter: [
                { mark: 'BMW' },
                { mark: 'BMW', model: 'X3' },
                { mark: 'AUDI', model: '1', nameplate: '2', generation: '3' },
                { mark: 'AUDI', model: '11', nameplate: '22' },
                {},
            ],
            exclude_catalog_filter: [
                { mark: 'BMW', model: 'X3', generation: '1' },
                { mark: 'LAND_ROVER', model: 'DISCOVERY' },
                { vendor: 'VENDOR1' },
            ],
        } } ],
        result: [ 'filter', [
            [
                'mmm-filter',
                'catalog_filter=' +
                'mark=BMW,mark=BMW,model=X3,' +
                'mark=AUDI,model=1,nameplate=2,generation=3,' +
                'mark=AUDI,model=11,nameplate=22,' +
                'exclude_catalog_filter=' +
                'mark=BMW,model=X3,generation=1,' +
                'mark=LAND_ROVER,model=DISCOVERY,' +
                'vendor=VENDOR1',
            ],
            [ 'marks_count', 2 ],
            [ 'models_count', 3 ],
            [ 'nameplates_count', 2 ],
            [ 'generations_count', 1 ],
            [ 'exclude_marks_count', 3 ],
            [ 'exclude_models_count', 2 ],
            [ 'exclude_nameplates_count', 0 ],
            [ 'exclude_generations_count', 1 ],
        ] ],
    },
];

TESTS.forEach(testCase => {
    it(`должен правильно преобразовать ${ JSON.stringify(testCase.args) }`, () => {
        expect(generateMetrikaFiltersEvents(...testCase.args)).toEqual(testCase.result);
    });
});

it(`не должен мутировать переданный массив`, () => {
    const searchParameters = {
        body_type_group: [ 'SEDAN', 'HATCHBACK' ],
    };
    generateMetrikaFiltersEvents(searchParameters);

    expect(searchParameters.body_type_group).toEqual([ 'SEDAN', 'HATCHBACK' ]);
});
