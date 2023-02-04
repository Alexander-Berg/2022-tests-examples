const generateCombinations = require('./generateCombinations');

const TEST_PARAMS_LIST = [
    'section',
    'geo_id',
    'category',
    'mark',
    'model',
];

it('Геренит все комбинации длины 2 из массива', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 2)).toMatchSnapshot();
});
it('Геренит все комбинации длины 3 из массива', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 3)).toMatchSnapshot();
});
it('Геренит все комбинации длины 4 из массива', () => {
    expect(generateCombinations(TEST_PARAMS_LIST, 4)).toMatchSnapshot();
});
