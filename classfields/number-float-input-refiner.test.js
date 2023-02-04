const numberFloatInputRefiner = require('./number-float-input-refiner');

const TEST_CASES = [
    {
        value: null,
        expected: '',
    },

    {
        value: undefined,
        expected: '',
    },

    {
        value: 'not a number',
        expected: '',
    },

    {
        value: 0,
        expected: '0',
    },

    {
        value: '.',
        expected: '0.',
    },

    {
        value: '0.',
        expected: '0.',
    },

    {
        value: '00.',
        expected: '0.',
    },

    {
        value: '00.00',
        expected: '0.00',
    },

    {
        value: '001',
        expected: '1',
    },

    {
        value: '10,00',
        expected: '10.00',
    },

    {
        value: '4.',
        expected: '4.',
    },

    {
        value: '.1',
        expected: '0.1',
    },

    {
        value: '10.06',
        expected: '10.06',
    },

    {
        value: '10_24w',
        expected: '1024',
    },

    {
        value: '102.4w',
        expected: '102.4',
    },
];

TEST_CASES.forEach(({ expected, value }) => {
    it(`number-float-input-refiner должен вернуть ${ expected } для ${ value } `, () => {
        expect(numberFloatInputRefiner(value)).toEqual(expected);
    });
});

it(`number-float-input-refiner должен оставить 3 знака после запятой`, () => {
    expect(numberFloatInputRefiner(30.0085, 3)).toEqual('30.008');
});
