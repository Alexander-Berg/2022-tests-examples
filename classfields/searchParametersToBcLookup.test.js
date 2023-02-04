const searchParametersToBcLookup = require('./searchParametersToBcLookup');

const TEST = [
    { params: {}, bc_lookup: '' },

    {
        params: {
            category: 'moto',
            moto_category: 'MOTORCYCLE',
            mark_model_nameplate: [ 'HONDA#CBR_600_F' ],
        },
        bc_lookup: [ 'MOTORCYCLE#HONDA#CBR_600_F' ],
    },
    {
        params: {
            category: 'moto',
            moto_category: 'MOTORCYCLE',
            catalog_filter: [ { mark: 'HONDA', model: 'CBR_600_F' } ],
        },
        bc_lookup: [ 'MOTORCYCLE#HONDA#CBR_600_F' ],
    },

    {
        params: {
            category: 'CaRs',
            mark_model_nameplate: [ 'AUDI#A4##123' ],
        },
        bc_lookup: [ 'AUDI#A4#123' ],
    },
    {
        params: {
            category: 'CaRs',
            catalog_filter: [ { mark: 'AUDI', model: 'A4', generation: '123' } ],
        },
        bc_lookup: [ 'AUDI#A4#123' ],
    },

    {
        params: {
            mark_model_nameplate: [ 'AUDI#A4##7754683' ],
            configuration_id: '20683225',
        },
        bc_lookup: [ 'AUDI#A4#7754683#20683225' ],
    },
    {
        params: {
            catalog_filter: [ { mark: 'AUDI', model: 'A4', generation: '7754683' } ],
            configuration_id: '20683225',
        },
        bc_lookup: [ 'AUDI#A4#7754683#20683225' ],
    },

    {
        params: {
            mark_model_nameplate: [ 'AUDI#A4##7754683', 'AUDI#A3' ],
            configuration_id: '20683225',
        },
        bc_lookup: [ 'AUDI#A4#7754683', 'AUDI#A3' ],
    },
    {
        params: {
            catalog_filter: [ { mark: 'AUDI', model: 'A4', generation: '7754683' }, { mark: 'AUDI', model: 'A3' } ],
            configuration_id: '20683225',
        },
        bc_lookup: [ 'AUDI#A4#7754683', 'AUDI#A3' ],
    },
    {
        params: {
            exclude_catalog_filter: [ { mark: 'AUDI', model: 'A3' } ],
        },
        bc_lookup: [ 'AUDI#A3' ],
    },
    {
        params: {
            exclude_catalog_filter: [ { mark: 'AUDI', model: 'A3' } ],
            catalog_filter: [ { mark: 'BMW' } ],
        },
        bc_lookup: [ 'BMW', 'AUDI#A3' ],
    },
];

TEST.forEach((testCase, i) => {
    it(`${ i }. should convert "${ JSON.stringify(testCase.params) }" to "${ testCase.bc_lookup }"`, () => {
        expect(searchParametersToBcLookup(testCase.params)).toEqual(testCase.bc_lookup);
    });
});
