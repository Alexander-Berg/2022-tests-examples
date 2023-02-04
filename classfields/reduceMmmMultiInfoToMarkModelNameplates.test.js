const reduceMmmInfoToMarkModelNameplates = require('./reduceMmmMultiInfoToMarkModelNameplates');

const TESTS = require('./reduceMmmMultiInfoToMarkModelNameplates.testcases');

TESTS.forEach(testCase => {
    it(`должен преобразовать ${ JSON.stringify(testCase.mmmInfo) } в ${ JSON.stringify(testCase.markModelNameplates) }`, () => {
        expect(reduceMmmInfoToMarkModelNameplates(testCase.mmmInfo)).toEqual(testCase.markModelNameplates);
    });
});
