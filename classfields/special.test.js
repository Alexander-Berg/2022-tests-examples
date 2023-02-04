const special = require('./special');

describe('.getRelatedParams', function() {

    const TESTS = [
        {
            params: {},
            result: null,
        },
        {
            params: { 'mark-model-nameplate': [ 'JEEP' ] },
            result: null,
        },
        {
            params: { 'mark-model-nameplate': [ 'VENDOR1' ] },
            result: null,
        },
        {
            params: { 'mark-model-nameplate': [ 'AUDI#A4' ] },
            result: { mark: 'AUDI', model: 'A4' },
        },
        {
            params: { 'mark-model-nameplate': [ 'AUDI#A4' ], year_from: '2010', year_to: '2015' },
            result: { mark: 'AUDI', model: 'A4', year_from: '2010', year_to: '2015' },
        },
        {
            params: { 'mark-model-nameplate': [ 'AUDI#A4##12345' ] },
            result: { mark: 'AUDI', model: 'A4', super_gen: '12345' },
        },
        {
            params: { 'mark-model-nameplate': [ 'AUDI#A4##12345' ], year_from: '2010', year_to: '2015' },
            result: { mark: 'AUDI', model: 'A4', super_gen: '12345', year_from: '2010', year_to: '2015' },
        },
        {
            // если много марок-моделей, то берем первую
            params: { 'mark-model-nameplate': [ 'AUDI#A4', 'BMW#3ER' ] },
            result: { mark: 'AUDI', model: 'A4' },
        },
        {
            // если много марок-моделей, то берем первую
            params: { 'mark-model-nameplate': [ 'AUDI', 'BMW#3ER' ] },
            result: { mark: 'BMW', model: '3ER' },
        },
        {
            // если много марок-моделей, то берем первую
            params: { 'mark-model-nameplate': [ 'VENDOR1', 'AUDI#A4' ] },
            result: { mark: 'AUDI', model: 'A4' },
        },
    ];

    TESTS.forEach(function(testCase, i) {
        const name = [
            'test #' + i,
            JSON.stringify(testCase.params),
            '->',
            JSON.stringify(testCase.result),
        ].join(' ');
        it(name, function() {
            expect(special.getRelatedParams(testCase.params)).toEqual(testCase.result);
        });

    });

});
