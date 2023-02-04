module.exports = [
    {
        markModelNameplates: [ 'AUDI', 'AUDI#A4' ],
        mmmInfo: [
            { mark: 'AUDI', generations: [] },
            { mark: 'AUDI', model: 'A4', generations: [] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4', 'AUDI#A4' ],
        mmmInfo: [
            { mark: 'AUDI', model: 'A4', generations: [] },
            { mark: 'AUDI', model: 'A4', generations: [] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4##1', 'AUDI#A4##2' ],
        mmmInfo: [
            { mark: 'AUDI', model: 'A4', generations: [ '1', '2' ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4#5#1', 'AUDI#A4##2' ],
        mmmInfo: [
            { mark: 'AUDI', model: 'A4#5', generations: [ '1' ] },
            { mark: 'AUDI', model: 'A4', generations: [ '2' ] },
        ],
    },
    {
        markModelNameplates: [ 'AUDI#A4##2' ],
        mmmInfo: [
            { mark: 'AUDI', model: 'A4', generations: [ '2' ] },
        ],
    },
];
