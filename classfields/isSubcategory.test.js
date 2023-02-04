const isSubcategory = require('./isSubcategory');
const getCategoryHash = require('./getCategoryHash');

const TEST_CASES = [
    {
        input: { category: 'TRUCKS', section: [ 'USED', 'NEW' ], truck_class: 'COMMERCIAL' },
        output: true,
    },
    {
        input: { category: 'TRUCKS', section: [ 'USED' ] },
        output: true,
    },
    {
        input: { category: 'TRUCKS', section: [ 'NEW' ] },
        output: true,
    },
    {
        input: { category: 'MOTO', section: [ 'NEW' ] },
        output: false,
    },
];

TEST_CASES.forEach((testCase) => {
    it(`должен правильно определять, является ли тарифф подкатегорией - "${ getCategoryHash(testCase.input) }"`, () => {
        expect(isSubcategory(testCase.input)).toEqual(testCase.output);
    });
});
