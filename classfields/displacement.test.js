jest.mock('./getDisplacementValues', () => () => [ 1000 ]);
jest.mock('./getDisplacementValuesCentimeters', () => () => [ 1000 ]);

const TESTS = [
    {
        arguments: [ { category: 'cars' } ],
        result: [ { val: 1000, text: '1.0 л' } ],
    },
    {
        arguments: [ { category: 'moto' } ],
        result: [ { val: 1000, text: '1000 см³' } ],
    },
];

const displacement = require('./displacement');

TESTS.forEach((test) => {
    it(`должен вернуть правильные значения для (${ JSON.stringify(test.arguments) })`,
        () => expect(displacement(...test.arguments)).toEqual(test.result),
    );
});
