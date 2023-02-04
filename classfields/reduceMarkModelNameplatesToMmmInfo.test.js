const reduceMarkModelNameplatesToMmmInfo = require('./reduceMarkModelNameplatesToMmmInfo');

const TESTS = require('./reduceMarkModelNameplatesToMmmInfo.testcases');

TESTS.forEach(testCase => {
    it(`должен преобразовать ${ JSON.stringify(testCase.markModelNameplates) } в ${ JSON.stringify(testCase.mmmInfo) }`, () => {
        expect(reduceMarkModelNameplatesToMmmInfo(testCase.markModelNameplates)).toEqual(testCase.mmmInfo);
    });
});
