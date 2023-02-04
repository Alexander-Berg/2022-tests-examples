module.exports = [
    {
        markModelNameplates: [ '' ],
        mmmInfo: [
            { mark: null, models: [] },
        ],
    },
    {
        markModelNameplates: [ '' ],
        mmmInfo: [ { } ],
    },
    {
        markModelNameplates: [ 'AUDI#A4' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4##1', 'AUDI#A4##2' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [ '1', '2' ], nameplates: [] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4#4#1', 'AUDI#A4#4#2', 'AUDI#A4#5#1', 'AUDI#A4#5#2' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [ '1', '2' ], nameplates: [ '4', '5' ] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4#5#1', 'AUDI#A4#5#3', 'AUDI#A3#6#2' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [ '1', '3' ], nameplates: [ '5' ] }, { id: 'A3', generations: [ '2' ], nameplates: [ '6' ] } ] },
        ],
    },
    {
        markModelNameplates: [ 'BMW#100##1', 'AUDI#100##1' ],
        mmmInfo: [
            { mark: 'BMW', models: [ { id: '100', generations: [ '1' ], nameplates: [] } ] },
            { mark: 'AUDI', models: [ { id: '100', generations: [ '1' ], nameplates: [] } ] },
        ],
    },
];
