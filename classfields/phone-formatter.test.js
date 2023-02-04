const phoneFormatter = require('auto-core/fields/phone/phone-formatter');

const TESTS = [
    { test: '89264247766', result: '+7 926 424-77-66' },
    { test: '+79264247766', result: '+7 926 424-77-66' },
    { test: '79264247766', result: '+7 926 424-77-66' },
    { test: '84832123456', result: '+7 483 212-34-56' },
    { test: '8 (926) 424 77 66', result: '+7 926 424-77-66' },
    { test: '+7 926 424 77 66', result: '+7 926 424-77-66' },
    { test: '7 926 424-77-66', result: '+7 926 424-77-66' },
    { test: '123456', result: '+123456' }, // unknown number
    { test: '87654321', result: '+87654321' }, // unknown number
];
const TESTS_NEED_EXT_UNDEFINED = [
    ...TESTS,
    { test: '749533203041', result: '+7 495 332-03-04 доб. 1' },
    { test: '74953320304100', result: '+7 495 332-03-04 доб. 100' },
    { test: '749533203041000', result: '+7 495 332-03-04 доб. 1000' },

];
const TESTS_NEED_EXT_TRUE = [
    ...TESTS,
    ...TESTS_NEED_EXT_UNDEFINED,
];
const TESTS_NEED_EXT_FALSE = [
    ...TESTS,
    { test: '749533203041', result: '+7 495 332-03-04' },
    { test: '74953320304100', result: '+7 495 332-03-04' },
    { test: '749533203041000', result: '+7 495 332-03-04' },
];
const TESTS_NEED_COLLAPSE_UNDEFINED = [
    ...TESTS,
    { test: '749533203041', result: '+7 495 332-03-04 доб. 1' },
    { test: '74953320304100', result: '+7 495 332-03-04 доб. 100' },
    { test: '749533203041000', result: '+7 495 332-03-04 доб. 1000' },

];
const TESTS_NEED_COLLAPSE_TRUE = [
    { test: '89264247766', result: '+79264247766' },
    { test: '+79264247766', result: '+79264247766' },
    { test: '79264247766', result: '+79264247766' },
    { test: '84832123456', result: '+74832123456' },
    { test: '8 (926) 424 77 66', result: '+79264247766' },
    { test: '+7 926 424 77 66', result: '+79264247766' },
    { test: '7 926 424-77-66', result: '+79264247766' },
    { test: '123456', result: '+123456' }, // unknown number
    { test: '87654321', result: '+87654321' }, // unknown number
    { test: '749533203041', result: '+74953320304доб.1' },
    { test: '74953320304100', result: '+74953320304доб.100' },
    { test: '749533203041000', result: '+74953320304доб.1000' },

];
const TESTS_NEED_COLLAPSE_FALSE = [
    ...TESTS,
    ...TESTS_NEED_COLLAPSE_UNDEFINED,
];

TESTS.forEach((testCase) => {
    it(`should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test)).toEqual(testCase.result);
    });
});
TESTS_NEED_EXT_UNDEFINED.forEach((testCase) => {
    it(`need ext undefined should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test)).toEqual(testCase.result);
    });
});
TESTS_NEED_EXT_TRUE.forEach((testCase) => {
    it(`need ext true should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test, { needExt: true })).toEqual(testCase.result);
    });
});
TESTS_NEED_EXT_FALSE.forEach((testCase) => {
    it(`need ext false should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test, { needExt: false })).toEqual(testCase.result);
    });
});
TESTS_NEED_COLLAPSE_UNDEFINED.forEach((testCase) => {
    it(`need collapse undefined should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test)).toEqual(testCase.result);
    });
});
TESTS_NEED_COLLAPSE_TRUE.forEach((testCase) => {
    it(`need collapse true should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test, { needCollapse: true })).toEqual(testCase.result);
    });
});
TESTS_NEED_COLLAPSE_FALSE.forEach((testCase) => {
    it(`need collapse false should format "${ testCase.test }" as "${ testCase.result }"`, () => {
        expect(phoneFormatter(testCase.test, { needCollapse: false })).toEqual(testCase.result);
    });
});
