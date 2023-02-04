const phoneIsRu = require('auto-core/fields/phone/phone-is-ru');

const TESTS = [
    { test: '89264247766', result: true },
    { test: '82264247766', result: false },
    { test: '+73264247766', result: true },
    { test: '78264247766', result: true },
    { test: '84832123456', result: true },
    { test: '81832123456', result: false },
    { test: '8 (926) 424 77 66', result: true },
    { test: '+7 926 424 77 66', result: true },
    { test: '7 926 424-77-66', result: true },
    { test: '123456', result: false },
    { test: '87654321', result: false },
];

TESTS.forEach((testCase) => {
    it(`should return "${ testCase.result }" for "${ testCase.test }"`, () => {
        expect(phoneIsRu(testCase.test)).toEqual(testCase.result);
    });
});
