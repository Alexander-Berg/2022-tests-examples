const reduceMmmInfoToMarkModelNameplates = require('./reduceMmmInfoToMarkModelNameplates');

const TESTS = require('./reduceMarkModelNameplatesToMmmInfo.testcases');

[
    ...TESTS,
    {
        markModelNameplates: [ 'AUDI#A4' ],
        mmmInfo: [
            { mark: 'AUDI', model: 'A4', generations: [ null ] },
        ],
    },
    {
        markModelNameplates: [ '' ],
        mmmInfo: [
            { mark: null, model: null, generations: [ null ] },
        ],
    },
    {
        markModelNameplates: [ '' ],
        mmmInfo: [ { } ],
    },
].forEach(testCase => {
    it(`должен преобразовать ${ JSON.stringify(testCase.mmmInfo) } в ${ JSON.stringify(testCase.markModelNameplates) }`, () => {
        expect(reduceMmmInfoToMarkModelNameplates(testCase.mmmInfo)).toEqual(testCase.markModelNameplates);
    });
});
