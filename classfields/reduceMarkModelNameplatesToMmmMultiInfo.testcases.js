module.exports = [
    {
        markModelNameplates: [ 'AUDI#A4' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI', 'AUDI#A4' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4', 'AUDI' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [], nameplates: [] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4', 'AUDI#A4' ],
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
        markModelNameplates: [ 'AUDI#A4#1#1', 'AUDI#A4#2#1' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [ '1' ], nameplates: [ '1', '2' ] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4#5#1', 'AUDI#A4##2', 'AUDI#A4##3' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [ '1', '2', '3' ], nameplates: [ '5' ] } ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4#5#1', 'AUDI#A3##2', 'AUDI#A4##3', 'AUDI#A3#6' ],
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
    {
        markModelNameplates: [ 'AUDI#A4##2' ],
        mmmInfo: [
            { mark: 'AUDI', models: [ { id: 'A4', generations: [ '2' ], nameplates: [] } ] },
        ],
    },
];
