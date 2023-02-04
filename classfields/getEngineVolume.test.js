const getEngineVolume = require('./getEngineVolume');

const TESTS = [
    { complectation: { volume: undefined }, result: '' },
    { complectation: { volume: 0 }, result: '0.0' },
    { complectation: { volume: 1 }, result: '0.0' },
    { complectation: { volume: 15 }, result: '0.0' },
    { complectation: { volume: 144 }, result: '0.1' },
    { complectation: { volume: 145 }, result: '0.2' },
    { complectation: { volume: 1545 }, result: '1.6' },
    { complectation: { volume: 1749 }, result: '1.8' },
    { complectation: { volume: 1749, 'volume-litres': 1 }, result: '1.8' },
    { complectation: { 'volume-litres': 1 }, result: '1.0' },
    { complectation: { 'volume-litres': 1.5 }, result: '1.5' },
    { complectation: { 'volume-litres': 10 }, result: '10.0' },
];

TESTS.forEach((testCase) => {
    const { complectation, result } = testCase;

    it(`should return ${ result } for volume: ${ complectation.volume }`, () => {
        expect(getEngineVolume(complectation)).toEqual(result);
    });
});
