const reduceMarkModelNameplatesToMmmMultiInfo = require('./reduceMarkModelNameplatesToMmmMultiInfo');

const TESTS = require('./reduceMarkModelNameplatesToMmmMultiInfo.testcases');

TESTS.forEach(testCase => {
    it(`должен преобразовать ${ JSON.stringify(testCase.markModelNameplates) } в ${ JSON.stringify(testCase.mmmInfo) }`, () => {
        expect(reduceMarkModelNameplatesToMmmMultiInfo(testCase.markModelNameplates)).toEqual(testCase.mmmInfo);
    });
});
